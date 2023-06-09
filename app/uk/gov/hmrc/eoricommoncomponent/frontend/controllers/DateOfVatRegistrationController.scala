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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.VatRegistrationDate
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.VatRegistrationDateForm.vatRegistrationDateForm
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionBusinessService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{date_of_vat_registration, we_cannot_confirm_your_identity}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DateOfVatRegistrationController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  dateOfVatRegistrationView: date_of_vat_registration,
  weCannotConfirmYourIdentity: we_cannot_confirm_your_identity
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(dateOfVatRegistrationView(vatRegistrationDateForm, service)))
    }

  private def lookupDateOfVatRegistration(vatRegistrationDateInput: VatRegistrationDate, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionBusinessService.getCachedVatControlListResponse.map {
      case Some(response)
          if LocalDate.parse(response.dateOfReg.getOrElse("")) == vatRegistrationDateInput.dateOfRegistration =>
        Redirect(ContactDetailsController.createForm(service))
      case _ => Redirect(DateOfVatRegistrationController.redirectToCannotConfirmIdentity(service))
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatRegistrationDateForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(dateOfVatRegistrationView(formWithErrors, service))),
        formData => lookupDateOfVatRegistration(formData, service)
      )
    }

  def redirectToCannotConfirmIdentity(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(
        Ok(weCannotConfirmYourIdentity(isInReviewMode = false, VatDetailsController.createForm(service).url, service))
      )
    }

}
