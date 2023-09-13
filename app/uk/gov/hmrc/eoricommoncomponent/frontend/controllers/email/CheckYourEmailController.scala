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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.confirmEmailYesNoAnswerForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.CheckYourEmailService

@Singleton
class CheckYourEmailController @Inject() (
  authAction: AuthAction,
  checkYourEmailService: CheckYourEmailService,
  mcc: MessagesControllerComponents
) extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserClearingCacheOnCompletionAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        checkYourEmailService.fetchEmailAndPopulateView(userWithEnrolments, service)
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.enrolledUserClearingCacheOnCompletionAction {
      implicit request => implicit userWithEnrolments: LoggedInUserWithEnrolments =>
        confirmEmailYesNoAnswerForm()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              checkYourEmailService.handleFormWithErrors(userWithEnrolments, formWithErrors, isInReviewMode, service),
            yesNoAnswer => checkYourEmailService.locationByAnswer(yesNoAnswer, service)
          )
    }

  def verifyEmailView(service: Service): Action[AnyContent] =
    authAction.enrolledUserClearingCacheOnCompletionAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        checkYourEmailService.fetchEmailAndPopulateView(userWithEnrolments, service, emailVerificationView = true)
    }

  def emailConfirmed(service: Service): Action[AnyContent] =
    authAction.enrolledUserClearingCacheOnCompletionAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        checkYourEmailService.emailConfirmed(userWithEnrolments, service)
    }

  def emailConfirmedContinue(service: Service): Action[AnyContent] =
    Action(_ => Redirect(UserLocationController.form(service)))

}
