/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  ContactDetails,
  RecipientDetails,
  SubscriptionDetails
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CdsSubscriber @Inject() (
  subscriptionService: SubscriptionService,
  sessionCache: SessionCache,
  handleSubscriptionService: HandleSubscriptionService,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData
)(implicit ec: ExecutionContext) {

  def subscribeWithCachedDetails(
    cdsOrganisationType: Option[CdsOrganisationType],
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier, request: Request[AnyContent], messages: Messages): Future[SubscriptionResult] = {
    val isRowF           = Future.successful(UserLocation.isRow(requestSessionData))
    val journeyF         = Future.successful(journey)
    val cachedCustomsIdF = subscriptionDetailsService.cachedCustomsId

    val result = for {
      isRow    <- isRowF
      journey  <- journeyF
      customId <- if (isRow) cachedCustomsIdF else Future.successful(None)
    } yield (journey, isRow, customId) match {
      case (Journey.Subscribe, true, Some(_)) => migrationEoriUK(service)                             //Has NINO/UTR as identifier UK journey
      case (Journey.Subscribe, true, None)    => migrationEoriROW(journey, service)                   //ROW
      case (Journey.Subscribe, false, _)      => migrationEoriUK(service)                             //UK Journey
      case _                                  => subscribeEori(cdsOrganisationType, journey, service) //Journey Get An EORI
    }
    result.flatMap(identity)
  }

  private def migrationEoriUK(
    service: Service
  )(implicit hc: HeaderCarrier, messages: Messages): Future[SubscriptionResult] =
    for {
      subscriptionDetails           <- sessionCache.subscriptionDetails
      email                         <- sessionCache.email
      registerWithEoriAndIdResponse <- sessionCache.registerWithEoriAndIdResponse
      subscriptionResult <- subscriptionService.existingReg(
        registerWithEoriAndIdResponse,
        subscriptionDetails,
        email,
        service
      )
      _ <- onSubscriptionResultForUKSubscribe(
        subscriptionResult,
        registerWithEoriAndIdResponse,
        subscriptionDetails,
        email,
        service
      )
    } yield subscriptionResult

  private def subscribeEori(
    cdsOrganisationType: Option[CdsOrganisationType],
    journey: Journey.Value,
    service: Service
  )(implicit hc: HeaderCarrier, messages: Messages): Future[SubscriptionResult] =
    for {
      registrationDetails <- sessionCache.registrationDetails
      (subscriptionResult, maybeSubscriptionDetails) <- fetchOtherDetailsFromCacheAndSubscribe(
        registrationDetails,
        cdsOrganisationType,
        journey,
        service
      )
      _ <- onSubscriptionResult(subscriptionResult, registrationDetails, maybeSubscriptionDetails, service)
    } yield subscriptionResult

  private def migrationEoriROW(journey: Journey.Value, service: Service)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[SubscriptionResult] =
    for {
      registrationDetails <- sessionCache.registrationDetails
      subscriptionDetails <- sessionCache.subscriptionDetails
      email               <- sessionCache.email
      subscriptionResult <- subscriptionService.subscribeWithMandatoryOnly(
        registrationDetails,
        subscriptionDetails,
        journey,
        service,
        Some(email)
      )
      _ <- onSubscriptionResultForRowSubscribe(
        subscriptionResult,
        registrationDetails,
        subscriptionDetails,
        email,
        service
      )
    } yield subscriptionResult

  private def fetchOtherDetailsFromCacheAndSubscribe(
    registrationDetails: RegistrationDetails,
    mayBeCdsOrganisationType: Option[CdsOrganisationType],
    journey: Journey.Value,
    service: Service
  )(implicit hc: HeaderCarrier): Future[(SubscriptionResult, Option[SubscriptionDetails])] =
    for {
      subscriptionDetailsHolder <- sessionCache.subscriptionDetails
      subscriptionResult <- subscriptionService.subscribe(
        registrationDetails,
        subscriptionDetailsHolder,
        mayBeCdsOrganisationType,
        journey,
        service
      )
    } yield (subscriptionResult, Some(subscriptionDetailsHolder))

  private def onSubscriptionResult(
    subscriptionResult: SubscriptionResult,
    regDetails: RegistrationDetails,
    subscriptionDetails: Option[SubscriptionDetails],
    service: Service
  )(implicit hc: HeaderCarrier, messages: Messages): Future[Unit] =
    subscriptionResult match {
      case success: SubscriptionSuccessful =>
        val safeId = regDetails.safeId
        val contactDetails: Option[ContactDetails] = subscriptionDetails
          .flatMap(_.contactDetails.map(_.contactDetails))
        val contactName = contactDetails.map(_.fullName)
        val cdsFullName = Some(regDetails.name)
        val email       = contactDetails.map(_.emailAddress).getOrElse(throw new IllegalStateException("Email required"))
        val mayBeEori   = Some(success.eori)

        completeSubscription(
          service,
          Journey.Register,
          regDetails.name,
          mayBeEori,
          email,
          safeId,
          contactName,
          cdsFullName,
          success.processingDate,
          success.formBundleId,
          success.emailVerificationTimestamp
        )

      case pending: SubscriptionPending =>
        val safeId = regDetails.safeId
        val contactDetails: Option[ContactDetails] = subscriptionDetails
          .flatMap(_.contactDetails.map(_.contactDetails))
        val contactName = contactDetails.map(_.fullName)
        val cdsFullName = Some(regDetails.name)
        val email       = contactDetails.map(_.emailAddress).getOrElse(throw new IllegalStateException("Email required"))
        val mayBeEori   = None
        completeSubscription(
          service,
          Journey.Register,
          regDetails.name,
          mayBeEori,
          email,
          safeId,
          contactName,
          cdsFullName,
          pending.processingDate,
          pending.formBundleId,
          pending.emailVerificationTimestamp
        )

      case failed: SubscriptionFailed =>
        sessionCache.saveSub02Outcome(Sub02Outcome(failed.processingDate, regDetails.name)).map(_ => (): Unit)
    }

  private def onSubscriptionResultForRowSubscribe(
    subscriptionResult: SubscriptionResult,
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    email: String,
    service: Service
  )(implicit hc: HeaderCarrier, messages: Messages): Future[Unit] =
    subscriptionResult match {
      case success: SubscriptionSuccessful =>
        val contactName = subDetails.contactDetails.map(_.fullName)
        val cdsFullName = Some(regDetails.name)
        completeSubscription(
          service,
          Journey.Subscribe,
          subDetails.name,
          Some(success.eori),
          email,
          regDetails.safeId,
          contactName,
          cdsFullName,
          success.processingDate,
          success.formBundleId,
          success.emailVerificationTimestamp
        )

      case pending: SubscriptionPending =>
        val contactName = subDetails.contactDetails.map(_.fullName)
        val cdsFullName = Some(regDetails.name)
        completeSubscription(
          service,
          Journey.Subscribe,
          subDetails.name,
          subDetails.eoriNumber.map(Eori),
          email,
          regDetails.safeId,
          contactName,
          cdsFullName,
          pending.processingDate,
          pending.formBundleId,
          pending.emailVerificationTimestamp
        )
      case failed: SubscriptionFailed =>
        sessionCache.saveSub02Outcome(Sub02Outcome(failed.processingDate, regDetails.name)).map(_ => (): Unit)
    }

  private def onSubscriptionResultForUKSubscribe(
    subscriptionResult: SubscriptionResult,
    regDetails: RegisterWithEoriAndIdResponse,
    subDetails: SubscriptionDetails,
    email: String,
    service: Service
  )(implicit hc: HeaderCarrier, messages: Messages): Future[Unit] = {
    val safeId = regDetails.responseDetail
      .flatMap(_.responseData.map(x => SafeId(x.SAFEID)))
      .getOrElse(throw new IllegalArgumentException("SAFEID Missing"))
    val contactName = regDetails.responseDetail.flatMap(_.responseData.flatMap(_.contactDetail.map(_.contactName)))
    val cdsFullName = regDetails.responseDetail.flatMap(_.responseData.map(_.trader.fullName))

    subscriptionResult match {
      case success: SubscriptionSuccessful =>
        completeSubscription(
          service,
          Journey.Subscribe,
          subDetails.name,
          Some(success.eori),
          email,
          safeId,
          contactName,
          cdsFullName,
          success.processingDate,
          success.formBundleId,
          success.emailVerificationTimestamp
        )
      case pending: SubscriptionPending =>
        completeSubscription(
          service,
          Journey.Subscribe,
          subDetails.name,
          subDetails.eoriNumber.map(Eori),
          email,
          safeId,
          contactName,
          cdsFullName,
          pending.processingDate,
          pending.formBundleId,
          pending.emailVerificationTimestamp
        )

      case failed: SubscriptionFailed =>
        sessionCache.saveSub02Outcome(Sub02Outcome(failed.processingDate, cdsFullName.getOrElse(subDetails.name))).map(
          _ => (): Unit
        )
    }
  }

  private def completeSubscription(
    service: Service,
    journey: Journey.Value,
    name: String,
    maybeEori: Option[Eori],
    email: String,
    safeId: SafeId,
    contactName: Option[String],
    cdsFullName: Option[String],
    processingDate: String,
    formBundleId: String,
    emailVerificationTimestamp: Option[DateTime]
  )(implicit hc: HeaderCarrier, messages: Messages): Future[Unit] =
    sessionCache.saveSub02Outcome(
      Sub02Outcome(processingDate, cdsFullName.getOrElse(name), maybeEori.map(_.id))
    ).flatMap { _ =>
      val recipientDetails =
        RecipientDetails(service, journey, email, contactName.getOrElse(""), cdsFullName, Some(processingDate))

      handleSubscriptionService.handleSubscription(
        formBundleId,
        recipientDetails,
        TaxPayerId(safeId.id),
        maybeEori,
        emailVerificationTimestamp,
        safeId
      )
    }

}
