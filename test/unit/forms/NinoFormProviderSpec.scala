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
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NinoMatch
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.NinoFormProvider

import java.time.{LocalDate, Year}
import java.time.format.DateTimeFormatter
import scala.collection.immutable.ArraySeq
import scala.util.Random

class NinoFormProviderSpec extends UnitSpec {

  private def randomNino: String = new Generator(new Random()).nextNino.nino

  private val formDataNino = Map(
    "first-name"          -> "ff",
    "last-name"           -> "ddd",
    "nino"                -> randomNino,
    "date-of-birth.day"   -> "22",
    "date-of-birth.month" -> "10",
    "date-of-birth.year"  -> "2019"
  )

  val ninoForm: Form[NinoMatch] = new NinoFormProvider().ninoForm

  "NinoForm" should {

    "only accept valid form" in {
      val data = formDataNino
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept first-name with ' apostrophe" in {
      val data = formDataNino.updated("first-name", "apos'trophe")
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept last-name with ' apostrophe" in {
      val data = formDataNino.updated("last-name", "apos'trophe")
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when a First Name is invalid" in {
      val data = formDataNino.updated("first-name", "")
      val res = ninoForm.bind(data)
      res.errors should not be empty
    }
    "fail when a Last Name is invalid" in {
      val data = formDataNino.updated("last-name", "")
      val res = ninoForm.bind(data)
      res.errors should not be empty
    }
    "fail when date of birth - day and year is missing" in {
      val data = formDataNino.updated("date-of-birth.day", "").updated("date-of-birth.year", "")
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", List("date-of-birth.day-date-of-birth.year.empty"), List()),
        FormError("date-of-birth.year", List(""), List())
      )
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data =
        formDataNino
          .updated("date-of-birth.day", DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay))
          .updated(
            "date-of-birth.month",
            DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)
          )
          .updated("date-of-birth.year", DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay))
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), ArraySeq("1900")))
    }
    "fail when a date of birth year invalid" in {
      val data = formDataNino.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error"), List()))
    }
    "fail when a date of birth too early" in {
      val data = formDataNino.updated("date-of-birth.year", "1800")
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), ArraySeq("1900")))
    }
  }

}
