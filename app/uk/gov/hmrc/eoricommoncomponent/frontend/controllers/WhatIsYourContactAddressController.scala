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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ContactAddressController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.EmbassyId
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{ContactAddressMatchModel, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactAddressFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_contact_address

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIsYourContactAddressController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  requestSessionData: RequestSessionData,
  subscriptionDetailsService: SubscriptionDetailsService,
  what_is_your_contact_address_view: what_is_your_contact_address,
  contactAddressFormProvider: ContactAddressFormProvider
)(implicit executionContext: ExecutionContext)
    extends CdsController(mcc) {

  private val contactAddressForm: Form[ContactAddressMatchModel] = contactAddressFormProvider.contactAddressForm

  def showForm(isInReviewMode: Boolean = false, service: Service): Action[AnyContent] = {
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      val (countriesToInclude, countriesInCountryPicker) =
        Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)

      Future.successful(
        Ok(
          what_is_your_contact_address_view(
            isInReviewMode,
            contactAddressForm,
            countriesToInclude,
            countriesInCountryPicker,
            EmbassyId,
            service
          )
        )
      )
    }
  }

  def submit(isInReviewMode: Boolean = false, service: Service): Action[AnyContent] = {
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      val filledForm = contactAddressForm.bindFromRequest()

      val (countriesToInclude, countriesInCountryPicker) =
        Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)

      if (filledForm.hasErrors)
        Future.successful(
          BadRequest(
            what_is_your_contact_address_view(
              isInReviewMode,
              filledForm,
              countriesToInclude,
              countriesInCountryPicker,
              EmbassyId,
              service
            )
          )
        )
      else {
        for {
          _ <- subscriptionDetailsService.cacheAddressDetails(filledForm.value.head)
        } yield Redirect(ContactAddressController.submit(isInReviewMode, service))
      }
    }
  }

}
