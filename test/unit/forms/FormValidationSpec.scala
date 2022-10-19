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

package unit.forms

import java.time.Year
import base.UnitSpec

import java.time.LocalDate
import play.api.data.{Form, FormError}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{IndividualNameAndDateOfBirth, NameDobMatchModel, NinoMatch}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{SicCodeViewModel, VatDetailsForm}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.{MatchingForms, SubscriptionForm}

import java.time.format.DateTimeFormatter
import scala.collection.mutable
import scala.util.Random

class FormValidationSpec extends UnitSpec {

  def randomNino: String = new Generator(new Random()).nextNino.nino

  lazy val nameDobForm: Form[NameDobMatchModel] = MatchingForms.enterNameDobForm
  lazy val ninoForm: Form[NinoMatch]            = MatchingForms.ninoForm

  lazy val thirdCountryIndividualNameDateOfBirthForm: Form[IndividualNameAndDateOfBirth] =
    MatchingForms.thirdCountryIndividualNameDateOfBirthForm

  lazy val vatDetailsForm = VatDetailsForm.vatDetailsForm

  lazy val dateOfEstablishmentForm: Form[LocalDate] = SubscriptionForm.subscriptionDateOfEstablishmentForm

  lazy val sicCodeForm: Form[SicCodeViewModel] = SubscriptionForm.sicCodeform

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

  val formDataSic = Map("sic" -> "99111")

