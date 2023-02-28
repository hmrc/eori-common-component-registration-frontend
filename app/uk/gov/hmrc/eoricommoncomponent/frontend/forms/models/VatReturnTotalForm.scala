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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

case class VatReturnTotal(returnAmountInput: String)

object VatReturnTotal {
  implicit val format: Format[VatReturnTotal] = Json.format[VatReturnTotal]
}

object VatReturnTotalForm extends Mappings {

  def validReturnAmount: Constraint[String] =
    Constraint({
      case amount if amount.matches(amountRegex.regex) => Valid
      case _                                           => Invalid(ValidationError("Enter the amount of your latest tax return. Numbers only"))
    })

  val vatReturnTotalForm =
    Form(mapping("vat-return-total" -> text.verifying(validReturnAmount))(VatReturnTotal.apply)(VatReturnTotal.unapply))

}
