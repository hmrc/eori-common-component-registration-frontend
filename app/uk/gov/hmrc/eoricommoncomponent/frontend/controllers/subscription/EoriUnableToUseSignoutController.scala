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

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.eoriSignoutYesNoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_signout

import scala.concurrent.Future

class EoriUnableToUseSignoutController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  eoriSignoutPage: eori_signout
) extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(eoriSignoutPage(service, eoriSignoutYesNoForm())))
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      eoriSignoutYesNoForm().bindFromRequest().fold(
        formWithError => Future.successful(BadRequest(eoriSignoutPage(service, formWithError))),
        answer =>
          if (answer.isYes)
            Future.successful(
              Redirect(
                uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController.logout(
                  service,
                  Journey.Subscribe
                )
              )
            )
          else Future.successful(Redirect(routes.EoriUnableToUseController.displayPage(service)))
      )
    }

}
