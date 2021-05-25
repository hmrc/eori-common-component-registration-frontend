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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email

import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Forms}
import play.api.i18n.Messages
import uk.gov.hmrc.emailaddress.EmailAddress

object EmailForm {

  private def oneOf[T](validValues: Set[T]): T => Boolean = validValues.contains

  private val validYesNoAnswerOptions = Set("true", "false")

  def validEmail: Constraint[String] =
    Constraint({
      case e if e.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.form-error.email"))
      case e if e.length > 50  => Invalid(ValidationError("cds.subscription.contact-details.form-error.email.too-long"))
      case e if !EmailAddress.isValid(e) =>
        Invalid(ValidationError("cds.subscription.contact-details.form-error.email.wrong-format"))
      case _ => Valid
    })

  val emailForm = Form(
    Forms.mapping("email" -> text.verifying(validEmail))(EmailViewModel.apply)(EmailViewModel.unapply)
  )

  case class YesNo(isYes: Boolean) {
    def isNo: Boolean = !isYes
  }

  def confirmEmailYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] = yesNoAnswerForm()

  def eoriSignoutForm()(implicit messages: Messages): Form[YesNo] = yesNoAnswerForm()

  private def yesNoAnswerForm()(implicit messages: Messages): Form[YesNo] = Form(
    mapping(
      "yes-no-answer" -> optional(
        text.verifying(
          messages("cds.subscription.check-your-email.page-error.yes-no-answer"),
          oneOf(validYesNoAnswerOptions)
        )
      ).verifying(messages("cds.subscription.check-your-email.page-error.yes-no-answer"), _.isDefined)
        .transform[Boolean](str => str.get.toBoolean, bool => Option(String.valueOf(bool)))
    )(YesNo.apply)(YesNo.unapply)
  )

}
