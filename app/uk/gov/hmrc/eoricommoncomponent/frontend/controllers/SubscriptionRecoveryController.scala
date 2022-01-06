/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import java.time.{LocalDate, LocalDateTime}

import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{Action, _}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.SUB09SubscriptionDisplayConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionDisplayResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.RecipientDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{HandleSubscriptionService, RandomUUIDGenerator}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.recovery_registration_exists
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionRecoveryController @Inject() (
  authAction: AuthAction,
  handleSubscriptionService: HandleSubscriptionService,
  sessionCache: SessionCache,
  SUB09Connector: SUB09SubscriptionDisplayConnector,
  mcc: MessagesControllerComponents,
  errorTemplateView: error_template,
  uuidGenerator: RandomUUIDGenerator,
  requestSessionData: RequestSessionData,
  alreadyHaveEori: recovery_registration_exists
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val logger: Logger = Logger(this.getClass)

  def complete(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithServiceAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscribeGetAnEori(service)
    }

  def eoriExist(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithServiceAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          eori <- sessionCache.eori
        } yield Ok(alreadyHaveEori(eori))
    }

  private def subscribeGetAnEori(
    service: Service
  )(implicit ec: ExecutionContext, request: Request[AnyContent]): Future[Result] = {
    val result = for {
      registrationDetails <- sessionCache.registrationDetails
      safeId          = registrationDetails.safeId.id
      queryParameters = ("taxPayerID" -> safeId) :: buildQueryParams
      sub09Result  <- SUB09Connector.subscriptionDisplay(queryParameters)
      sub01Outcome <- sessionCache.sub01Outcome
    } yield sub09Result match {
      case Right(subscriptionDisplayResponse) =>
        val eori = subscriptionDisplayResponse.responseDetail.EORINo
          .getOrElse(throw new IllegalStateException("no eori found in the response"))

        sessionCache.saveEori(Eori(eori)).flatMap { _ =>
          val mayBeEmail = subscriptionDisplayResponse.responseDetail.contactInformation
            .flatMap(c => c.emailAddress.filter(EmailAddress.isValid(_) && c.emailVerificationTimestamp.isDefined))
          mayBeEmail.map { email =>
            onSUB09Success(
              sub01Outcome.processedDate,
              email,
              safeId,
              Eori(eori),
              subscriptionDisplayResponse,
              getDateOfBirthOrDateOfEstablishment(
                subscriptionDisplayResponse,
                registrationDetails.dateOfEstablishmentOption,
                registrationDetails.dateOfBirthOption
              ),
              service
            )(Redirect(Sub02Controller.end(service)))
          }.getOrElse {
            logger.info("Email Missing")
            Future.successful(Redirect(SubscriptionRecoveryController.eoriExist(service)))
          }
        }
      case Left(_) =>
        Future.successful(InternalServerError(errorTemplateView()))
    }
    result.flatMap(identity)
  }

  private def buildQueryParams: List[(String, String)] =
    List("regime" -> "CDS", "acknowledgementReference" -> uuidGenerator.generateUUIDAsString)

  private case class SubscriptionInformation(
    processedDate: String,
    email: String,
    emailVerificationTimestamp: Option[LocalDateTime],
    formBundleId: String,
    recipientFullName: String,
    name: String,
    eori: Eori,
    safeId: SafeId,
    dateOfEstablishment: Option[LocalDate]
  )

  private def onSUB09Success(
    processedDate: String,
    email: String,
    safeId: String,
    eori: Eori,
    subscriptionDisplayResponse: SubscriptionDisplayResponse,
    dateOfEstablishment: Option[LocalDate],
    service: Service
  )(redirect: => Result)(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[Result] = {
    val formBundleId =
      subscriptionDisplayResponse.responseCommon.returnParameters
        .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue))
        .getOrElse(throw new IllegalStateException("NO ETMPFORMBUNDLENUMBER specified"))

    //As the result of migration person of contact is likely to be empty use string Customer
    val recipientFullName =
      subscriptionDisplayResponse.responseDetail.contactInformation.flatMap(_.personOfContact).getOrElse("Customer")
    val name = subscriptionDisplayResponse.responseDetail.CDSFullName
    val emailVerificationTimestamp =
      subscriptionDisplayResponse.responseDetail.contactInformation.flatMap(_.emailVerificationTimestamp)

    val subscriptionInformation = SubscriptionInformation(
      processedDate,
      email,
      emailVerificationTimestamp,
      formBundleId,
      recipientFullName,
      name,
      eori,
      SafeId(safeId),
      dateOfEstablishment
    )

    completeEnrolment(service, subscriptionInformation)(redirect)
  }

  private def completeEnrolment(service: Service, subscriptionInformation: SubscriptionInformation)(
    redirect: => Result
  )(implicit hc: HeaderCarrier, messages: Messages): Future[Result] =
    for {
      // Update Recovered Subscription Information
      _ <- updateSubscription(subscriptionInformation)
      // Update Email
//      _ <- updateEmail(journey, subscriptionInformation)  // TODO - ECC-307
      // Subscribe Call for enrolment
      _ <- subscribe(service, subscriptionInformation)
      // Issuer Call for enrolment
      res <- Future.successful(NO_CONTENT)
    } yield res match {
      case NO_CONTENT => redirect
      case _          => throw new IllegalArgumentException("Tax Enrolment issuer call failed")
    }

  private def updateSubscription(subscriptionInformation: SubscriptionInformation)(implicit hc: HeaderCarrier) =
    sessionCache.saveSub02Outcome(
      Sub02Outcome(
        subscriptionInformation.processedDate,
        subscriptionInformation.name,
        Some(subscriptionInformation.eori.id)
      )
    )

  private def subscribe(service: Service, subscriptionInformation: SubscriptionInformation)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Unit] =
    handleSubscriptionService
      .handleSubscription(
        subscriptionInformation.formBundleId,
        RecipientDetails(
          service,
          Journey.Register, // TODO this value is going into backend database, we need to understand how removing can affect it
          subscriptionInformation.email,
          subscriptionInformation.recipientFullName,
          Some(subscriptionInformation.name),
          Some(subscriptionInformation.processedDate)
        ),
        TaxPayerId(subscriptionInformation.safeId.id),
        Some(subscriptionInformation.eori),
        subscriptionInformation.emailVerificationTimestamp,
        subscriptionInformation.safeId
      )

  private def getDateOfBirthOrDateOfEstablishment(
    response: SubscriptionDisplayResponse,
    dateOfEstablishmentCaptured: Option[LocalDate],
    dateOfBirthCaptured: Option[LocalDate]
  )(implicit request: Request[AnyContent]): Option[LocalDate] = {
    val isIndividualOrSoleTrader = requestSessionData.isIndividualOrSoleTrader
    val dateOfEstablishment      = response.responseDetail.dateOfEstablishment // Date we hold
    (isIndividualOrSoleTrader, dateOfEstablishment, dateOfEstablishmentCaptured, dateOfBirthCaptured) match {
      case (_, Some(date), _, _)     => Some(date)
      case (false, _, Some(date), _) => Some(date)
      case (true, _, _, Some(date))  => Some(date)
      case _                         => throw MissingDateException()
    }
  }

  case class MissingDateException(msg: String = "Missing date of enrolment or birth") extends Exception(msg)

}
