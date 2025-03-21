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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.embassy

import play.api.data.Form
import play.api.data.Forms.{default, mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EmbassyAddressMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.countryCodeGB
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Constraints

import javax.inject.Singleton

@Singleton
class EmbassyAddressForm() extends Constraints {

  def form: Form[EmbassyAddressMatchModel] =
    Form {
      mapping(
        "line-1"      -> text.verifying(validLineOne),
        "line-2"      -> optional(text.verifying(validLineTwo)),
        "townCity"    -> text.verifying(validLineTownCity),
        "postcode"    -> text.verifying(validPostcode),
        "countryCode" -> default(text, countryCodeGB)
      )(EmbassyAddressMatchModel.apply)(EmbassyAddressMatchModel.unapply)
    }

  private def validLineOne: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching.embassy-address.line-1.error.empty"))
      case s if !s.matches(noTagsRegex) => Invalid(ValidationError("cds.matching.embassy-address.line.error.invalid-chars"))
      case _ => Valid
    })

  private def validLineTwo: Constraint[String] =
    Constraint({
      case s if !s.matches(noTagsRegex) => Invalid(ValidationError("cds.matching.embassy-address.line.error.invalid-chars"))
      case _ => Valid
    })

  private def validLineTownCity: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching.embassy-address.town-city.error.empty"))
      case s if s.trim.length > 34 => Invalid(ValidationError("cds.matching.embassy-address.town-city.error.too-long"))
      case s if !s.matches(noTagsRegex) => Invalid(ValidationError("cds.matching.embassy-address.line.error.invalid-chars"))
      case _ => Valid
    })

}
