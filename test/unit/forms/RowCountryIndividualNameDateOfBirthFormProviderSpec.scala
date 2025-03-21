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

package unit.forms

import base.UnitSpec
import play.api.data.{Form, FormError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.IndividualNameAndDateOfBirth
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.RowCountryIndividualNameDateOfBirthFormProvider

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Year}
import scala.collection.immutable.ArraySeq

class RowCountryIndividualNameDateOfBirthFormProviderSpec extends UnitSpec {

  val formDataRow: Map[String, String] = Map(
    "given-name"          -> "ff",
    "middle-name"         -> "",
    "family-name"         -> "ddd",
    "date-of-birth.day"   -> "22",
    "date-of-birth.month" -> "10",
    "date-of-birth.year"  -> "2019"
  )

  val thirdCountryIndividualNameDateOfBirthForm: Form[IndividualNameAndDateOfBirth] = new RowCountryIndividualNameDateOfBirthFormProvider().form

  "RowIndividualForm" should {

    "only accept valid form" in {
      val data = formDataRow
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept Given name with ' apostrophe" in {
      val data = formDataRow.updated("given-name", "apos'trophe")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept Family name with ' apostrophe" in {
      val data = formDataRow.updated("family-name", "apos'trophe")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept middle name with ' apostrophe" in {
      val data = formDataRow.updated("middle-name", "apos'trophe")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when a Given Name is invalid" in {
      val data = formDataRow.updated("given-name", "")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors should not be empty
    }
    "fail when a Family Name is invalid" in {
      val data = formDataRow.updated("family-name", "")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors should not be empty
    }
    "fail when date of birth - day and month is missing" in {
      val data = formDataRow.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", List("date-of-birth.day-date-of-birth.month.empty"), List()),
        FormError("date-of-birth.month", List(""), List())
      )
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data =
        formDataRow
          .updated("date-of-birth.day", DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay))
          .updated(
            "date-of-birth.month",
            DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)
          )
          .updated("date-of-birth.year", DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay))
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), ArraySeq("1900")))
    }
    "fail when a date of birth year invalid" in {
      val data = formDataRow.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", List("date.year.error"), List()))
    }
    "fail when a date of birth too early" in {
      val data = formDataRow.updated("date-of-birth.year", "1800")
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), ArraySeq("1900")))
    }
  }
}
