/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.SicCodeViewModel

import java.time.LocalDate

object SubscriptionForm {

  private val validConfirmIndividualTypes = Set(CdsOrganisationType.SoleTraderId, CdsOrganisationType.IndividualId)

  private val confirmIndividualTypeError = "cds.confirm-individual-type.error.individual-type"

  val confirmIndividualTypeForm: Form[CdsOrganisationType] = Form(
    "individual-type" -> mandatoryString(confirmIndividualTypeError)(oneOf(validConfirmIndividualTypes))
      .transform[CdsOrganisationType](CdsOrganisationType.forId, _.id)
  )

  val subscriptionDateOfEstablishmentForm: Form[LocalDate] = Form(
    "date-of-establishment" -> mandatoryDateTodayOrBefore(
      onEmptyError = "doe.error.empty-date",
      onInvalidDateError = "doe.error.invalid-date",
      onDateInFutureError = "doe.error.future-date",
      minYear = DateConverter.earliestYearDateOfEstablishment
    )
  )

  val sicCodeform = Form(
    Forms.mapping("sic" -> text.verifying(validSicCode))(SicCodeViewModel.apply)(SicCodeViewModel.unapply)
  )

  private def validSicCode: Constraint[String] =
    Constraint("constraints.sic")({
      case s if s.filterNot(_.isWhitespace).isEmpty       => Invalid(ValidationError("cds.subscription.sic.error.empty"))
      case s if !s.filterNot(_.isWhitespace).matches("[0-9]*") => Invalid(ValidationError("cds.subscription.sic.error.wrong-format"))
      case s if s.filterNot(_.isWhitespace).length < 4         => Invalid(ValidationError("cds.subscription.sic.error.too-short"))
      case s if s.filterNot(_.isWhitespace).length > 5         => Invalid(ValidationError("cds.subscription.sic.error.too-long"))
      case _                         => Valid
    })

  def validEoriWithOrWithoutGB: Constraint[String] =
    Constraint({
      case e if formatInput(e).isEmpty =>
        Invalid(ValidationError("ecc.matching-error.eori.isEmpty"))
      case e if formatInput(e).forall(_.isDigit) && formatInput(e).length < 12 =>
        Invalid(ValidationError("ecc.matching-error.eori.wrong-length.too-short"))
      case e if formatInput(e).startsWith("GB") && formatInput(e).length < 14 =>
        Invalid(ValidationError("ecc.matching-error.gbeori.wrong-length.too-short"))
      case e if formatInput(e).forall(_.isDigit) && formatInput(e).length > 15 =>
        Invalid(ValidationError("ecc.matching-error.eori.wrong-length.too-long"))
      case e if formatInput(e).startsWith("GB") && formatInput(e).length > 17 =>
        Invalid(ValidationError("ecc.matching-error.gbeori.wrong-length.too-long"))
      case e if formatInput(e).take(2).forall(_.isLetter) && !formatInput(e).startsWith("GB") =>
        Invalid(ValidationError("ecc.matching-error.eori.not-gb"))
      case e if !formatInput(e).matches("^GB[0-9]{12,15}$") && !formatInput(e).matches("[0-9]{12,15}") =>
        Invalid(ValidationError("ecc.matching-error.eori"))
      case _ => Valid
    })

  def validEmail: Constraint[String] =
    Constraint({
      case e if e.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.form-error.email"))
      case e if e.length > 50  => Invalid(ValidationError("cds.subscription.contact-details.form-error.email.too-long"))
      case e if !EmailAddress.isValid(e) =>
        Invalid(ValidationError("cds.subscription.contact-details.form-error.email.wrong-format"))
      case _ => Valid
    })

  def validFullName: Constraint[String] =
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.subscription.contact-details.form-error.full-name"))
      case s if s.length > 70  => Invalid(ValidationError("cds.subscription.full-name.error.too-long"))
      case _                   => Valid
    })

}
