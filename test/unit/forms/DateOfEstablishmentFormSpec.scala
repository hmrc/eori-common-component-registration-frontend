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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.DateOfEstablishmentForm

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Year}
import scala.collection.immutable.ArraySeq

class DateOfEstablishmentFormSpec extends UnitSpec {

  val form: Form[LocalDate] = new DateOfEstablishmentForm().form()

  val formDataDoE: Map[String, String] = Map(
    "date-of-establishment.day"   -> "1",
    "date-of-establishment.month" -> "1",
    "date-of-establishment.year"  -> "2019"
  )

  "Date of establishment form" should {

    "only accept valid form" in {
      val data = formDataDoE
      val res = form.bind(data)
      res.errors shouldBe Seq.empty
    }

    "fail when date of establishment is missing" in {
      val data = formDataDoE
        .updated("date-of-establishment.day", "")
        .updated(
          "date-of-establishment.month",
          ""
        )
        .updated("date-of-establishment.year", "")
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", List("doe.error.empty-date"), List()))

    }

    "fail when date of establishment in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataDoE
        .updated(
          "date-of-establishment.day",
          DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)
        )
        .updated("date-of-establishment.month", DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay))
        .updated(
          "date-of-establishment.year",
          DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay)
        )
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", Seq("doe.error.minMax"), ArraySeq("1000")))
    }

    "fail when date of establishment year invalid" in {
      val data = formDataDoE.updated("date-of-establishment.year", Year.now.plusYears(1).getValue.toString)
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment.year", List("date.year.error"), List()))
    }

    "fail when date of establishment too late" in {
      val data = formDataDoE.updated("date-of-establishment.year", "9999")
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment.year", List("date.year.error"), List()))
    }

    "fail with a month error, when month is populated with blanks" in {
      val data = Map(
        "date-of-establishment.day"   -> "1",
        "date-of-establishment.month" -> " ",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-establishment.month", List("date-of-establishment.month.empty"), List())
      )
    }

    "fail when the date is invalid" in {
      val data = Map(
        "date-of-establishment.day"   -> "31",
        "date-of-establishment.month" -> "2",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", List("doe.error.invalid-date"), List()))
    }

    "pass when the date has a day of 31" in {
      val data = Map(
        "date-of-establishment.day"   -> "31",
        "date-of-establishment.month" -> "1",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Nil
    }

    "fail when the date contains a day greater than 31" in {
      val data = Map(
        "date-of-establishment.day"   -> "32",
        "date-of-establishment.month" -> "1",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment.day", Seq("date.day.error"), ArraySeq()))
    }

    "pass when the date has a month of 12" in {
      val data = Map(
        "date-of-establishment.day"   -> "31",
        "date-of-establishment.month" -> "12",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Nil
    }

    "fail when the date contains a month greater than 12" in {
      val data = Map(
        "date-of-establishment.day"   -> "31",
        "date-of-establishment.month" -> "13",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment.month", Seq("date.month.error"), ArraySeq()))
    }

    "fail when the date contains a day greater than 31 and a month greater than 12" in {
      val data = Map(
        "date-of-establishment.day"   -> "32",
        "date-of-establishment.month" -> "13",
        "date-of-establishment.year"  -> "2019"
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-establishment.day", Seq("date.day.error"), ArraySeq()),
        FormError("date-of-establishment.month", Seq("date.month.error"), ArraySeq())
      )
    }
  }
}
