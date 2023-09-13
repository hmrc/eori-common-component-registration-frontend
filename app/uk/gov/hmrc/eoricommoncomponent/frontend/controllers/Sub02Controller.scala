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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthAction, EnrolmentExtractor}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionCreateResponse._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Sub02Controller @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  sessionCache: SessionCache,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  sub01OutcomeView: sub01_outcome_processing,
  sub02RequestNotProcessed: sub02_request_not_processed,
  sub02SubscriptionInProgressView: sub02_subscription_in_progress,
  sub02EoriAlreadyAssociatedView: sub02_eori_already_associated,
  sub02EoriAlreadyExists: sub02_eori_already_exists,
  standaloneOutcomeView: standalone_subscription_outcome,
  subscriptionOutcomeView: subscription_outcome,
  cdsSubscriber: CdsSubscriber
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with EnrolmentExtractor {

  private val logger = Logger(this.getClass)

  def subscribe(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      val selectedOrganisationType: Option[CdsOrganisationType] =
        requestSessionData.userSelectedOrganisationType
      val internalId = InternalId(loggedInUser.internalId)
      val groupId    = GroupId(loggedInUser.groupId)
      cdsSubscriber
        .subscribeWithCachedDetails(selectedOrganisationType, service)
        .flatMap { subscribeResult =>
          subscribeResult match {
            case _: SubscriptionSuccessful =>
              subscriptionDetailsService
                .saveKeyIdentifiers(groupId, internalId, service)
                .map(_ => Redirect(Sub02Controller.end(service)))
            case _: SubscriptionPending =>
              subscriptionDetailsService
                .saveKeyIdentifiers(groupId, internalId, service)
                .map(_ => Redirect(Sub02Controller.pending(service)))
            case SubscriptionFailed(EoriAlreadyExists, _) =>
              Future.successful(Redirect(Sub02Controller.eoriAlreadyExists(service)))
            case SubscriptionFailed(EoriAlreadyAssociated, _) =>
              Future.successful(Redirect(Sub02Controller.eoriAlreadyAssociated(service)))
            case SubscriptionFailed(SubscriptionInProgress, _) =>
              Future.successful(Redirect(Sub02Controller.subscriptionInProgress(service)))
            case SubscriptionFailed(RequestNotProcessed, _) =>
              Future.successful(Redirect(Sub02Controller.requestNotProcessed(service)))
            case res: SubscriptionFailed =>
              // $COVERAGE-OFF$Loggers
              logger.warn(s"Unexpected response returned from SUB02: ${res.failureReason}")
              // $COVERAGE-ON
              throw new IllegalArgumentException(s"Cannot redirect for subscription with registration journey")
          }
        } recoverWith {
        case e: Exception =>
          val error = "Subscription Error. "
          // $COVERAGE-OFF$Loggers
          logger.warn(error, e)
          // $COVERAGE-ON
          Future.failed(new RuntimeException(error, e))
      }
    }

  def subscriptionNextSteps(service: Service): String =
    s"cds.subscription.outcomes.success.extra.information.next.new.${service.code}"

  def end(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        subDetails   <- sessionCache.subscriptionDetails
        sub02Outcome <- sessionCache.sub02Outcome
        sub01Outcome <- sessionCache.sub01Outcome
        _            <- sessionCache.journeyCompleted
      } yield
        if (service.code.equalsIgnoreCase(Service.eoriOnly.code))
          Ok(
            standaloneOutcomeView(
              sub02Outcome.eori
                .getOrElse("EORI not populated from Sub02 response."),
              if (sub01Outcome.processedDate.nonEmpty) sub01Outcome.processedDate else sub02Outcome.processedDate,
              service
            )
          )
        else {
          val subscriptionTo = s"ecc.start-page.para1.bullet2.new.${service.code}"
          Ok(
            subscriptionOutcomeView(
              service,
              sub02Outcome.eori
                .getOrElse("EORI not populated from Sub02 response."),
              if (sub01Outcome.processedDate.nonEmpty) sub01Outcome.processedDate else sub02Outcome.processedDate,
              subscriptionTo,
              subscriptionNextSteps(service)
            )
          )
        }
  }

  def eoriAlreadyExists(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        name          <- sessionCache.subscriptionDetails.map(_.name)
        processedDate <- sessionCache.sub01Outcome.map(_.processedDate)
        _             <- sessionCache.journeyCompleted
      } yield Ok(sub02EoriAlreadyExists(name, processedDate, service))
    }

  def eoriAlreadyAssociated(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        name          <- sessionCache.subscriptionDetails.map(_.name)
        processedDate <- sessionCache.sub01Outcome.map(_.processedDate)
        _             <- sessionCache.journeyCompleted
      } yield Ok(sub02EoriAlreadyAssociatedView(name, processedDate, service))
    }

  def subscriptionInProgress(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        submissionCompleteDetails <- sessionCache.submissionCompleteDetails
        _                         <- sessionCache.journeyCompleted
      } yield Ok(sub02SubscriptionInProgressView(submissionCompleteDetails.processingDate, service))
    }

  def requestNotProcessed(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        _ <- sessionCache.journeyCompleted
      } yield Ok(sub02RequestNotProcessed(service))
    }

  def pending(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        subscriptionDetails <- sessionCache.subscriptionDetails
        sub01Outcome        <- sessionCache.sub01Outcome
        _                   <- sessionCache.journeyCompleted
      } yield Ok(sub01OutcomeView(sub01Outcome.processedDate, service))
  }

}
