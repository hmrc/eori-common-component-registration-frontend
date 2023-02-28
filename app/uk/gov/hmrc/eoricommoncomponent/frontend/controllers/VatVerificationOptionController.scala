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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{vat_verification_option}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatVerificationOptionController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  vatVerificationView: vat_verification_option,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(
          Ok(vatVerificationView(vatVerificationOptionYesNoAnswerForm(), isInReviewMode = false, service))
        )
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          useVatRegisteredDate <- subscriptionBusinessService.getCachedVatVerificationOption
          yesNo: YesNo = YesNo(useVatRegisteredDate)
        } yield Ok(
          vatVerificationView(vatVerificationOptionYesNoAnswerForm().fill(yesNo), isInReviewMode = true, service)
        )
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatVerificationOptionYesNoAnswerForm()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(vatVerificationView(formWithErrors, isInReviewMode, service))),
          yesNoAnswer =>
            subscriptionDetailsService.cacheVatVerificationOption(yesNoAnswer).flatMap {
              _ =>
                if (isInReviewMode && yesNoAnswer.isYes)
                  // TODO: The date you became VAT registered
                  Future.successful(Redirect(VatDetailsController.reviewForm(service)))
                else if (yesNoAnswer.isNo) Future.successful(Redirect(VatReturnController.createForm(service)))
                else
                  // TODO: Your latest VAT Return total
                  Future.successful(Redirect(routes.VatGroupsCannotRegisterUsingThisServiceController.form(service)))
            }
        )
    }

}
