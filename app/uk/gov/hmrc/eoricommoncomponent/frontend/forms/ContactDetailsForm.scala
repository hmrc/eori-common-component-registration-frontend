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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationKeys
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsViewModel

import javax.inject.Singleton

@Singleton
class ContactDetailsForm() {
  private val nameRegex = "[a-zA-Z0-9-' ]*"

  def contactDetailsCreateForm(): Form[ContactDetailsViewModel] =
    Form(
      mapping(
        "full-name"                    -> text.verifying(validFullName),
        EmailVerificationKeys.EmailKey -> optional(text),
        "telephone"                    -> text.verifying(validPhone)
      )(ContactDetailsViewModel.apply)(ContactDetailsViewModel.unapply)
    )

  private def validPhone: Constraint[String] =
    Constraint({
      case e if e.trim.isEmpty => Invalid(ValidationError("cds.contact-details.page-error.telephone.isEmpty"))
      case e if e.length > 24 =>
        Invalid(ValidationError("cds.contact-details.page-error.telephone.wrong-length.too-long"))
      case e if !e.matches("""[A-Z0-9 +)/(\\\-\*#]{0,24}""") =>
        Invalid(ValidationError("cds.contact-details.page-error.telephone.wrong-format"))
      case _ => Valid
    })

  def validFullName: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.form-error.full-name"))
      case s if !s.matches(nameRegex) =>
        Invalid(ValidationError("cds.subscription.contact-details.form-error.full-name.wrong-format"))
      case s if s.length > 70 => Invalid(ValidationError("cds.subscription.full-name.error.too-long"))
      case _ => Valid
    })

}
