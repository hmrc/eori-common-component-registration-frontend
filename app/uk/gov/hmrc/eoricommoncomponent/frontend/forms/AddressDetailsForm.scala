/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.data.Forms.{mapping, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm.{noTagsRegex, validCharsRegex}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.postcodeMapping
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel

import javax.inject.Singleton

@Singleton
class AddressDetailsForm() {

  def addressDetailsCreateForm(): Form[AddressViewModel] =
    Form(
      mapping(
        "street"      -> text.verifying(validStreet),
        "city"        -> text.verifying(validCity),
        "postcode"    -> postcodeMapping,
        "countryCode" -> text.verifying(validCountry)
      )(AddressViewModel.apply)(AddressViewModel.unapply)
    )

  private def validStreet: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.address-details.street.empty.error"))
      case s if s.trim.length > 70 => Invalid(ValidationError("cds.subscription.address-details.street.too-long.error"))
      case s if !s.matches(validCharsRegex) => Invalid(ValidationError("cds.subscription.address-details.street.error.invalid-chars"))
      case _ => Valid
    })

  private def validCountry: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching-error.country.invalid"))
      case s if s.trim.length != 2 => Invalid(ValidationError("cds.matching-error.country.invalid"))
      case s if !s.matches(noTagsRegex) => Invalid(ValidationError("cds.matching.organisation-address.line.error.invalid-chars"))
      case _ => Valid
    })

  def validCity: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.address-details.page-error.city"))
      case s if s.trim.length > 35 => Invalid(ValidationError("cds.subscription.address-details.page-error.city.too-long"))
      case s if !s.matches(validCharsRegex) => Invalid(ValidationError("cds.subscription.address-details.city.error.invalid-chars"))
      case _ => Valid
    })
}

object AddressDetailsForm {
  val noTagsRegex = "^[^<>]+$"
  val validCharsRegex = """^[A-Za-z0-9 \-,.&']+$"""
}
