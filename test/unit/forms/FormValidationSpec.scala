/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.Year

import base.UnitSpec
import org.joda.time.LocalDate
import play.api.data.{Form, FormError}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{IndividualNameAndDateOfBirth, NameDobMatchModel, NinoMatch}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm

import scala.util.Random

class FormValidationSpec extends UnitSpec {

  def randomNino: String = new Generator(new Random()).nextNino.nino

  lazy val nameDobForm: Form[NameDobMatchModel] = MatchingForms.enterNameDobForm
  lazy val ninoForm: Form[NinoMatch]            = MatchingForms.ninoForm

  lazy val thirdCountryIndividualNameDateOfBirthForm: Form[IndividualNameAndDateOfBirth] =
    MatchingForms.thirdCountryIndividualNameDateOfBirthForm

  lazy val vatDetailsForm = VatDetailsForm.vatDetailsForm

  lazy val dateOfEstablishmentForm: Form[LocalDate] = SubscriptionForm.subscriptionDateOfEstablishmentForm

  val formData = Map(
    "first-name"          -> "ff",
    "middle-name"         -> "",
    "last-name"           -> "ddd",
    "date-of-birth.day"   -> "22",
    "date-of-birth.month" -> "10",
    "date-of-birth.year"  -> "2019"
  )

  val formDataNino = Map(
    "first-name"          -> "ff",
    "last-name"           -> "ddd",
    "nino"                -> randomNino,
    "date-of-birth.day"   -> "22",
    "date-of-birth.month" -> "10",
    "date-of-birth.year"  -> "2019"
  )

  val formDataRow = Map(
    "given-name"          -> "ff",
    "middle-name"         -> "",
    "family-name"         -> "ddd",
    "date-of-birth.day"   -> "22",
    "date-of-birth.month" -> "10",
    "date-of-birth.year"  -> "2019"
  )

  val formDataVAT = Map(
    "postcode"                 -> "AB12CD",
    "vat-number"               -> "123456789",
    "vat-effective-date.day"   -> "1",
    "vat-effective-date.month" -> "1",
    "vat-effective-date.year"  -> "2019"
  )

  val formDataDoE = Map(
    "date-of-establishment.day"   -> "1",
    "date-of-establishment.month" -> "1",
    "date-of-establishment.year"  -> "2019"
  )

