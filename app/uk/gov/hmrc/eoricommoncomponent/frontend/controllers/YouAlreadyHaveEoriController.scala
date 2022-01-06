/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.you_already_have_eori
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

class YouAlreadyHaveEoriController @Inject() (
  authAction: AuthAction,
  eoriExistsView: you_already_have_eori,
  mcc: MessagesControllerComponents
) extends FrontendController(mcc) with I18nSupport {

  // Note: permitted for user with service enrolment
  def display(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithServiceAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(eoriExistsView(service)))
    }

}
