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

package unit.forms

import base.UnitSpec
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.{Form, FormError}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.{VatRegistrationDate, VatRegistrationDateFormProvider}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.TimeService

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Year}
import scala.collection.immutable.ArraySeq

class VatRegistrationDateFormSpec extends UnitSpec with MockitoSugar with BeforeAndAfter {

  val mockTimeService = mock[TimeService]

  lazy val form: Form[VatRegistrationDate] = new VatRegistrationDateFormProvider(mockTimeService)()

  val formDataVAT = Map(
    "vat-registration-date.day"   -> "1",
    "vat-registration-date.month" -> "1",
    "vat-registration-date.year"  -> "2019"
  )

  before {
    reset(mockTimeService)
    when(mockTimeService.getTodaysDate).thenReturn(LocalDate.of(2023, 1, 23))
  }

  "VatRegistrationDateForm" should {

    "call the time service every time we try to bind the form" in {

      when(mockTimeService.getTodaysDate).thenReturn(LocalDate.of(2023, 1, 23))

      val data = Map(
        "vat-registration-date.day"   -> "24",
        "vat-registration-date.month" -> "01",
        "vat-registration-date.year"  -> "2023"
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("vat-registration-date", "vat.error.minMax", ArraySeq("1970")))

      when(mockTimeService.getTodaysDate).thenReturn(LocalDate.of(2023, 1, 24))
      val res2 = form.bind(data)
      res2.errors shouldBe Nil

    }

    "only accept valid form" in {
      val data = formDataVAT
      val res  = form.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when effective date - year and month is missing" in {
      val data = formDataVAT.updated("vat-registration-date.year", "").updated("vat-registration-date.month", "")
      val res  = form.bind(data)
      res.errors shouldBe Seq(
        FormError(
          "vat-registration-date.month",
          List("vat-registration-date.month-vat-registration-date.year.empty"),
          List()
        ),
        FormError("vat-registration-date.year", List(""), List())
      )
    }
    "fail when effective date month invalid" in {
      val data = formDataVAT.updated("vat-registration-date.month", "13")
      val res  = form.bind(data)
      res.errors shouldBe Seq(FormError("vat-registration-date.month", Seq("date.month.error")))
    }

    "fail when effective date in future" in {
      val todayPlusOneDay = LocalDate.of(2023, 1, 24)
      val data = formDataVAT.updated(
        "vat-registration-date.day",
        DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)
      ).updated("vat-registration-date.month", DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)).updated(
        "vat-registration-date.year",
        DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay)
      )
      val res = form.bind(data)
      res.errors shouldBe Seq(FormError("vat-registration-date", Seq("vat.error.minMax"), ArraySeq("1970")))
    }
    "fail when effective date year invalid" in {
      val data = formDataVAT.updated("vat-registration-date.year", Year.now.plusYears(1).getValue.toString)
      val res  = form.bind(data)
      res.errors shouldBe Seq(FormError("vat-registration-date", Seq("vat.error.minMax"), ArraySeq("1970")))
    }
    "fail when effective date too early" in {
      val data = formDataVAT.updated("vat-registration-date.year", "1000")
      val res  = form.bind(data)
      res.errors shouldBe Seq(FormError("vat-registration-date", Seq("vat.error.minMax"), ArraySeq("1970")))
    }

  }

}
