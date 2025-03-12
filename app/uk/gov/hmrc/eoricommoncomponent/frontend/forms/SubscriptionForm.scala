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

import play.api.data.Forms.text
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Forms}
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.SicCodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

import java.time.LocalDate

object SubscriptionForm extends Mappings {
  private val nameRegex = "[a-zA-Z0-9-' ]*"

  private val validConfirmIndividualTypes = Set(CdsOrganisationType.SoleTraderId, CdsOrganisationType.IndividualId)

  private val confirmIndividualTypeError = "cds.confirm-individual-type.error.individual-type"

  val confirmIndividualTypeForm: Form[CdsOrganisationType] = Form(
    "individual-type" -> mandatoryString(confirmIndividualTypeError)(oneOf(validConfirmIndividualTypes))
      .transform[CdsOrganisationType](CdsOrganisationType.forId, _.id)
  )

  val subscriptionDateOfEstablishmentForm: Form[LocalDate] = {

    val minimumDate = LocalDate.of(DateConverter.earliestYearDateOfEstablishment, 1, 1)
    val today = LocalDate.now()

    Form(
      "date-of-establishment" -> localDate(
        emptyKey = "doe.error.empty-date",
        invalidKey = "doe.error.invalid-date"
      ).verifying(minDate(minimumDate, "doe.error.minMax", DateConverter.earliestYearDateOfEstablishment.toString))
        .verifying(maxDate(today, "doe.error.minMax", DateConverter.earliestYearDateOfEstablishment.toString))
    )
  }

  val sicCodeform = Form(
    Forms.mapping("sic" -> text.verifying(validSicCode))(SicCodeViewModel.apply)(SicCodeViewModel.unapply)
  )

  private def validSicCode: Constraint[String] =
    Constraint("constraints.sic") { s =>
      val sicCodeWhitespaceRemoved = s.toString.filterNot(_.isWhitespace)

      sicCodeWhitespaceRemoved match {
        case s if s.isEmpty =>
          Invalid(ValidationError("cds.subscription.sic.error.empty"))
        case s if !s.matches("[0-9]*") =>
          Invalid(ValidationError("cds.subscription.sic.error.wrong-format"))
        case s if s.length < 4 =>
          Invalid(ValidationError("cds.subscription.sic.error.too-short"))
        case s if s.length > 5 =>
          Invalid(ValidationError("cds.subscription.sic.error.too-long"))
        case _ => Valid
      }
    }

  def validFullName: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.form-error.full-name"))
      case s if !s.matches(nameRegex) =>
        Invalid(ValidationError("cds.subscription.contact-details.form-error.full-name.wrong-format"))
      case s if s.length > 70 => Invalid(ValidationError("cds.subscription.full-name.error.too-long"))
      case _ => Valid
    })

}
