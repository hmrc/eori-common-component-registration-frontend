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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings

import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.formatInput

import java.time.LocalDate
import scala.util.matching.Regex

trait Constraints {

  protected val noTagsRegex = "^[^<>]+$"
  protected val nameRegex = "[a-zA-Z0-9-' ]*"
  protected val validCharsRegex = """^[A-Za-z0-9 \-,.&']+$"""
  protected val postcodeRegex: Regex =
    "^(?i)(GIR 0AA)|((([A-Z][0-9][0-9]?)|(([A-Z][A-HJ-Y][0-9][0-9]?)|(([A-Z][0-9][A-Z])|([A-Z][A-HJ-Y][0-9]?[A-Z])))) ?[0-9][A-Z]{2})$".r

  protected def maxDate(maximum: => LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] = {
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }
  }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] = {
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _ =>
        Valid
    }
  }

  def validLine1: Constraint[String] = {
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching.organisation-address.line-1.error.empty"))
      case s if s.trim.length > 35 => Invalid(ValidationError("cds.matching.organisation-address.line-1.error.too-long"))
      case s if !s.matches(validCharsRegex) => Invalid(ValidationError("cds.matching.organisation-address.line-1.error.invalid-chars"))
      case _ => Valid
    })
  }

  def validLine2: Constraint[String] = {
    Constraint({
      case s if s.trim.length > 34 => Invalid(ValidationError("cds.matching.organisation-address.line-2.error.too-long"))
      case s if !s.matches(validCharsRegex) => Invalid(ValidationError("cds.matching.organisation-address.line-2.error.invalid-chars"))
      case _ => Valid
    })
  }

  def validLine3: Constraint[String] = {
    Constraint({
      case s if s.trim.isEmpty => Invalid(ValidationError("cds.matching.organisation-address.line-3.error.empty"))
      case s if s.trim.length > 34 => Invalid(ValidationError("cds.matching.organisation-address.line-3.error.too-long"))
      case s if !s.matches(validCharsRegex) => Invalid(ValidationError("cds.matching.organisation-address.line-3.error.invalid-chars"))
      case _ => Valid
    })
  }

  def validLine4: Constraint[String] = {
    Constraint({
      case s if s.trim.length > 35 => Invalid(ValidationError("cds.matching.organisation-address.line-4.error.too-long"))
      case s if !s.matches(validCharsRegex) => Invalid(ValidationError("cds.matching.organisation-address.line-4.error.invalid-chars"))
      case _ => Valid
    })
  }

  def validOrgName(emptyErrMsg: String, tooLongErrMsg: String, tagsErrMsg: String): Constraint[String] = {
    Constraint({
      case s if s.isEmpty => Invalid(ValidationError(emptyErrMsg))
      case s if s.length > 105 => Invalid(ValidationError(tooLongErrMsg))
      case s if !s.matches(noTagsRegex) => Invalid(ValidationError(tagsErrMsg))
      case _ => Valid
    })
  }

  def validName(nameKey: String): Constraint[String] = {
    Constraint(s"constraints.$nameKey")({
      case s if s.isEmpty => Invalid(ValidationError(s"cds.subscription.$nameKey.error.empty"))
      case s if !s.matches(nameRegex) => Invalid(ValidationError(s"cds.subscription.$nameKey.error.wrong-format"))
      case s if s.length > 35 => Invalid(ValidationError(s"cds.subscription.$nameKey.error.too-long"))
      case _ => Valid
    })
  }

  def validPostcode: Constraint[String] = {
    Constraint({
      case s if s.matches(postcodeRegex.regex) => Valid
      case _ => Invalid(ValidationError("cds.subscription.contact-details.error.postcode"))
    })
  }

  def validNino: Constraint[String] = {
    Constraint({
      case s if formatInput(s).isEmpty => Invalid(ValidationError("cds.subscription.nino.error.empty"))
      case s if formatInput(s).length != 9 => Invalid(ValidationError("cds.subscription.nino.error.wrong-length"))
      case s if !formatInput(s).matches("[a-zA-Z0-9]*") => Invalid(ValidationError("cds.matching.nino.invalid"))
      case s if !Nino.isValid(formatInput(s)) => Invalid(ValidationError("cds.matching.nino.invalid"))
      case _ => Valid
    })
  }

  def validUtr: Constraint[String] = {

    def validUtrFormat(utr: Option[String]): Boolean = {

      val ZERO = 0
      val ONE = 1
      val TWO = 2
      val THREE = 3
      val FOUR = 4
      val FIVE = 5
      val SIX = 6
      val SEVEN = 7
      val EIGHT = 8
      val NINE = 9
      val TEN = 10

      def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
        val mapOfRemainders = Map(
          ZERO  -> TWO,
          ONE   -> ONE,
          TWO   -> NINE,
          THREE -> EIGHT,
          FOUR  -> SEVEN,
          FIVE  -> SIX,
          SIX   -> FIVE,
          SEVEN -> FOUR,
          EIGHT -> THREE,
          NINE  -> TWO,
          TEN   -> ONE
        )
        mapOfRemainders.get(remainder).contains(checkDigit)
      }

      utr match {
        case Some(u) =>
          val utrWithoutK = u.trim.stripSuffix("K").stripSuffix("k")
          utrWithoutK.length == TEN && utrWithoutK.forall(_.isDigit) && {
            val actualUtr = utrWithoutK.toList
            val checkDigit = actualUtr.head.asDigit
            val restOfUtr = actualUtr.tail
            val weights = List(SIX, SEVEN, EIGHT, NINE, TEN, FIVE, FOUR, THREE, TWO)
            val weightedUtr = for ((w1, u1) <- weights zip restOfUtr) yield w1 * u1.asDigit
            val total = weightedUtr.sum
            val remainder = total % 11
            isValidUtr(remainder, checkDigit)
          }
        case None => false
      }
    }

    def validLength: String => Boolean = s => s.length == 10 || (s.endsWith("k") || s.endsWith("K") && s.length == 11)

    Constraint({
      case s if formatInput(s).isEmpty => Invalid(ValidationError("cds.matching-error.business-details.utr.isEmpty"))
      case s if !validLength(formatInput(s)) => Invalid(ValidationError("cds.matching-error.utr.length"))
      case s if !validUtrFormat(Some(formatInput(s))) => Invalid(ValidationError("cds.matching-error.utr.invalid"))
      case _ => Valid
    })
  }

}

object Constraints extends Constraints
