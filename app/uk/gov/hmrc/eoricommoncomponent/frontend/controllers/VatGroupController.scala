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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.EmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_group

@Singleton
class VatGroupController @Inject() (mcc: MessagesControllerComponents, vatGroupView: vat_group)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] = Action { implicit request =>
    Ok(vatGroupView(vatGroupYesNoAnswerForm(), service))
  }

  def submit(service: Service): Action[AnyContent] = Action { implicit request =>
    vatGroupYesNoAnswerForm()
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(vatGroupView(formWithErrors, service)),
        yesNoAnswer =>
          if (yesNoAnswer.isNo)
            // TODO - need service url param here
            Redirect(EmailController.form(service))
          else
            Redirect(routes.VatGroupsCannotRegisterUsingThisServiceController.form(service))
      )
  }

}