  "NameDobForm" should {

    "only accept valid form" in {
      val data = formData
      val res  = nameDobForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept first-name with ' apostrophe" in {
      val data = formData.updated("first-name", "apos'trophe")
      val res  = nameDobForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept last-name with ' apostrophe" in {
      val data = formData.updated("last-name", "apos'trophe")
      val res  = nameDobForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept middle-name with ' apostrophe" in {
      val data = formData.updated("middle-name", "apos'trophe")
      val res  = nameDobForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "fail when a First Name is invalid" in {
      val data = formData.updated("first-name", "")
      val res  = nameDobForm.bind(data)
      assert(res.errors.nonEmpty)
    }
    "fail when a Last Name is invalid" in {
      val data = formData.updated("last-name", "")
      val res  = nameDobForm.bind(data)
      assert(res.errors.nonEmpty)
    }
    "fail when a date of birth is missing" in {
      val data = formData.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.empty-date")))
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formData.updated("date-of-birth.day", todayPlusOneDay.toString("dd")).updated(
        "date-of-birth.month",
        todayPlusOneDay.toString("MM")
      ).updated("date-of-birth.year", todayPlusOneDay.toString("YYYY"))
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.future-date")))
    }
    "fail when a date of birth year invalid" in {
      val data = formData.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error")))
    }
    "fail when a date of birth too early" in {
      val data = formData.updated("date-of-birth.year", "1800")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error")))
    }
  }

  "NinoForm" should {

    "only accept valid form" in {
      val data = formDataNino
      val res  = ninoForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept first-name with ' apostrophe" in {
      val data = formDataNino.updated("first-name", "apos'trophe")
      val res  = ninoForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept last-name with ' apostrophe" in {
      val data = formDataNino.updated("last-name", "apos'trophe")
      val res  = ninoForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "fail when a First Name is invalid" in {
      val data = formDataNino.updated("first-name", "")
      val res  = ninoForm.bind(data)
      assert(res.errors.nonEmpty)
    }
    "fail when a Last Name is invalid" in {
      val data = formDataNino.updated("last-name", "")
      val res  = ninoForm.bind(data)
      assert(res.errors.nonEmpty)
    }
    "fail when a date of birth is missing" in {
      val data = formDataNino.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.empty-date")))
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataNino.updated("date-of-birth.day", todayPlusOneDay.toString("dd")).updated(
        "date-of-birth.month",
        todayPlusOneDay.toString("MM")
      ).updated("date-of-birth.year", todayPlusOneDay.toString("YYYY"))
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.future-date")))
    }
    "fail when a date of birth year invalid" in {
      val data = formDataNino.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error")))
    }
    "fail when a date of birth too early" in {
      val data = formDataNino.updated("date-of-birth.year", "1800")
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error")))
    }
  }

  "RowIndividualForm" should {

    "only accept valid form" in {
      val data = formDataRow
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept Given name with ' apostrophe" in {
      val data = formDataRow.updated("given-name", "apos'trophe")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept Family name with ' apostrophe" in {
      val data = formDataRow.updated("family-name", "apos'trophe")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "accept middle name with ' apostrophe" in {
      val data = formDataRow.updated("middle-name", "apos'trophe")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "fail when a Given Name is invalid" in {
      val data = formDataRow.updated("given-name", "")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      assert(res.errors.nonEmpty)
    }
    "fail when a Family Name is invalid" in {
      val data = formDataRow.updated("family-name", "")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      assert(res.errors.nonEmpty)
    }
    "fail when a date of birth is missing" in {
      val data = formDataRow.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.empty-date")))
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataRow.updated("date-of-birth.day", todayPlusOneDay.toString("dd")).updated(
        "date-of-birth.month",
        todayPlusOneDay.toString("MM")
      ).updated("date-of-birth.year", todayPlusOneDay.toString("YYYY"))
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.future-date")))
    }
    "fail when a date of birth year invalid" in {
      val data = formDataRow.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error")))
    }
    "fail when a date of birth too early" in {
      val data = formDataRow.updated("date-of-birth.year", "1800")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth.year", Seq("date.year.error")))
    }
  }

  "VAT details form" should {
    "only accept valid form" in {
      val data = formDataVAT
      val res  = vatDetailsForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "fail when effective date is missing" in {
      val data = formDataVAT.updated("vat-effective-date.day", "").updated("vat-effective-date.month", "")
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date", Seq("vat.error.empty-date")))
    }
    "fail when effective date in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataVAT.updated("vat-effective-date.day", todayPlusOneDay.toString("dd")).updated(
        "vat-effective-date.month",
        todayPlusOneDay.toString("MM")
      ).updated("vat-effective-date.year", todayPlusOneDay.toString("YYYY"))
      val res = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date", Seq("vat.error.future-date")))
    }
    "fail when effective date year invalid" in {
      val data = formDataVAT.updated("vat-effective-date.year", Year.now.plusYears(1).getValue.toString)
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date.year", Seq("date.year.error")))
    }
    "fail when effective date too early" in {
      val data = formDataVAT.updated("vat-effective-date.year", "1000")
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date.year", Seq("date.year.error")))
    }
  }

  "Date of establishment form" should {
    "only accept valid form" in {
      val data = formDataDoE
      val res  = dateOfEstablishmentForm.bind(data)
      assert(res.errors.isEmpty)
    }
    "fail when date of establishment is missing" in {
      val data = formDataDoE.updated("date-of-establishment.day", "").updated("date-of-establishment.month", "")
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", Seq("doe.error.empty-date")))
    }
    "fail when date of establishment in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataDoE.updated("date-of-establishment.day", todayPlusOneDay.toString("dd")).updated(
        "date-of-establishment.month",
        todayPlusOneDay.toString("MM")
      ).updated("date-of-establishment.year", todayPlusOneDay.toString("YYYY"))
      val res = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", Seq("doe.error.future-date")))
    }
    "fail when date of establishment year invalid" in {
      val data = formDataDoE.updated("date-of-establishment.year", Year.now.plusYears(1).getValue.toString)
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment.year", Seq("date.year.error")))
    }
    "fail when date of establishment too early" in {
      val data = formDataDoE.updated("date-of-establishment.year", "999")
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment.year", Seq("date.year.error")))
    }
  }
}
