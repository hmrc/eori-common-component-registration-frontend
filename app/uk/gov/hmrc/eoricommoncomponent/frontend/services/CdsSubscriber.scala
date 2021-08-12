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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import javax.inject.{Inject, Singleton}
import java.time.ZonedDateTime
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  ContactDetails,
  RecipientDetails,
  SubscriptionDetails
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CdsSubscriber @Inject() (
  subscriptionService: SubscriptionService,
  sessionCache: SessionCache,
  handleSubscriptionService: HandleSubscriptionService
)(implicit ec: ExecutionContext) {

  def subscribeWithCachedDetails(cdsOrganisationType: Option[CdsOrganisationType], service: Service)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[SubscriptionResult] =
    subscribeEori(cdsOrganisationType, service)

  private def subscribeEori(cdsOrganisationType: Option[CdsOrganisationType], service: Service)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[SubscriptionResult] =
    for {
      registrationDetails <- sessionCache.registrationDetails
      (subscriptionResult, maybeSubscriptionDetails) <- fetchOtherDetailsFromCacheAndSubscribe(
        registrationDetails,
        cdsOrganisationType,
        service
      )
      _ <- onSubscriptionResult(subscriptionResult, registrationDetails, maybeSubscriptionDetails, service)
    } yield subscriptionResult

  private def fetchOtherDetailsFromCacheAndSubscribe(
    registrationDetails: RegistrationDetails,
    mayBeCdsOrganisationType: Option[CdsOrganisationType],
    service: Service
  )(implicit hc: HeaderCarrier): Future[(SubscriptionResult, Option[SubscriptionDetails])] =
    for {
      subscriptionDetailsHolder <- sessionCache.subscriptionDetails
      subscriptionResult <- subscriptionService.subscribe(
        registrationDetails,
        subscriptionDetailsHolder,
        mayBeCdsOrganisationType,
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
    emailVerificationTimestamp: Option[ZonedDateTime]
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
