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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.RegistrationDisplayResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationDisplayService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{NewSubscription, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{
  sub01_outcome_processing,
  sub01_outcome_rejected
}
import uk.gov.hmrc.http.HeaderCarrier

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
  sub01OutcomeRejected: sub01_outcome_rejected,
  errorTemplate: error_template
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def isAffinityOrganisation(affinityGroup: Option[AffinityGroup]): Boolean =
    affinityGroup.contains(AffinityGroup.Organisation)

  private def continue(service: Service, journey: Journey.Value)(implicit
    request: Request[AnyContent],
    user: LoggedInUserWithEnrolments
  ): Future[Result] =
    Future.successful(
      Ok(userLocationView(userLocationForm, service, journey, isAffinityOrganisation(user.affinityGroup)))
    )

  def form(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => implicit user: LoggedInUserWithEnrolments =>
      continue(service, journey)
    }

  private def forRow(service: Service, journey: Journey.Value, groupId: GroupId, location: String)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ) =
    subscriptionStatusBasedOnSafeId(groupId).map {
      case (NewSubscription | SubscriptionRejected, Some(safeId)) =>
        registrationDisplayService
          .requestDetails(safeId)
          .flatMap(cacheAndRedirect(service, journey, location))
      case (status, _) =>
        subscriptionStatus(status, groupId, service, journey, Some(location))
    }.flatMap(identity)

  def submit(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      userLocationForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              userLocationView(formWithErrors, service, journey, isAffinityOrganisation(loggedInUser.affinityGroup))
            )
          ),
        details =>
          (journey, details.location, loggedInUser.groupId) match {
            case (_, Some(UserLocation.Iom), Some(_)) =>
              Future.successful(Redirect(YouNeedADifferentServiceIomController.form(service, journey)))
            case (Journey.Register, Some(location), Some(id)) if UserLocation.isRow(location) =>
              forRow(service, journey, GroupId(id), location)
            case _ =>
              Future.successful(
                Redirect(OrganisationTypeController.form(service, journey))
                  .withSession(
                    requestSessionData
                      .sessionWithUserLocationAdded(sessionInfoBasedOnJourney(journey, details.location))
                  )
              )
          }
      )
    }

  private def sessionInfoBasedOnJourney(journey: Journey.Value, location: Option[String]): String =
    journey match {
      case Journey.Register =>
        location match {
          case Some(UserLocation.ThirdCountry)      => "third-country"
          case Some(UserLocation.ThirdCountryIncEU) => "third-country-inc-eu"
          case Some(UserLocation.Eu)                => "eu"
          case Some(UserLocation.Iom)               => "iom"
          case Some(UserLocation.Islands)           => "islands"
          case Some(UserLocation.Uk)                => "uk"
          case _                                    => throw new IllegalStateException("User Location not set")
        }
      case _ =>
        location.getOrElse(throw new IllegalStateException("User Location not set"))

    }

  private def subscriptionStatusBasedOnSafeId(groupId: GroupId)(implicit hc: HeaderCarrier) =
    for {
      mayBeSafeId <- save4LaterService.fetchSafeId(groupId)
      preSubscriptionStatus <- mayBeSafeId match {
        case Some(safeId) =>
          subscriptionStatusService.getStatus("SAFE", safeId.id)
        case None => Future.successful(NewSubscription)
      }
    } yield (preSubscriptionStatus, mayBeSafeId)

  private def handleExistingSubscription(groupId: GroupId, service: Service)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    save4LaterService
      .fetchSafeId(groupId)
      .flatMap(
        safeId =>
          sessionCache
            .saveRegistrationDetails(RegistrationDetails.rdSafeId(safeId.get))
            .map(_ => Redirect(SubscriptionRecoveryController.complete(service, Journey.Register)))
      )

  def subscriptionStatus(
    preSubStatus: PreSubscriptionStatus,
    groupId: GroupId,
    service: Service,
    journey: Journey.Value,
    location: Option[String]
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] =
    preSubStatus match {
      case SubscriptionProcessing =>
        Future.successful(Redirect(UserLocationController.processing(service)))
      case SubscriptionExists => handleExistingSubscription(groupId, service)
      case NewSubscription | SubscriptionRejected =>
        Future.successful(
          Redirect(OrganisationTypeController.form(service, journey))
            .withSession(requestSessionData.sessionWithUserLocationAdded(sessionInfoBasedOnJourney(journey, location)))
        )
    }

  def cacheAndRedirect(service: Service, journey: Journey.Value, location: String)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Either[_, RegistrationDisplayResponse] => Future[Result] = {

    case rResponse @ Right(RegistrationDisplayResponse(_, Some(_))) =>
      registrationDisplayService.cacheDetails(rResponse.value).flatMap { _ =>
        Future.successful(
          Redirect(BusinessDetailsRecoveryController.form(service, journey)).withSession(
            requestSessionData.sessionWithUserLocationAdded(sessionInfoBasedOnJourney(journey, Some(location)))
          )
        )
      }
    case _ => Future.successful(InternalServerError(errorTemplate()))
  }

  def processing(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.sub01Outcome
        .map(_.processedDate)
        .map(processedDate => Ok(sub01OutcomeProcessing(None, processedDate)))
  }

  def rejected(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      sessionCache.sub01Outcome
        .map(_.processedDate)
        .map(processedDate => Ok(sub01OutcomeRejected(None, processedDate, service)))
  }

}
