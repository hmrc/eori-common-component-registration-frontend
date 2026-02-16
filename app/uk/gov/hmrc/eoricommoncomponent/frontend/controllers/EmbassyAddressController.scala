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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.EmbassyId
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EmbassyAddressMatchModel, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.embassy.EmbassyAddressForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegistrationDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.embassy_address

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmbassyAddressController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  regDetailsCreator: RegistrationDetailsCreator,
  registrationDetailsService: RegistrationDetailsService,
  subscriptionFlowManager: SubscriptionFlowManager,
  embassyAddressForm: EmbassyAddressForm,
  embassy_address_view: embassy_address
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def showForm(isInReviewMode: Boolean = false, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      sessionCache.registrationDetails.map { regDetails =>
        val addr = regDetails.address
        val embassyAddrModel = EmbassyAddressMatchModel(
          addr.addressLine1,
          addr.addressLine2,
          addr.addressLine3.getOrElse(""),
          addr.postalCode.getOrElse(""),
          addr.countryCode
        )
        val filledForm = embassyAddressForm.form.fill(embassyAddrModel)
        val (countriesToInclude, countriesInCountryPicker) =
          Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)
        Ok(
          embassy_address_view(
            isInReviewMode,
            filledForm,
            countriesToInclude,
            countriesInCountryPicker,
            EmbassyId,
            service
          )
        )
      }
    }

  def submit(isInReviewMode: Boolean = false, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      val filledForm = embassyAddressForm.form.bindFromRequest()

      val (countriesToInclude, countriesInCountryPicker) =
        Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)

      if (filledForm.hasErrors)
        Future.successful(
          BadRequest(
            embassy_address_view(
              isInReviewMode,
              filledForm,
              countriesToInclude,
              countriesInCountryPicker,
              EmbassyId,
              service
            )
          )
        )
      else if (isInReviewMode)
        registrationDetailsService
          .cacheAddress(regDetailsCreator.registrationAddressEmbassyAddress(filledForm.value.head))
          .map(_ => Redirect(routes.DetermineReviewPageController.determineRoute(service)))
      else
        registrationDetailsService
          .cacheAddress(
            regDetailsCreator.registrationAddressEmbassyAddress(filledForm.value.head)
          )
          .flatMap { _ =>
            subscriptionFlowManager.startSubscriptionFlow(service)
          } map { case (firstSubscriptionPage, session) =>
          Redirect(firstSubscriptionPage.url(service)).withSession(session)
        }
    }

}
