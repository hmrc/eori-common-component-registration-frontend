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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Company, Partnership, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.organisationTypeDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RegistrationDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.organisation_type

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationTypeController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  organisationTypeView: organisation_type,
  registrationDetailsService: RegistrationDetailsService,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def nameIdOrganisationMatching(orgType: String, service: Service): Call =
    NameIdOrganisationController.form(orgType, service)

  private def whatIsYourOrgNameMatching(orgType: String, service: Service): Call =
    WhatIsYourOrgNameController.showForm(false, orgType, service)

  private def individualMatching(orgType: String, service: Service): Call =
    NameDobController.form(orgType, service)

  private def thirdCountryIndividualMatching(orgType: String, service: Service): Call =
    RowIndividualNameDateOfBirthController.form(orgType, service)

  private def organisationWhatIsYourOrgName(orgType: String, service: Service): Call =
    WhatIsYourOrgNameController.showForm(false, orgType, service)

  private def matchingDestinations(service: Service): Map[CdsOrganisationType, Call] =
    Map[CdsOrganisationType, Call](
      Company                       -> nameIdOrganisationMatching(CompanyId, service),
      SoleTrader                    -> individualMatching(SoleTraderId, service),
      Individual                    -> individualMatching(IndividualId, service),
      Partnership                   -> nameIdOrganisationMatching(PartnershipId, service),
      LimitedLiabilityPartnership   -> nameIdOrganisationMatching(LimitedLiabilityPartnershipId, service),
      CharityPublicBodyNotForProfit -> whatIsYourOrgNameMatching(CharityPublicBodyNotForProfitId, service),
      ThirdCountryOrganisation      -> organisationWhatIsYourOrgName(ThirdCountryOrganisationId, service),
      ThirdCountrySoleTrader        -> thirdCountryIndividualMatching(ThirdCountrySoleTraderId, service),
      ThirdCountryIndividual        -> thirdCountryIndividualMatching(ThirdCountryIndividualId, service)
    )

  def form(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionDetailsService.cachedOrganisationType map { orgType =>
          def filledForm = orgType.map(organisationTypeDetailsForm.fill(_)).getOrElse(organisationTypeDetailsForm)
          requestSessionData.selectedUserLocation match {
            case Some(_) =>
              Ok(organisationTypeView(filledForm, requestSessionData.selectedUserLocation, service))
            case None => Ok(organisationTypeView(filledForm, Some("uk"), service))
          }
        }
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        organisationTypeDetailsForm.bindFromRequest.fold(
          formWithErrors => {
            val userLocation = requestSessionData.selectedUserLocation
            Future.successful(BadRequest(organisationTypeView(formWithErrors, userLocation, service)))
          },
          organisationType =>
            registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType) flatMap { ok =>
              if (ok)
                Future.successful(
                  Redirect(matchingDestinations(service)(organisationType))
                    .withSession(requestSessionData.sessionWithOrganisationTypeAdded(organisationType))
                )
              else throw new IllegalStateException(s"Unable to save $organisationType registration in cache")
            }
        )
    }

}
