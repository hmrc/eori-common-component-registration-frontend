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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.registration

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm._
import uk.gov.hmrc.eoricommoncomponent.frontend.playext.form.ConditionalMapping
import uk.gov.voa.play.form.ConditionalMappings.isEqual
import uk.gov.voa.play.form.MandatoryOptionalMapping

object ContactDetailsForm {
  private val Length2    = 2
  private val noAnswered = "false"

  def contactDetailsCreateForm(): Form[ContactDetailsViewModel] =
    Form(
      mapping(
        "full-name" -> text.verifying(validFullName),
        "email"     -> optional(text),
        "telephone" -> text.verifying(validPhone),
        "fax"       -> optional(text.verifying(validFax)),
        "use-registered-address" -> validMultipleChoiceWithCustomError(
          "cds.subscription.contact-details.error.use-registered-address"
        ),
        "street" -> ConditionalMapping(
          condition = isEqual("use-registered-address", noAnswered),
          wrapped = MandatoryOptionalMapping(text.verifying(validStreet)),
          elseValue = (key, data) => data.get(key)
        ),
        "city" -> ConditionalMapping(
          condition = isEqual("use-registered-address", noAnswered),
          wrapped = MandatoryOptionalMapping(text.verifying(validCity)),
          elseValue = (key, data) => data.get(key)
        ),
        "postcode" -> ConditionalMapping(
          condition = isEqual("use-registered-address", noAnswered),
          wrapped = postcodeMapping,
          elseValue = (key, data) => data.get(key)
        ),
        "countryCode" -> ConditionalMapping(
          condition = isEqual("use-registered-address", noAnswered),
          wrapped = MandatoryOptionalMapping(
            mandatoryString("cds.subscription.contact-details.form-error.country")(s => s.length == Length2)
          ),
          elseValue = (key, data) => data.get(key)
        )
      )(ContactDetailsViewModel.apply)(ContactDetailsViewModel.unapply)
    )

  private def validStreet: Constraint[String] =
    Constraint({
      case e if e.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.error.street"))
      case e if e.length > 70  => Invalid(ValidationError("cds.subscription.contact-details.error.street.too-long"))
      case _                   => Valid
    })

  private def validPhone: Constraint[String] =
    Constraint({
      case e if e.trim.isEmpty => Invalid(ValidationError("cds.contact-details.page-error.telephone.isEmpty"))
      case e if e.length > 24 =>
        Invalid(ValidationError("cds.contact-details.page-error.telephone.wrong-length.too-long"))
      case e if !e.matches("""[A-Z0-9 +)/(\\\-\*#]{0,24}""") =>
        Invalid(ValidationError("cds.contact-details.page-error.telephone.wrong-format"))
      case _ => Valid
    })

  private def validFax: Constraint[String] =
    Constraint({
      case e if e.length > 24 => Invalid(ValidationError("cds.contact-details.page-error.fax.wrong-length.too-long"))
      case e if !e.matches("[A-Z0-9 +)/( -*#]{0,24}") =>
        Invalid(ValidationError("cds.contact-details.page-error.fax.wrong-format"))
      case _ => Valid
    })

}