  "NameDobForm" should {

    "only accept valid form" in {
      val data = formData
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept first-name with ' apostrophe" in {
      val data = formData.updated("first-name", "apos'trophe")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept last-name with ' apostrophe" in {
      val data = formData.updated("last-name", "apos'trophe")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept middle-name with ' apostrophe" in {
      val data = formData.updated("middle-name", "apos'trophe")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when a First Name is invalid" in {
      val data = formData.updated("first-name", "")
      val res  = nameDobForm.bind(data)
      res.errors should not be empty
    }
    "fail when a Last Name is invalid" in {
      val data = formData.updated("last-name", "")
      val res  = nameDobForm.bind(data)
      res.errors should not be empty
    }
    "fail when a date of birth is missing" in {
      val data = formData.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", Seq("dob.error.empty-date")),
        FormError("date-of-birth.month", "")
      )
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data =
        formData.updated("date-of-birth.day", DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)).updated(
          "date-of-birth.month",
          DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)
        ).updated("date-of-birth.year", DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay))
      val res = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
    "fail when a date of birth year invalid" in {
      val data = formData.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
    "fail when a date of birth too early" in {
      val data = formData.updated("date-of-birth.year", "1800")
      val res  = nameDobForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
  }

  "NinoForm" should {

    "only accept valid form" in {
      val data = formDataNino
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept first-name with ' apostrophe" in {
      val data = formDataNino.updated("first-name", "apos'trophe")
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept last-name with ' apostrophe" in {
      val data = formDataNino.updated("last-name", "apos'trophe")
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when a First Name is invalid" in {
      val data = formDataNino.updated("first-name", "")
      val res  = ninoForm.bind(data)
      res.errors should not be empty
    }
    "fail when a Last Name is invalid" in {
      val data = formDataNino.updated("last-name", "")
      val res  = ninoForm.bind(data)
      res.errors should not be empty
    }
    "fail when a date of birth is missing" in {
      val data = formDataNino.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", Seq("dob.error.empty-date")),
        FormError("date-of-birth.month", "")
      )
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data =
        formDataNino.updated("date-of-birth.day", DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)).updated(
          "date-of-birth.month",
          DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)
        ).updated("date-of-birth.year", DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay))
      val res = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
    "fail when a date of birth year invalid" in {
      val data = formDataNino.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
    "fail when a date of birth too early" in {
      val data = formDataNino.updated("date-of-birth.year", "1800")
      val res  = ninoForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
  }

  "RowIndividualForm" should {

    "only accept valid form" in {
      val data = formDataRow
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept Given name with ' apostrophe" in {
      val data = formDataRow.updated("given-name", "apos'trophe")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept Family name with ' apostrophe" in {
      val data = formDataRow.updated("family-name", "apos'trophe")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept middle name with ' apostrophe" in {
      val data = formDataRow.updated("middle-name", "apos'trophe")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when a Given Name is invalid" in {
      val data = formDataRow.updated("given-name", "")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors should not be empty
    }
    "fail when a Family Name is invalid" in {
      val data = formDataRow.updated("family-name", "")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors should not be empty
    }
    "fail when a date of birth is missing" in {
      val data = formDataRow.updated("date-of-birth.day", "").updated("date-of-birth.month", "")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-birth.day", Seq("dob.error.empty-date")),
        FormError("date-of-birth.month", "")
      )
    }
    "fail when a date of birth in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data =
        formDataRow.updated("date-of-birth.day", DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)).updated(
          "date-of-birth.month",
          DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)
        ).updated("date-of-birth.year", DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay))
      val res = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
    "fail when a date of birth year invalid" in {
      val data = formDataRow.updated("date-of-birth.year", Year.now.plusYears(1).getValue.toString)
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
    "fail when a date of birth too early" in {
      val data = formDataRow.updated("date-of-birth.year", "1800")
      val res  = thirdCountryIndividualNameDateOfBirthForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-birth", Seq("dob.error.minMax"), Array("1900")))
    }
  }

  "VAT details form" should {
    "only accept valid form" in {
      val data = formDataVAT
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when effective date is missing" in {
      val data = formDataVAT.updated("vat-effective-date.day", "").updated("vat-effective-date.month", "")
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(
        FormError("vat-effective-date.day", Seq("vat.error.empty-date")),
        FormError("vat-effective-date.month", "")
      )
    }
    "fail when effective date in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataVAT.updated(
        "vat-effective-date.day",
        DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)
      ).updated("vat-effective-date.month", DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)).updated(
        "vat-effective-date.year",
        DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay)
      )
      val res = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date", Seq("vat.error.minMax"), Array("1970")))
    }
    "fail when effective date year invalid" in {
      val data = formDataVAT.updated("vat-effective-date.year", Year.now.plusYears(1).getValue.toString)
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date", Seq("vat.error.minMax"), Array("1970")))
    }
    "fail when effective date too early" in {
      val data = formDataVAT.updated("vat-effective-date.year", "1000")
      val res  = vatDetailsForm.bind(data)
      res.errors shouldBe Seq(FormError("vat-effective-date", Seq("vat.error.minMax"), Array("1970")))
    }
  }

  "Date of establishment form" should {
    "only accept valid form" in {
      val data = formDataDoE
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail when date of establishment is missing" in {
      val data = formDataDoE.updated("date-of-establishment.day", "").updated("date-of-establishment.month", "")
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(
        FormError("date-of-establishment.day", Seq("doe.error.empty-date")),
        FormError("date-of-establishment.month", "")
      )

    }
    "fail when date of establishment in future" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val data = formDataDoE.updated(
        "date-of-establishment.day",
        DateTimeFormatter.ofPattern("dd").format(todayPlusOneDay)
      ).updated("date-of-establishment.month", DateTimeFormatter.ofPattern("MM").format(todayPlusOneDay)).updated(
        "date-of-establishment.year",
        DateTimeFormatter.ofPattern("YYYY").format(todayPlusOneDay)
      )
      val res = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", Seq("doe.error.minMax"), Array("1000")))
    }
    "fail when date of establishment year invalid" in {
      val data = formDataDoE.updated("date-of-establishment.year", Year.now.plusYears(1).getValue.toString)
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", Seq("doe.error.minMax"), Array("1000")))
    }
    "fail when date of establishment too early" in {
      val data = formDataDoE.updated("date-of-establishment.year", "999")
      val res  = dateOfEstablishmentForm.bind(data)
      res.errors shouldBe Seq(FormError("date-of-establishment", Seq("doe.error.minMax"), Array("1000")))
    }
  }

  "sicCodeForm" should {

    "only accept valid form" in {
      val data = formDataSic
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept sic code with leading whitespace" in {
      val data = formDataSic.updated("sic", " 10009")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept sic code with trailing whitespace" in {
      val data = formDataSic.updated("sic", "10009 ")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "accept sic code with multiple whitespaces" in {
      val data = formDataSic.updated("sic", " 100 09 ")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq.empty
    }
    "fail sic code with only whitespaces - empty string" in {
      val data = formDataSic.updated("sic", "    ")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.empty")))
    }
    "fail sic code with invalid characters - wrong format" in {
      val data = formDataSic.updated("sic", "111k2")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.wrong-format")))
    }
    "fail sic code too short" in {
      val data = formDataSic.updated("sic", "111")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.too-short")))
    }
    "fail sic code too long" in {
      val data = formDataSic.updated("sic", "111111")
      val res  = sicCodeForm.bind(data)
      res.errors shouldBe Seq(FormError("sic", Seq("cds.subscription.sic.error.too-long")))
    }
  }
}
