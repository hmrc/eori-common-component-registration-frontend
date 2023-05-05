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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationKeys
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm._

object ContactDetailsForm {

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

}
