/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.SUB09SubscriptionDisplayConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionDisplayResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.RecipientDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{
  HandleSubscriptionService,
  RandomUUIDGenerator,
  TaxEnrolmentsService,
  UpdateVerifiedEmailService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, recovery_registration_exists}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class SubscriptionRecoveryController @Inject() (
  authAction: AuthAction,
  handleSubscriptionService: HandleSubscriptionService,
  taxEnrolmentService: TaxEnrolmentsService,
  updateVerifiedEmailService: UpdateVerifiedEmailService,
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
        } yield Ok(alreadyHaveEori(eori, service))
    }

  private def subscribeGetAnEori(
    service: Service
  )(implicit ec: ExecutionContext, request: Request[AnyContent]): Future[Result] = {
    val result = for {
      registrationDetails <- sessionCache.registrationDetails
      safeId = registrationDetails.safeId.id
      sub09Result  <- SUB09Connector.subscriptionDisplay(safeId, uuidGenerator.generateUUIDAsString)
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
            // $COVERAGE-OFF$Loggers
            logger.info("Email Missing")
            // $COVERAGE-ON
            Future.successful(Redirect(SubscriptionRecoveryController.eoriExist(service)))
          }
        }
      case Left(_) =>
        Future.successful(InternalServerError(errorTemplateView(service)))
    }
    result.flatMap(identity)
  }

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
  )(
    redirect: => Result
  )(implicit headerCarrier: HeaderCarrier, request: Request[_], messages: Messages): Future[Result] = {
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
      enrichFormBundleId(service, formBundleId),
      recipientFullName,
      name,
      eori,
      SafeId(safeId),
      dateOfEstablishment
    )

    completeEnrolment(service, subscriptionInformation)(redirect)(headerCarrier, request, messages)
  }

  private def enrichFormBundleId(service: Service, formBundleId: String) =
    if (service.enrolmentKey.equalsIgnoreCase(Service.cds.enrolmentKey))
      s"$formBundleId${Random.nextInt(1000)}cds"
    else
      formBundleId + service.code + "-" + (100000 + Random.nextInt(900000)).toString

  private def completeEnrolment(service: Service, subscriptionInformation: SubscriptionInformation)(
    redirect: => Result
  )(implicit hc: HeaderCarrier, request: Request[_], messages: Messages): Future[Result] =
    for {
      // Update Recovered Subscription Information
      _ <- updateSubscription(subscriptionInformation)
      // Update Email
      _ <- if (service.enrolmentKey == Service.cds.enrolmentKey) updateEmail(subscriptionInformation)
      else Future.successful(None)
      // Subscribe Call for enrolment
      _ <- subscribe(service, subscriptionInformation)(hc, messages)
      // Issuer Call for enrolment
      res <- issue(service, subscriptionInformation)
    } yield res match {
      case NO_CONTENT => redirect
      case _          => throw new IllegalArgumentException("Tax Enrolment issuer call failed")
    }

  private def updateEmail(
    subscriptionInformation: SubscriptionInformation
  )(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    updateVerifiedEmailService
      .updateVerifiedEmail(newEmail = subscriptionInformation.email, eori = subscriptionInformation.eori.id)
      .map {
        case true => Some(true)
        case _    => throw new IllegalArgumentException("UpdateEmail failed")
      }

  private def updateSubscription(subscriptionInformation: SubscriptionInformation)(implicit request: Request[_]) =
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

  private def issue(service: Service, subscriptionInformation: SubscriptionInformation)(implicit
    hc: HeaderCarrier
  ): Future[Int] =
    taxEnrolmentService.issuerCall(
      subscriptionInformation.formBundleId,
      subscriptionInformation.eori,
      subscriptionInformation.dateOfEstablishment,
      service
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
