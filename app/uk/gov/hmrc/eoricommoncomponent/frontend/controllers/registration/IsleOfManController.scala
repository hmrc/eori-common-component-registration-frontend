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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.VatRegisteredUkController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatGroupController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.isleOfManYesNoAnswerForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.isle_of_man

@Singleton
class IsleOfManController @Inject() (view: isle_of_man, mcc: MessagesControllerComponents) extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] = Action { implicit request =>
    Ok(view(isleOfManYesNoAnswerForm(), service))
  }

  def submit(service: Service): Action[AnyContent] = Action { implicit request =>
    isleOfManYesNoAnswerForm()
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(view(formWithErrors, service)),
        isleOfManYesNoAnswerForm => destinationsByAnswer(isleOfManYesNoAnswerForm, service)
      )
  }

  private def destinationsByAnswer(yesNoAnswer: YesNo, service: Service): Result = yesNoAnswer match {
    case theAnswer if theAnswer.isYes => Redirect(VatRegisteredUkController.form(service))
    case _                            => Redirect(VatGroupController.createForm(service, Journey.Register))
  }

}
