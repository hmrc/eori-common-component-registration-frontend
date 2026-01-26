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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_verification_option

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatVerificationOptionController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  subscriptionDetailsService: SubscriptionDetailsService,
  vatVerificationView: vat_verification_option
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(vatVerificationView(vatVerificationOptionAnswerForm(), service)))
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      vatVerificationOptionAnswerForm()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(vatVerificationView(formWithErrors, service))),
          vatVerificationOption =>
            subscriptionDetailsService.cacheVatVerificationOption(vatVerificationOption).flatMap { _ =>
              vatVerificationOption.isDateOption match {
                case true => Future.successful(Redirect(DateOfVatRegistrationController.createForm(service)))
                case false => Future.successful(Redirect(VatReturnController.createForm(service)))
              }
            }
        )
    }

}
