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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Company, Partnership, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.OrganisationTypeDetailsFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, RequestSessionData}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RegistrationDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.organisation_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrganisationTypeController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  organisationTypeView: organisation_type,
  registrationDetailsService: RegistrationDetailsService,
  subscriptionDetailsService: SubscriptionDetailsService,
  appConfig: AppConfig,
  organisationTypeDetailsFormProvider: OrganisationTypeDetailsFormProvider
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val form = organisationTypeDetailsFormProvider.form()

  private def nameIdOrganisationMatching(orgType: String, service: Service, userLocation: UserLocation): Call = {
    if ((userLocation == UserLocation.Iom || orgType == CharityPublicBodyNotForProfitId) && appConfig.allowNoIdJourney) {
      WhatIsYourOrgNameController.showForm(isInReviewMode = false, orgType, service)
    } else {
      NameIdOrganisationController.form(orgType, service)
    }
  }

  private def individualMatching(orgType: String, service: Service): Call =
    NameDobController.form(orgType, service)

  private def thirdCountryIndividualMatching(orgType: String, service: Service): Call =
    RowIndividualNameDateOfBirthController.form(orgType, service)

  private def organisationWhatIsYourOrgName(orgType: String, service: Service): Call =
    WhatIsYourOrgNameController.showForm(isInReviewMode = false, orgType, service)

  def embassyMatching(orgType: String, service: Service): Call =
    EmbassyNameController.showForm(isInReviewMode = false, orgType, service)

  private def matchingDestinations(service: Service, userLocation: UserLocation): Map[CdsOrganisationType, Call] =
    Map[CdsOrganisationType, Call](
      Company                       -> nameIdOrganisationMatching(CompanyId, service, userLocation),
      SoleTrader                    -> individualMatching(SoleTraderId, service),
      Individual                    -> individualMatching(IndividualId, service),
      Partnership                   -> nameIdOrganisationMatching(PartnershipId, service, userLocation),
      LimitedLiabilityPartnership   -> nameIdOrganisationMatching(LimitedLiabilityPartnershipId, service, userLocation),
      CharityPublicBodyNotForProfit -> nameIdOrganisationMatching(
        CharityPublicBodyNotForProfitId,
        service,
        userLocation
      ),
      ThirdCountryOrganisation      -> organisationWhatIsYourOrgName(ThirdCountryOrganisationId, service),
      ThirdCountrySoleTrader        -> thirdCountryIndividualMatching(ThirdCountrySoleTraderId, service),
      ThirdCountryIndividual        -> thirdCountryIndividualMatching(ThirdCountryIndividualId, service),
      Embassy                       -> embassyMatching(EmbassyId, service)
    )

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      subscriptionDetailsService.cachedOrganisationType map { orgType =>
        def filledForm: Form[CdsOrganisationType] = orgType.map(form.fill).getOrElse(form)
        requestSessionData.selectedUserLocation match {
          case Some(_) =>
            Ok(
              organisationTypeView(
                filledForm,
                requestSessionData.selectedUserLocation,
                appConfig.allowNoIdJourney,
                service
              )
            )
          case None =>
            Ok(organisationTypeView(filledForm, Some(UserLocation.Uk), appConfig.allowNoIdJourney, service))
        }
      }
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val userLocation = requestSessionData.selectedUserLocation
            Future.successful(
              BadRequest(organisationTypeView(formWithErrors, userLocation, appConfig.allowNoIdJourney, service))
            )
          },
          organisationType => {
            val userLocation =
              requestSessionData.selectedUserLocation.getOrElse(throw DataUnavailableException("User Location not set"))
            registrationDetailsService.initialiseCacheWithRegistrationDetails(organisationType) flatMap { ok =>
              if (ok)
                Future.successful(
                  Redirect(matchingDestinations(service, userLocation)(organisationType))
                    .withSession(requestSessionData.sessionWithOrganisationTypeAdded(organisationType))
                )
              else throw new IllegalStateException(s"Unable to save $organisationType registration in cache")
            }
          }
        )
    }

}
