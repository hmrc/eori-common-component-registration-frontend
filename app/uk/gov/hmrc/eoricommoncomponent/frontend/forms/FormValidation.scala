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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.lift
import uk.gov.hmrc.eoricommoncomponent.frontend.playext.form.ConditionalMapping
import uk.gov.voa.play.form.ConditionalMappings.isAnyOf
import uk.gov.voa.play.form.MandatoryOptionalMapping

import scala.util.matching.Regex

object FormValidation {

  val postCodeMandatoryCountryCodes = Seq("GB", "GG", "JE", "IM")

  val postcodeRegex: Regex =
    "^(?i)(GIR 0AA)|((([A-Z][0-9][0-9]?)|(([A-Z][A-HJ-Y][0-9][0-9]?)|(([A-Z][0-9][A-Z])|([A-Z][A-HJ-Y][0-9]?[A-Z])))) ?[0-9][A-Z]{2})$".r

  def postcodeMapping: Mapping[Option[String]] =
    ConditionalMapping(
      condition = isAnyOf("countryCode", postCodeMandatoryCountryCodes),
      wrapped = MandatoryOptionalMapping(text.verifying(validPostcode)),
      elseValue = (key, data) => data.get(key)
    ).verifying(lift(postcodeMax(9)))

  private def validPostcode: Constraint[String] =
    Constraint({
      case s if s.matches(postcodeRegex.regex) => Valid
      case _                                   => Invalid(ValidationError("cds.subscription.contact-details.error.postcode"))
    })

  private def postcodeMax(limit: Int): Constraint[String] =
    Constraint({
      case s if s.length > limit => Invalid(ValidationError("cds.subscription.postcode.error.too-long." + limit))
      case _                     => Valid
    })

  def validCity: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.address-details.page-error.city"))
      case s if s.trim.length > 35 =>
        Invalid(ValidationError("cds.subscription.address-details.page-error.city.too-long"))
      case _ => Valid
    })

}
