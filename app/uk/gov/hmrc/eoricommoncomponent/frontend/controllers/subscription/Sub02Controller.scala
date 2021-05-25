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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthAction, EnrolmentExtractor}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionCreateResponse._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.migration_success
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.xi_eori_guidance
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Sub02Controller @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  sessionCache: SessionCache,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  migrationSuccessView: migration_success,
  sub01OutcomeView: sub01_outcome_processing,
  sub02RequestNotProcessed: sub02_request_not_processed,
  sub02SubscriptionInProgressView: sub02_subscription_in_progress,
  sub02EoriAlreadyAssociatedView: sub02_eori_already_associated,
  sub02EoriAlreadyExists: sub02_eori_already_exists,
  sub01OutcomeRejected: sub01_outcome_rejected,
  subscriptionOutcomeView: subscription_outcome,
  xiEoriGuidancePage: xi_eori_guidance,
  cdsSubscriber: CdsSubscriber
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with EnrolmentExtractor {

  private val logger = Logger(this.getClass)

  def subscribe(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      val selectedOrganisationType: Option[CdsOrganisationType] =
        requestSessionData.userSelectedOrganisationType
      val internalId = InternalId(loggedInUser.internalId)
      val groupId    = GroupId(loggedInUser.groupId)
      cdsSubscriber
        .subscribeWithCachedDetails(selectedOrganisationType, service, journey)
        .flatMap { subscribeResult =>
          (subscribeResult, journey) match {
            case (_: SubscriptionSuccessful, Journey.Register) =>
              subscriptionDetailsService
                .saveKeyIdentifiers(groupId, internalId, service)
                .map(_ => Redirect(Sub02Controller.end(service)))
            case (_: SubscriptionPending, _) =>
              subscriptionDetailsService
                .saveKeyIdentifiers(groupId, internalId, service)
                .map(_ => Redirect(Sub02Controller.pending(service)))
            case (SubscriptionFailed(EoriAlreadyExists, _), _) =>
              Future.successful(Redirect(Sub02Controller.eoriAlreadyExists(service)))
            case (SubscriptionFailed(EoriAlreadyAssociated, _), _) =>
              Future.successful(Redirect(Sub02Controller.eoriAlreadyAssociated(service)))
            case (SubscriptionFailed(SubscriptionInProgress, _), _) =>
              Future.successful(Redirect(Sub02Controller.subscriptionInProgress(service)))
            case (SubscriptionFailed(RequestNotProcessed, _), _) =>
              Future.successful(Redirect(Sub02Controller.requestNotProcessed(service)))
            case (_: SubscriptionFailed, _) =>
              Future.successful(Redirect(Sub02Controller.rejected(service)))
            case _ =>
              throw new IllegalArgumentException(s"Cannot redirect for subscription with journey: $journey")
          }
        } recoverWith {
        case e: Exception =>
          logger.error("Subscription Error. ", e)
          Future.failed(new RuntimeException("Subscription Error. ", e))
      }
    }

  def xiEoriGuidance: Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(xiEoriGuidancePage()))
  }

  def end(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        sub02Outcome <- sessionCache.sub02Outcome
        _            <- sessionCache.remove
        _            <- sessionCache.saveSub02Outcome(sub02Outcome)
      } yield Ok(
        subscriptionOutcomeView(
          sub02Outcome.eori
            .getOrElse("EORI not populated from Sub02 response."),
          sub02Outcome.fullName,
          sub02Outcome.processedDate
        )
      ).withSession(newUserSession)
  }

  // End of normal subscription journey
  def migrationEnd(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithServiceAction {
    implicit request => user: LoggedInUserWithEnrolments =>
      activatedEnrolmentForService(user, service) match {
        case Some(_) => Future.successful(Redirect(CompletedEnrolmentController.enrolSuccess(service)))
        case _ =>
          if (UserLocation.isRow(requestSessionData))
            subscriptionDetailsService.cachedCustomsId flatMap {
              case Some(_) => renderPageWithName(service)
              case _       => renderPageWithNameRow(service)
            }
          else renderPageWithName(service)
      }
  }

  def rejected(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        sub02Outcome <- sessionCache.sub02Outcome
        _            <- sessionCache.remove
      } yield Ok(sub01OutcomeRejected(Some(sub02Outcome.fullName), sub02Outcome.processedDate, service)).withSession(
        newUserSession
      )
  }

  def eoriAlreadyExists(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        sub02Outcome <- sessionCache.sub02Outcome
        _            <- sessionCache.remove
      } yield Ok(sub02EoriAlreadyExists(sub02Outcome.fullName, sub02Outcome.processedDate)).withSession(newUserSession)
    }

  def eoriAlreadyAssociated(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        sub02Outcome <- sessionCache.sub02Outcome
        _            <- sessionCache.remove
      } yield Ok(sub02EoriAlreadyAssociatedView(sub02Outcome.fullName, sub02Outcome.processedDate)).withSession(
        newUserSession
      )
    }

  def subscriptionInProgress(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        sub02Outcome <- sessionCache.sub02Outcome
        _            <- sessionCache.remove
      } yield Ok(sub02SubscriptionInProgressView(sub02Outcome.fullName, sub02Outcome.processedDate)).withSession(
        newUserSession
      )
    }

  def requestNotProcessed(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        _ <- sessionCache.remove
      } yield Ok(sub02RequestNotProcessed()).withSession(newUserSession)
    }

  def pending(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        sub02Outcome <- sessionCache.sub02Outcome
        _            <- sessionCache.remove
      } yield Ok(sub01OutcomeView(Some(sub02Outcome.fullName), sub02Outcome.processedDate)).withSession(newUserSession)
  }

  private def renderPageWithName(service: Service)(implicit hc: HeaderCarrier, request: Request[_]) =
    for {
      name <- sessionCache.registerWithEoriAndIdResponse.map(
        _.responseDetail.flatMap(_.responseData.map(_.trader.fullName))
      )
      sub02Outcome <- sessionCache.sub02Outcome
      _            <- sessionCache.remove
      _ <- sessionCache.saveSub02Outcome(
        Sub02Outcome(sub02Outcome.processedDate, name.getOrElse(""), sub02Outcome.eori)
      )
    } yield Ok(
      migrationSuccessView(
        sub02Outcome.eori,
        name.getOrElse(throw new IllegalStateException("Name not populated from reg06")),
        sub02Outcome.processedDate,
        service
      )
    ).withSession(newUserSession)

  private def renderPageWithNameRow(service: Service)(implicit hc: HeaderCarrier, request: Request[_]) =
    for {
      sub02Outcome <- sessionCache.sub02Outcome
      _            <- sessionCache.remove
      _            <- sessionCache.saveSub02Outcome(sub02Outcome)
    } yield Ok(
      migrationSuccessView(sub02Outcome.eori, sub02Outcome.fullName, sub02Outcome.processedDate, service)
    ).withSession(newUserSession)

}
