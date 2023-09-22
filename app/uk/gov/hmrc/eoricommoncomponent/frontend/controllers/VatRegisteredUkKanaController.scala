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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.vatRegisteredUkYesNoAnswerForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_registered_uk_kana

import javax.inject.{Inject, Singleton}

@Singleton
class VatRegisteredUkKanaController @Inject() (
  vatRegisteredUkView: vat_registered_uk_kana,
  mcc: MessagesControllerComponents
) extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] = Action { implicit request =>
    Ok(vatRegisteredUkView(vatRegisteredUkYesNoAnswerForm(), service = service))
  }

  def submit(service: Service): Action[AnyContent] = Action { implicit request =>
    vatRegisteredUkYesNoAnswerForm()
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(vatRegisteredUkView(formWithErrors, service = service)),
        vatRegisteredUkYesNoAnswerForm => Redirect(destinationsByAnswer(vatRegisteredUkYesNoAnswerForm))
      )
  }

  def destinationsByAnswer(yesNoAnswer: YesNo): String = yesNoAnswer match {
    case theAnswer if theAnswer.isYes => "https://www.tax.service.gov.uk/shortforms/form/EORIVAT?details=&vat=yes"
    case _                            => "https://www.tax.service.gov.uk/shortforms/form/EORINonVATImport?details=&vat=no"
  }

}
