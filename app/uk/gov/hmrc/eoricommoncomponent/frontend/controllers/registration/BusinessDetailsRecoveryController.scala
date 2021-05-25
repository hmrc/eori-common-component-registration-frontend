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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.BusinessDetailsRecoveryPage
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_details_recovery
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsRecoveryController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  sessionCache: SessionCache,
  mcc: MessagesControllerComponents,
  businessDetailsRecoveryView: business_details_recovery,
  save4LaterService: Save4LaterService,
  subscriptionFlowManager: SubscriptionFlowManager
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        regDetails <- sessionCache.registrationDetails
      } yield regDetails match {
        case individual: RegistrationDetailsIndividual =>
          Ok(businessDetailsRecoveryView(individual.name, concatenateAddress(individual), true, service))
        case org: RegistrationDetailsOrganisation =>
          Ok(businessDetailsRecoveryView(org.name, concatenateAddress(org), false, service))
        case _ =>
          throw new IllegalArgumentException("Required RegistrationDetailsIndividual | RegistrationDetailsOrganisation")
      }
    }

  def continue(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => userId: LoggedInUserWithEnrolments =>
      {
        for {
          regDetails <- sessionCache.registrationDetails
          orgType    <- save4LaterService.fetchOrgType(GroupId(userId.groupId))
        } yield {
          val location =
            requestSessionData.selectedUserLocation.getOrElse(throw new IllegalStateException("Location not set"))
          regDetails match {
            case _: RegistrationDetailsIndividual =>
              continueBasedOnJourney(service, journey, location, orgType)
            case _: RegistrationDetailsOrganisation =>
              continueBasedOnJourney(service, journey, location, orgType)
            case _ =>
              throw new IllegalArgumentException(
                "Required RegistrationDetailsIndividual | RegistrationDetailsOrganisation"
              )

          }
        }
      }.flatMap(identity)
    }

  private def continueBasedOnJourney(
    service: Service,
    journey: Journey.Value,
    location: String,
    orgType: Option[CdsOrganisationType]
  )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {

    def startSubscription: CdsOrganisationType => Future[Result] =
      organisationType => {
        subscriptionFlowManager.startSubscriptionFlow(
          Some(BusinessDetailsRecoveryPage),
          organisationType,
          service,
          journey
        ) map {
          case (page, newSession) =>
            val sessionWithOrganisationType = requestSessionData
              .sessionWithOrganisationTypeAdded(newSession, organisationType)
            val session =
              requestSessionData.existingSessionWithUserLocationAdded(
                sessionWithOrganisationType,
                sessionInfoBasedOnJourney(journey, Some(location))
              )
            Redirect(page.url(service)).withSession(session)
        }
      }

    startSubscription(orgType.getOrElse(throw new IllegalStateException("OrganisationType not found in cache")))
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
          case _                                    => throw new IllegalStateException("User Location not set")
        }
      case _ =>
        location.getOrElse(throw new IllegalStateException("User Location not set"))
    }

  private def concatenateAddress(registrationDetails: RegistrationDetails): AddressViewModel =
    AddressViewModel(registrationDetails.address)

}
