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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  VatDetailsEuConfirmController,
  VatRegisteredEuController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.removeVatYesNoAnswer
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionVatEUDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.are_you_sure_remove_vat

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AreYouSureYouWantToDeleteVatController @Inject() (
  authAction: AuthAction,
  subscriptionVatEUDetailsService: SubscriptionVatEUDetailsService,
  mcc: MessagesControllerComponents,
  areYouSureRemoveVatView: are_you_sure_remove_vat
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(index: Int, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionVatEUDetailsService.vatEuDetails(index) map {
        case Some(vatDetails) =>
          Ok(areYouSureRemoveVatView(removeVatYesNoAnswer, service, vatDetails, isInReviewMode = false))
        case _ => Redirect(VatDetailsEuConfirmController.createForm(service))
      }
    }

  def reviewForm(index: Int, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionVatEUDetailsService.vatEuDetails(index) map {
        case Some(vatDetails) =>
          Ok(areYouSureRemoveVatView(removeVatYesNoAnswer, service, vatDetails, isInReviewMode = true))
        case _ => Redirect(VatDetailsEuConfirmController.reviewForm(service))
      }
    }

  def submit(index: Int, service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionVatEUDetailsService.vatEuDetails(index) flatMap {
        case Some(vatDetails) =>
          removeVatYesNoAnswer
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future
                  .successful(BadRequest(areYouSureRemoveVatView(formWithErrors, service, vatDetails, isInReviewMode))),
              yesNoAnswer =>
                if (yesNoAnswer.isYes)
                  subscriptionVatEUDetailsService
                    .removeSingleEuVatDetails(vatDetails) flatMap (_ => redirectToVatConfirm(service, isInReviewMode))
                else redirectToVatConfirm(service, isInReviewMode)
            )
        case _ => throw new IllegalStateException("Vat details for remove not found")
      }
    }

  private def redirectToVatConfirm(service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionVatEUDetailsService.cachedEUVatDetails map {
      case Seq() =>
        if (isInReviewMode)
          Redirect(VatRegisteredEuController.reviewForm(service))
        else
          Redirect(VatRegisteredEuController.createForm(service))
      case _ =>
        if (isInReviewMode)
          Redirect(VatDetailsEuConfirmController.reviewForm(service))
        else
          Redirect(VatDetailsEuConfirmController.createForm(service))
    }

}
