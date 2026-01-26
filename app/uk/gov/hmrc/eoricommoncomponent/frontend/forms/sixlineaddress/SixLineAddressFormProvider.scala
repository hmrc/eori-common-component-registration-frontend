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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.sixlineaddress

import play.api.data.Forms.{default, mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SixLineAddressMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.{lift, mandatoryString}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.postcodeMapping
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings
import uk.gov.voa.play.form.MandatoryOptionalMapping

import javax.inject.Singleton

@Singleton
class SixLineAddressFormProvider() extends Mappings {

  private val postcodeMaxLength = 9
  private val countryCodeGB = "GB"

  def ukSixLineAddressForm: Form[SixLineAddressMatchModel] = {
    Form(
      mapping(
        "line-1"      -> text.verifying(validLine1),
        "line-2"      -> optional(text.verifying(validLine2)),
        "line-3"      -> text.verifying(validLine3),
        "line-4"      -> optional(text.verifying(validLine4)),
        "postcode"    -> mandatoryOptPostCodeMapping,
        "countryCode" -> default(text, countryCodeGB)
      )(SixLineAddressMatchModel.apply)(SixLineAddressMatchModel.unapply)
    )
  }

  def channelIslandSixLineAddressForm: Form[SixLineAddressMatchModel] = {
    Form(
      mapping(
        "line-1"      -> text.verifying(validLine1),
        "line-2"      -> optional(text.verifying(validLine2)),
        "line-3"      -> text.verifying(validLine3),
        "line-4"      -> optional(text.verifying(validLine4)),
        "postcode"    -> mandatoryOptPostCodeMapping,
        "countryCode" -> mandatoryString("cds.matching-error.country.invalid")(s => s.length == 2)
      )(SixLineAddressMatchModel.apply)(SixLineAddressMatchModel.unapply)
    )
  }

  def thirdCountrySixLineAddressForm: Form[SixLineAddressMatchModel] = {
    Form(
      mapping(
        "line-1"      -> text.verifying(validLine1),
        "line-2"      -> optional(text.verifying(validLine2)),
        "line-3"      -> text.verifying(validLine3),
        "line-4"      -> optional(text.verifying(validLine4)),
        "postcode"    -> postcodeMapping,
        "countryCode" -> mandatoryString("cds.matching-error.country.invalid")(s => s.length == 2)
          .verifying(rejectGB)
      )(SixLineAddressMatchModel.apply)(SixLineAddressMatchModel.unapply)
    )
  }

  def mandatoryOptPostCodeMapping: Mapping[Option[String]] = {
    MandatoryOptionalMapping(text.verifying(validPostcode)).verifying(lift(postcodeMax))
  }

  private def postcodeMax: Constraint[String] = {
    Constraint({
      case s if s.length > postcodeMaxLength => Invalid(ValidationError("cds.subscription.postcode.error.too-long." + postcodeMaxLength))
      case _ => Valid
    })
  }

  private def rejectGB: Constraint[String] = {
    Constraint {
      case `countryCodeGB` => Invalid("cds.matching-error.country.unacceptable")
      case _ => Valid
    }
  }
}
