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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.BusinessDetailsRecoveryPage
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  DataUnavailableException,
  RequestSessionData,
  SessionCache
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.business_details_recovery

import javax.inject.{Inject, Singleton}
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

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
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

  def continue(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => userId: LoggedInUserWithEnrolments =>
      {
        for {
          regDetails <- sessionCache.registrationDetails
          orgType    <- save4LaterService.fetchOrgType(GroupId(userId.groupId))
        } yield {
          val location =
            requestSessionData.selectedUserLocation.getOrElse(throw DataUnavailableException("User Location not set"))
          regDetails match {
            case _: RegistrationDetailsIndividual =>
              continue(service, location, orgType)
            case _: RegistrationDetailsOrganisation =>
              continue(service, location, orgType)
            case _ =>
              throw new IllegalArgumentException(
                "Required RegistrationDetailsIndividual | RegistrationDetailsOrganisation"
              )

          }
        }
      }.flatMap(identity)
    }

  private def continue(service: Service, location: String, orgType: Option[CdsOrganisationType])(implicit
    request: Request[AnyContent]
  ): Future[Result] = {

    val organisationType = orgType.getOrElse(throw DataUnavailableException("OrganisationType not found in cache"))

    subscriptionFlowManager.startSubscriptionFlow(Some(BusinessDetailsRecoveryPage), organisationType, service) map {
      case (page, newSession) =>
        val sessionWithOrganisationType = requestSessionData
          .sessionWithOrganisationTypeAdded(newSession, organisationType)
        val session =
          requestSessionData.existingSessionWithUserLocationAdded(sessionWithOrganisationType, location)
        Redirect(page.url(service)).withSession(session)
    }
  }

  private def concatenateAddress(registrationDetails: RegistrationDetails): AddressViewModel =
    AddressViewModel(registrationDetails.address)

}
