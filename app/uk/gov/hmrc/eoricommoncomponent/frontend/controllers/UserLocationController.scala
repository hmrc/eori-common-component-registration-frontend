/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RegistrationInfoRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.RegistrationDisplayResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserLocationController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  save4LaterService: Save4LaterService,
  subscriptionStatusService: SubscriptionStatusService,
  sessionCache: SessionCache,
  registrationDisplayService: RegistrationDisplayService,
  mcc: MessagesControllerComponents,
  userLocationView: user_location,
  sub01OutcomeProcessing: sub01_outcome_processing,
  errorTemplate: error_template,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def isAffinityOrganisation(affinityGroup: Option[AffinityGroup]): Boolean =
    affinityGroup.contains(AffinityGroup.Organisation)

  private def continue(
    service: Service
  )(implicit request: Request[AnyContent], user: LoggedInUserWithEnrolments): Future[Result] =
    Future.successful(Ok(userLocationView(userLocationForm, service, isAffinityOrganisation(user.affinityGroup))))

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => implicit user: LoggedInUserWithEnrolments =>
      continue(service)
    }

  private def forRow(service: Service, groupId: GroupId, location: UserLocation)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ) =
    subscriptionStatusBasedOnSafeId(groupId)(hc, service, request).map {
      case (NewSubscription | SubscriptionRejected, Some(safeId)) =>
        registrationDisplayService
          .requestDetails(safeId)
          .flatMap(cacheAndRedirect(service, location, groupId))
      case (status, _) =>
        subscriptionStatus(status, groupId, service, location)
    }.flatMap(identity _)

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      userLocationForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(userLocationView(formWithErrors, service, isAffinityOrganisation(loggedInUser.affinityGroup)))
          ),
        location =>
          (location, loggedInUser.groupId) match {
            case (location, Some(id)) if UserLocation.isRow(location) =>
              forRow(service, GroupId(id), location)
            case (UserLocation.Iom, Some(_)) if !appConfig.allowNoIdJourney =>
              Future.successful(Redirect(YouNeedADifferentServiceIomController.form(service)))
            case _ =>
              save4LaterService.saveUserLocation(GroupId(loggedInUser.groupId.head), location)
                .map { _ =>
                  Redirect(OrganisationTypeController.form(service))
                    .withSession(
                      requestSessionData
                        .sessionWithUserLocationAdded(location)
                    )
                }
          }
      )
    }

  private def subscriptionStatusBasedOnSafeId(
    groupId: GroupId
  )(implicit hc: HeaderCarrier, service: Service, request: Request[_]) =
    for {
      mayBeSafeId <- save4LaterService.fetchSafeId(groupId)
      preSubscriptionStatus <- mayBeSafeId match {
        case Some(safeId) =>
          subscriptionStatusService.getStatus(RegistrationInfoRequest.SAFE, safeId.id)
        case None => Future.successful(NewSubscription)
      }
    } yield (preSubscriptionStatus, mayBeSafeId)

  private def handleExistingSubscription(groupId: GroupId, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Result] =
    save4LaterService
      .fetchSafeId(groupId)
      .flatMap(
        safeId =>
          sessionCache
            .saveRegistrationDetails(RegistrationDetails.rdSafeId(safeId.get))
            .map(_ => Redirect(SubscriptionRecoveryController.complete(service)))
      )

  def subscriptionStatus(
    preSubStatus: PreSubscriptionStatus,
    groupId: GroupId,
    service: Service,
    location: UserLocation
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] =
    preSubStatus match {
      case SubscriptionProcessing =>
        Future.successful(Redirect(UserLocationController.processing(service)))
      case SubscriptionExists => handleExistingSubscription(groupId, service)
      case NewSubscription | SubscriptionRejected =>
        save4LaterService.saveUserLocation(groupId, location).flatMap { _ =>
          Future.successful(
            Redirect(OrganisationTypeController.form(service))
              .withSession(requestSessionData.sessionWithUserLocationAdded(location))
          )
        }
    }

  def cacheAndRedirect(service: Service, location: UserLocation, groupId: GroupId)(implicit
    request: Request[AnyContent]
  ): Either[_, RegistrationDisplayResponse] => Future[Result] = {

    case rResponse @ Right(RegistrationDisplayResponse(_, Some(_))) =>
      registrationDisplayService.cacheDetails(rResponse.value).flatMap { _ =>
        save4LaterService.saveUserLocation(groupId, location).map { _ =>
          Redirect(BusinessDetailsRecoveryController.form(service)).withSession(
            requestSessionData.sessionWithUserLocationAdded(location)
          )

        }
      }
    case _ => Future.successful(InternalServerError(errorTemplate(service)))
  }

  def processing(service: Service): Action[AnyContent] = authAction.enrolledUserWithSessionAction(service) {
    implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.sub01Outcome
        .map(_.processedDate)
        .map(processedDate => Ok(sub01OutcomeProcessing(processedDate, service)))
  }

}
