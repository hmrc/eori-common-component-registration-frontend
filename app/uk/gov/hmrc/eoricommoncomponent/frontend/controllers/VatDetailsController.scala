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
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  InvalidResponse,
  NotFoundResponse,
  ServiceUnavailableResponse,
  VatControlListConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, VatControlListRequest}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.VatRegistrationDateFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsForm.vatDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatDetailsController @Inject() (
  authAction: AuthAction,
  vatControlListConnector: VatControlListConnector,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  vatDetailsView: vat_details,
  errorTemplate: error_template,
  weCannotConfirmYourIdentity: date_of_vat_registration,
  subscriptionDetailsService: SubscriptionDetailsService,
  form: VatRegistrationDateFormProvider
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  val dateForm = form()

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(vatDetailsView(vatDetailsForm, isInReviewMode = false, service)))
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.getCachedUkVatDetails.map {
          case Some(vatDetails) =>
            Ok(vatDetailsView(vatDetailsForm.fill(vatDetails), isInReviewMode = true, service))
          case None => Ok(vatDetailsView(vatDetailsForm, isInReviewMode = true, service))
        }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatDetailsForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(vatDetailsView(formWithErrors, isInReviewMode, service))),
        formData => lookupVatDetails(formData, isInReviewMode, service)
      )
    }

  private def lookupVatDetails(vatForm: VatDetails, isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    vatControlListConnector.vatControlList(VatControlListRequest(vatForm.number)).flatMap {
      case Right(vatControlListResponse) =>
        if (vatControlListResponse.isPostcodeAssociatedWithVrn(vatForm))
          subscriptionDetailsService
            .cacheUkVatDetails(vatForm)
            .map {
              _ =>
                subscriptionDetailsService.cacheVatControlListResponse(vatControlListResponse)
                if (isInReviewMode)
                  Redirect(
                    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
                      .determineRoute(service)
                  )
                else if (vatControlListResponse.isLastReturnMonthPeriodNonEmpty)
                  Redirect(DateOfVatRegistrationController.createForm(service))
                else
                  Redirect(DateOfVatRegistrationController.createForm(service))
            }
        else
          Future.successful(Redirect(VatDetailsController.vatDetailsNotMatched(service)))
      case Left(errorResponse) =>
        errorResponse match {
          case NotFoundResponse =>
            Future.successful(Redirect(VatDetailsController.vatDetailsNotMatched(service)))
          case InvalidResponse =>
            Future.successful(Redirect(VatDetailsController.vatDetailsNotMatched(service)))
          case ServiceUnavailableResponse => Future.successful(Results.ServiceUnavailable(errorTemplate()))
        }
    }

  def vatDetailsNotMatched(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(weCannotConfirmYourIdentity(dateForm, service)))
    }

}
