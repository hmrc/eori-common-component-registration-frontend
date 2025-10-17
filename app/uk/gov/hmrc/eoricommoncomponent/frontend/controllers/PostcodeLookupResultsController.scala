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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{ConfirmContactDetailsController, ManualAddressController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressResultsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.postcodelookup.PostcodeLookupService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.postcode_address_result

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostcodeLookupResultsController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  addressLookupResultsPage: postcode_address_result,
  addressResultsForm: AddressResultsForm,
  postcodeLookupService: PostcodeLookupService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      postcodeLookupService.lookup().map {
        case Some((addressLookupSuccess, postcodeViewModel)) =>
          Ok(addressLookupResultsPage(addressResultsForm.form(addressLookupSuccess.addresses), postcodeViewModel, addressLookupSuccess.addresses, service))
        case None => Redirect(ManualAddressController.createForm(service))
      }
    }

  def submit(service: Service): Action[AnyContent] = {
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      postcodeLookupService.lookupNoRepeat().flatMap {
        case None => Future.successful(Redirect(ManualAddressController.createForm(service)))
        case Some((addressLookupSuccess, postcodeViewModel)) => {
          addressResultsForm
            .form(addressLookupSuccess.addresses)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(addressLookupResultsPage(formWithErrors, postcodeViewModel, addressLookupSuccess.addresses, service))),
              (address: Address) => {
                postcodeLookupService
                  .ensuringAddressPopulated(address)
                  .flatMap(_ => Future.successful(Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false))))
              }
            )
        }
      }
    }
  }
}
