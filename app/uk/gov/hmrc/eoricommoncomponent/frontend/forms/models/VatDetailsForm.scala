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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData

case class VatDetails(postcode: String, number: String) {

  def isGiant: Boolean = number.startsWith("654") || number.startsWith("8888")
}

object VatDetails {
  implicit val format: Format[VatDetails] = Json.format[VatDetails]
}

object VatDetailsForm extends Mappings {

  def validPostcode(requestSessionData: RequestSessionData)(implicit request: Request[AnyContent]): Constraint[String] =
    Constraint("constraints.postcode") { pcode =>
      pcode.filterNot(_.isWhitespace) match {
        case s if s.matches(postcodeRegex.regex) => Valid
        case _ if requestSessionData.isRestOfTheWorld => Valid
        case _ => Invalid(ValidationError("cds.subscription.vat-details.postcode.required.error"))
      }

    }

  private def validVatNumber: Constraint[String] =
    Constraint("constraints.vat-number") { vat =>
      vat.filterNot(_.isWhitespace) match {
        case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.vat-uk.required.error"))
        case s if !s.matches("^([0-9]{9})$") => Invalid(ValidationError("cds.subscription.vat-uk.length.error"))
        case _ => Valid
      }

    }

  class VatDetailsForm(requestSessionData: RequestSessionData) {

    def vatDetailsForm(implicit request: Request[AnyContent]): Form[VatDetails] =
      Form(
        mapping(
          "postcode"   -> text.verifying(validPostcode(requestSessionData)),
          "vat-number" -> text.verifying(validVatNumber)
        )((postCode, vat) => VatDetails.apply(postCode, vat.filterNot(_.isWhitespace)))(VatDetails.unapply)
      )

  }

}
