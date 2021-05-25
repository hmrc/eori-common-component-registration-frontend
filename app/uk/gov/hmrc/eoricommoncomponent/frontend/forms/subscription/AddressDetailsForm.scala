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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription

import play.api.data.Form
import play.api.data.Forms.{text, _}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel

object AddressDetailsForm {

  def addressDetailsCreateForm()(implicit messages: Messages): Form[AddressViewModel] =
    Form(
      mapping(
        "street"   -> text.verifying(validLine1),
        "city"     -> text.verifying(validCity),
        "postcode" -> postcodeMapping,
        "countryCode" -> text.verifying(
          "cds.subscription.address-details.countryCode.error.label",
          s => s.trim.nonEmpty && s != messages("cds.subscription.address-details.country.emptyValueText")
        )
      )(AddressViewModel.apply)(AddressViewModel.unapply)
    )

  def validLine1: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty     => Invalid(ValidationError("cds.subscription.address-details.street.empty.error"))
      case s if s.trim.length > 70 => Invalid(ValidationError("cds.subscription.address-details.street.too-long.error"))
      case _                       => Valid
    })

}
