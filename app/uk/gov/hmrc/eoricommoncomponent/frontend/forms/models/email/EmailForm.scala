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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email

import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.{Form, Forms}
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationKeys
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.emailaddress.EmailAddressValidation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.oneOf

object EmailForm {

  private val validYesNoAnswerOptions = Set("true", "false")
  private val emailAddressValidation = new EmailAddressValidation

  def validEmail: Constraint[String] =
    Constraint({
      case e if e.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.form-error.email"))
      case e if e.length > 50 => Invalid(ValidationError("cds.subscription.contact-details.form-error.email.too-long"))
      case e if !emailAddressValidation.isValid(e) =>
        Invalid(ValidationError("cds.subscription.contact-details.form-error.email.wrong-format"))
      case _ => Valid
    })

  val emailForm: Form[EmailViewModel] = Form(
    Forms.mapping(EmailVerificationKeys.EmailKey -> text.verifying(validEmail))(EmailViewModel.apply)(
      EmailViewModel.unapply
    )
  )

  def confirmEmailYesNoAnswerForm()(implicit messages: Messages): Form[YesNo] = Form(
    mapping(
      YesNo.yesAndNoAnswer -> optional(
        text.verifying(
          messages("cds.subscription.check-your-email.page-error.yes-no-answer"),
          oneOf(validYesNoAnswerOptions)
        )
      ).verifying(messages("cds.subscription.check-your-email.page-error.yes-no-answer"), _.isDefined)
        .transform[Boolean](str => str.get.toBoolean, bool => Option(String.valueOf(bool)))
    )(YesNo.apply)(YesNo.unapply)
  )

}
