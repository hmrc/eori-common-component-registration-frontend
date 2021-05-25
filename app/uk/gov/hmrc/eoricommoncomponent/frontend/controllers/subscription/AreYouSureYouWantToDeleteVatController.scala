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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.{
  VatDetailsEuConfirmController,
  VatRegisteredEuController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.removeVatYesNoAnswer
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionVatEUDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.are_you_sure_remove_vat

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AreYouSureYouWantToDeleteVatController @Inject() (
  authAction: AuthAction,
  subscriptionVatEUDetailsService: SubscriptionVatEUDetailsService,
  mcc: MessagesControllerComponents,
  areYouSureRemoveVatView: are_you_sure_remove_vat
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(index: Int, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionVatEUDetailsService.vatEuDetails(index) map {
        case Some(vatDetails) =>
          Ok(areYouSureRemoveVatView(removeVatYesNoAnswer, service, journey, vatDetails, isInReviewMode = false))
        case _ => Redirect(VatDetailsEuConfirmController.createForm(service, journey))
      }
    }

  def reviewForm(index: Int, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionVatEUDetailsService.vatEuDetails(index) map {
        case Some(vatDetails) =>
          Ok(areYouSureRemoveVatView(removeVatYesNoAnswer, service, journey, vatDetails, isInReviewMode = true))
        case _ => Redirect(VatDetailsEuConfirmController.reviewForm(service, journey))
      }
    }

  def submit(index: Int, service: Service, journey: Journey.Value, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionVatEUDetailsService.vatEuDetails(index) flatMap {
        case Some(vatDetails) =>
          removeVatYesNoAnswer
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future
                  .successful(
                    BadRequest(areYouSureRemoveVatView(formWithErrors, service, journey, vatDetails, isInReviewMode))
                  ),
              yesNoAnswer =>
                if (yesNoAnswer.isYes)
                  subscriptionVatEUDetailsService
                    .removeSingleEuVatDetails(vatDetails) flatMap (
                    _ => redirectToVatConfirm(service, journey, isInReviewMode)
                  )
                else redirectToVatConfirm(service, journey, isInReviewMode)
            )
        case _ => throw new IllegalStateException("Vat details for remove not found")
      }
    }

  private def redirectToVatConfirm(service: Service, journey: Journey.Value, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionVatEUDetailsService.cachedEUVatDetails map {
      case Seq() =>
        if (isInReviewMode)
          Redirect(VatRegisteredEuController.reviewForm(service, journey))
        else
          Redirect(VatRegisteredEuController.createForm(service, journey))
      case _ =>
        if (isInReviewMode)
          Redirect(VatDetailsEuConfirmController.reviewForm(service, journey))
        else
          Redirect(VatDetailsEuConfirmController.createForm(service, journey))
    }

}
