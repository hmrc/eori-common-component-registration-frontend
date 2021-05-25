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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Company, Partnership, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.organisationTypeDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.organisation_type

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationTypeController @Inject() (
  authAction: AuthAction,
  subscriptionFlowManager: SubscriptionFlowManager,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  organisationTypeView: organisation_type,
  registrationDetailsService: RegistrationDetailsService,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def nameIdOrganisationMatching(orgType: String, service: Service, journey: Journey.Value): Call =
    NameIdOrganisationController.form(orgType, service, journey)

  private def whatIsYourOrgNameMatching(orgType: String, service: Service, journey: Journey.Value): Call =
    WhatIsYourOrgNameController.showForm(false, orgType, service, journey)

  private def individualMatching(orgType: String, service: Service, journey: Journey.Value): Call =
    NameDobController.form(orgType, service, journey)

  private def thirdCountryIndividualMatching(orgType: String, service: Service, journey: Journey.Value): Call =
    RowIndividualNameDateOfBirthController.form(orgType, service, journey)

  private def organisationWhatIsYourOrgName(orgType: String, service: Service, journey: Journey.Value): Call =
    WhatIsYourOrgNameController.showForm(false, orgType, service, journey)

  private def matchingDestinations(service: Service, journey: Journey.Value): Map[CdsOrganisationType, Call] =
    Map[CdsOrganisationType, Call](
      Company                       -> nameIdOrganisationMatching(CompanyId, service, journey),
      SoleTrader                    -> individualMatching(SoleTraderId, service, journey),
      Individual                    -> individualMatching(IndividualId, service, journey),
      Partnership                   -> nameIdOrganisationMatching(PartnershipId, service, journey),
      LimitedLiabilityPartnership   -> nameIdOrganisationMatching(LimitedLiabilityPartnershipId, service, journey),
      CharityPublicBodyNotForProfit -> whatIsYourOrgNameMatching(CharityPublicBodyNotForProfitId, service, journey),
      ThirdCountryOrganisation      -> organisationWhatIsYourOrgName(ThirdCountryOrganisationId, service, journey),
      ThirdCountrySoleTrader        -> thirdCountryIndividualMatching(ThirdCountrySoleTraderId, service, journey),
      ThirdCountryIndividual        -> thirdCountryIndividualMatching(ThirdCountryIndividualId, service, journey)
    )

  def form(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionDetailsService.cachedOrganisationType map { orgType =>
          def filledForm = orgType.map(organisationTypeDetailsForm.fill(_)).getOrElse(organisationTypeDetailsForm)
          requestSessionData.selectedUserLocation match {
            case Some(_) =>
              Ok(organisationTypeView(filledForm, requestSessionData.selectedUserLocation, service, journey))
            case None => Ok(organisationTypeView(filledForm, Some("uk"), service, journey))
          }
        }
    }

  def submit(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request =>
        def startSubscription: CdsOrganisationType => Future[Result] = { organisationType =>
          subscriptionFlowManager.startSubscriptionFlow(
            cdsOrganisationType = organisationType,
            service = service,
            journey = journey
          ) map {
            case (page, newSession) =>
              val session = requestSessionData.sessionWithOrganisationTypeAdded(newSession, organisationType)
              Redirect(page.url(service)).withSession(session)
          }
        }

        _: LoggedInUserWithEnrolments =>
          organisationTypeDetailsForm.bindFromRequest.fold(
            formWithErrors => {
              val userLocation = requestSessionData.selectedUserLocation
              Future.successful(BadRequest(organisationTypeView(formWithErrors, userLocation, service, journey)))
            },
            organisationType =>
              journey match {
                case Journey.Subscribe =>
                  registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType) flatMap { ok =>
                    if (ok) startSubscription(organisationType)
                    else throw new IllegalStateException(s"Unable to save $organisationType registration in cache")
                  }
                case Journey.Register =>
                  registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType) flatMap { ok =>
                    if (ok)
                      Future.successful(
                        Redirect(matchingDestinations(service, journey)(organisationType))
                          .withSession(requestSessionData.sessionWithOrganisationTypeAdded(organisationType))
                      )
                    else throw new IllegalStateException(s"Unable to save $organisationType registration in cache")
                  }
              }
          )
    }

}
