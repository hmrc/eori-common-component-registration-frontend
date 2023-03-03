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

import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatReturnTotal
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatReturnTotalForm.vatReturnTotalForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{ContactDetailsController, VatDetailsController}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatReturnController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  vatReturnTotalView: vat_return_total,
  weCannotConfirmYourIdentity: we_cannot_confirm_your_identity
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(vatReturnTotalView(vatReturnTotalForm, service)))
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatReturnTotalForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(vatReturnTotalView(formWithErrors, service))),
        formData => lookupVatReturn(formData, service)
      )
    }

  private def lookupVatReturn(vatReturnTotal: VatReturnTotal, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionBusinessService.getCachedVatControlListResponse.map {
      case Some(response)
          //TODO: redirect to date page as response is empty
          if response.lastNetDue.getOrElse(
            redirectToCannotConfirmIdentity(service)
          ) == vatReturnTotal.returnAmountInput.toDouble =>
        subscriptionDetailsService.cacheUserVatAmountInput(vatReturnTotal.returnAmountInput)
        Redirect(ContactDetailsController.createForm(service))
      case _ => redirectToCannotConfirmIdentity(service)
    }

  private def redirectToCannotConfirmIdentity(service: Service)(implicit request: Request[AnyContent]): Result = Ok(
    weCannotConfirmYourIdentity(isInReviewMode = false, VatDetailsController.createForm(service).url, service)
  )

}
