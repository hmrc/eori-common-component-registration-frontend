/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.cache

import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Session}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.IndividualOrganisations
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{IndividualSubscriptionFlow, OrganisationSubscriptionFlow, PartnershipSubscriptionFlow, SubscriptionFlow}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError.DataNotFound
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Iom
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.vat.details.VatDetails

import javax.inject.{Inject, Singleton}

@Singleton
class RequestSessionData @Inject() (audit: Auditor) {

  def storeUserSubscriptionFlow(subscriptionFlow: SubscriptionFlow, uriBeforeSubscriptionFlow: String)(implicit
    request: Request[AnyContent]
  ): Session =
    request.session + (RequestSessionDataKeys.subscriptionFlow -> subscriptionFlow.name) +
      (RequestSessionDataKeys.uriBeforeSubscriptionFlow        -> uriBeforeSubscriptionFlow)

  def userSubscriptionFlow(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Either[SessionError, SubscriptionFlow] =
    request.session.data.get(RequestSessionDataKeys.subscriptionFlow) match {
      case Some(flowName) => Right(SubscriptionFlow(flowName))
      case None =>
        audit.sendSubscriptionFlowSessionFailureEvent(Json.toJson(request.session.data))
        Left(DataNotFound(RequestSessionDataKeys.subscriptionFlow))
    }

  def userSelectedOrganisationType(implicit request: Request[AnyContent]): Option[CdsOrganisationType] =
    request.session.data.get(RequestSessionDataKeys.selectedOrganisationType).map(CdsOrganisationType.forId)

  def sessionWithOrganisationTypeAdded(existingSession: Session, organisationType: CdsOrganisationType): Session =
    existingSession + (RequestSessionDataKeys.selectedOrganisationType -> organisationType.id)

  def sessionWithOrganisationTypeAdded(
    organisationType: CdsOrganisationType
  )(implicit request: Request[AnyContent]): Session =
    sessionWithOrganisationTypeAdded(request.session, organisationType)

  def sessionWithoutOrganisationType(implicit request: Request[AnyContent]): Session =
    request.session - RequestSessionDataKeys.selectedOrganisationType

  def sessionForStartAgain(implicit request: Request[AnyContent]): Session =
    request.session -
      RequestSessionDataKeys.subscriptionFlow -
      RequestSessionDataKeys.selectedOrganisationType -
      RequestSessionDataKeys.uriBeforeSubscriptionFlow

  def selectedUserLocation(implicit request: Request[AnyContent]): Option[UserLocation] = {
    val userLocation = request.session.data.get(RequestSessionDataKeys.selectedUserLocation)

    userLocation match {
      case Some("islands") => Some(UserLocation.ThirdCountry)
      case Some("eu") => Some(UserLocation.ThirdCountry)
      case location => location.flatMap(UserLocation.enumerable.withName)
    }
  }

  def selectedUserLocationWithIslands(implicit request: Request[AnyContent]): Option[UserLocation] =
    request.session.data.get(RequestSessionDataKeys.selectedUserLocation).flatMap(UserLocation.enumerable.withName)

  def sessionWithUserLocationAdded(userLocation: String)(implicit request: Request[AnyContent]): Session =
    request.session + (RequestSessionDataKeys.selectedUserLocation -> userLocation)

  def existingSessionWithUserLocationAdded(existingSession: Session, userLocation: String): Session =
    existingSession + (RequestSessionDataKeys.selectedUserLocation -> userLocation)

  def isPartnership(implicit request: Request[AnyContent]): Boolean = userSelectedOrganisationType.fold(false) { orgType =>
    orgType == CdsOrganisationType.Partnership
  }

  def isCharity(implicit request: Request[AnyContent]): Boolean =
    userSelectedOrganisationType.fold(false)(orgType => orgType == CdsOrganisationType.CharityPublicBodyNotForProfit)

  def isIndividualOrSoleTrader(implicit request: Request[AnyContent]): Boolean =
    userSelectedOrganisationType.fold(false) { orgType =>
      IndividualOrganisations.contains(orgType)
    }

  def isRestOfTheWorld(implicit request: Request[AnyContent]): Boolean =
    userSelectedOrganisationType.fold(false) { orgType =>
      CdsOrganisationType.RestOfTheWorld.contains(orgType)
    }

  def bypassIomUkVatLookup(
    appConfig: AppConfig,
    userLocation: UserLocation,
    requestSessionData: RequestSessionData,
    formData: VatDetails
  )(implicit request: Request[AnyContent]): Boolean = {
    def isIomOrCharityGiant: Boolean = {
      userLocation == Iom || requestSessionData.userSelectedOrganisationType.fold(false) {
        case CdsOrganisationType.CharityPublicBodyNotForProfit => formData.isGiant
        case _ => false
      }
    }

    !appConfig.validateUkIomGiantVrnFeatureFlag && isIomOrCharityGiant
  }

  private val registrationUkSubscriptionFlows =
    Seq(OrganisationSubscriptionFlow, PartnershipSubscriptionFlow, IndividualSubscriptionFlow)

  def isRegistrationUKJourney(implicit request: Request[AnyContent]): Boolean =
    request.session.data.get(RequestSessionDataKeys.subscriptionFlow) match {
      case Some(flowName) => registrationUkSubscriptionFlows.contains(SubscriptionFlow(flowName))
      case None => false
    }

}

object RequestSessionDataKeys {
  val selectedOrganisationType = "selected-organisation-type"
  val selectedUserLocation = "selected-user-location"
  val subscriptionFlow = "subscription-flow"
  val uriBeforeSubscriptionFlow = "uri-before-subscription-flow"
  val unmatchedUser = "unmatched-user"
}
