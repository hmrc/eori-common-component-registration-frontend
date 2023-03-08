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

import java.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

case class VatDetailsOld(postcode: String, number: String, effectiveDate: LocalDate)

object VatDetailsOld {
  implicit val format: Format[VatDetailsOld] = Json.format[VatDetailsOld]
}

object VatDetailsFormOld extends Mappings {

  def validPostcode: Constraint[String] =
    Constraint({
      case s if s.matches(postcodeRegex.regex) => Valid
      case _                                   => Invalid(ValidationError("cds.subscription.vat-details.postcode.required.error"))
    })

  def validVatNumber: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty             => Invalid(ValidationError("cds.subscription.vat-uk.required.error"))
      case s if !s.matches("^([0-9]{9})$") => Invalid(ValidationError("cds.subscription.vat-uk.length.error"))
      case _                               => Valid
    })

  val vatDetailsFormOld = {

    val minimumDate = LocalDate.of(DateConverter.earliestYearEffectiveVatDate, 1, 1)
    val today       = LocalDate.now()

    Form(
      mapping(
        "postcode"   -> text.verifying(validPostcode),
        "vat-number" -> text.verifying(validVatNumber),
        "vat-effective-date" -> localDate(
          emptyKey = "vat.error.empty-date",
          invalidKey = "vat.error.invalid-date"
        ).verifying(minDate(minimumDate, "vat.error.minMax", DateConverter.earliestYearEffectiveVatDate.toString))
          .verifying(maxDate(today, "vat.error.minMax", DateConverter.earliestYearEffectiveVatDate.toString))
      )(VatDetailsOld.apply)(VatDetailsOld.unapply)
    )
  }

}
