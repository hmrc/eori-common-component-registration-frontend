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

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Company, Individual, LimitedLiabilityPartnership, Partnership, SoleTrader}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, ContactAddressMatchModel, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactAddressFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, RequestSessionData}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RegistrationDetailsService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_organisations_address

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIsYourOrganisationsAddressController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  requestSessionData: RequestSessionData,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionFlowManager: SubscriptionFlowManager,
  registrationDetailsService: RegistrationDetailsService,
  what_is_your_organisations_address_view: what_is_your_organisations_address,
  contactAddressFormProvider: ContactAddressFormProvider
)(implicit executionContext: ExecutionContext)
    extends CdsController(mcc) {

  private val contactAddressForm: Form[ContactAddressMatchModel] = contactAddressFormProvider.contactAddressForm

  def showForm(isInReviewMode: Boolean = false, service: Service): Action[AnyContent] = {
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      val (countriesToInclude, countriesInCountryPicker) =
        Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)

      subscriptionDetailsService.cachedOrganisationType.map { optOrgType =>
        val orgType = optOrgType.getOrElse(throw DataUnavailableException("organisation type unavailable"))
        Ok(
          what_is_your_organisations_address_view(
            isInReviewMode,
            contactAddressForm,
            countriesToInclude,
            countriesInCountryPicker,
            orgType.id,
            service
          )
        )
      }
    }
  }

  def submit(isInReviewMode: Boolean = false, service: Service): Action[AnyContent] = {
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedOrganisationType.flatMap { optOrgType =>
        val orgType = optOrgType.getOrElse(throw DataUnavailableException("organisation type unavailable"))

        val filledForm = contactAddressForm.bindFromRequest()

        val (countriesToInclude, countriesInCountryPicker) =
          Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)

        if (filledForm.hasErrors)
          Future.successful(
            BadRequest(
              what_is_your_organisations_address_view(
                isInReviewMode,
                filledForm,
                countriesToInclude,
                countriesInCountryPicker,
                orgType.id,
                service
              )
            )
          )
        else {
          val addr = filledForm.value.head
          registrationDetailsService
            .cacheAddress(
              Address(addr.lineOne, addr.lineTwo, Some(addr.townCity), None, Some(addr.postcode), addr.country)
            )
            .flatMap { _ =>
              subscriptionDetailsService
                .cacheAddressDetails(filledForm.value.head)
                .flatMap { _ =>
                  subscriptionDetailsService.cachedUtrMatch.flatMap { optUtrMatchModel =>
                    val hasUtr = optUtrMatchModel.exists(_.haveUtr.contains(true))
                    subscriptionFlowManager.startSubscriptionFlowWithPage(
                      None,
                      service,
                      redirectFlowLocation(orgType, hasUtr)
                    )
                  }
                }
                .map { case (flowPageOne, session) =>
                  Redirect(flowPageOne.url(service)).withSession(session)
                }

            }
        }
      }
    }
  }

  private def redirectFlowLocation(cdsOrganisationType: CdsOrganisationType, hasUtr: Boolean)(implicit
    request: Request[AnyContent]
  ): SubscriptionFlow = {
    (requestSessionData.selectedUserLocation, cdsOrganisationType) match {
      case (Some(UserLocation.Iom), Partnership) => PartnershipSubscriptionFlowIom
      case (Some(UserLocation.Iom), Individual) => IndividualSoleTraderFlowIom
      case (Some(UserLocation.Iom), SoleTrader) => IndividualSoleTraderFlowIom
      case (Some(UserLocation.Iom), Company) => CompanyLlpFlowIom
      case (Some(UserLocation.Iom), LimitedLiabilityPartnership) => CompanyLlpFlowIom
      case _ =>
        if (hasUtr) {
          CharityPublicBodySubscriptionFlow
        } else {
          CharityPublicBodySubscriptionNoUtrFlow
        }
    }
  }

}
