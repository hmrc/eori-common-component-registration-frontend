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

package unit.forms

import base.UnitSpec
import play.api.data.{Form, FormError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameDobMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.NameDobFormProvider

import java.time.{LocalDate, Year}
import java.time.format.DateTimeFormatter
import scala.collection.immutable.ArraySeq

class NameDobProviderSpec extends UnitSpec {

  val formData: Map[String, String] = Map(
    "first-name"          -> "ff",
    "middle-name"         -> "",
    "last-name"           -> "ddd",
    "date-of-birth.day"   -> "22",
    "date-of-birth.month" -> "10",
    "date-of-birth.year"  -> "2019"
  )

  val nameDobForm: Form[NameDobMatchModel] = new NameDobFormProvider().enterNameDobForm

  "NameDobForm" should {

    "only accept valid form" in {
      val data = formData
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept first-name with ' apostrophe" in {
      val data = formData.updated("first-name", "apos'trophe")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept last-name with ' apostrophe" in {
      val data = formData.updated("last-name", "apos'trophe")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept middle-name with ' apostrophe" in {
      val data = formData.updated("middle-name", "apos'trophe")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when a First Name is invalid" in {
      val data = formData.updated("first-name", "")
      val res = nameDobForm.bind(data)
      res.errors should not be empty
    }
    "fail when a Last Name is invalid" in {
      val data = formData.updated("last-name", "")
      val res = nameDobForm.bind(data)
      res.errors should not be empty
    }
    "fail when date of birth - day and month missing" in {
      val data = formData.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", List("date-of-birth.day-date-of-birth.month.empty"), List()),
        FormError("date-of-birth.month", List(""), List())
      )
    }
    "fail when date of birth - day and year missing" in {
      val data = formData.updated("date-of-birth.day", "").updated("date-of-birth.year", "")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", List("date-of-birth.day-date-of-birth.year.empty"), List()),
        FormError("date-of-birth.year", List(""), List())
      )
    }
    "fail when date of birth - year and month missing" in {
      val data = formData.updated("date-of-birth.year", "").updated("date-of-birth.month", "")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.month", List("date-of-birth.month-date-of-birth.year.empty"), List()),
        FormError("date-of-birth.year", List(""), List())
      )
    }
    "fail when date of birth - day missing" in {
      val data = formData.updated("date-of-birth.day", "")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.day", List("date-of-birth.day.empty"), List()))
    }
    "fail when date of birth - month missing" in {
      val data = formData.updated("date-of-birth.month", "")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.month", List("date-of-birth.month.empty"), List()))
    }
    "fail when date of birth - year missing" in {
      val data = formData.updated("date-of-birth.year", "")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", List("date-of-birth.year.empty"), List()))
    }
    "fail when date of birth - year 3 digits" in {
      val data = formData.updated("date-of-birth.year", "899")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", List("date-invalid-year-too-short"), List()))
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data =
        formData
          .updated("date-of-birth.day", DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay))
          .updated(
            "date-of-birth.month",
            DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)
          )
          .updated("date-of-birth.year", DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay))
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), ArraySeq("1900")))
    }
    "fail when a date of birth year invalid" in {
      val data = formData.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error"), List()))
    }
    "fail when a date of birth too early" in {
      val data = formData.updated("date-of-birth.year", "1800")
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), ArraySeq("1900")))
    }
  }
}
