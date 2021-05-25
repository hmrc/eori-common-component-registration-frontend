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

package unit.views.migration

import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{
  AddressLookupParams,
  AddressViewModel,
  CompanyRegisteredCountry
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.check_your_details
import uk.gov.hmrc.play.language.LanguageUtils
import util.ViewSpec

class CheckYourDetailsSpec extends ViewSpec {

  val view = instanceOf[check_your_details]

  private val languageUtils = instanceOf[LanguageUtils]

  private val organisationType = Some(CdsOrganisationType.Company)

  private val contactDetails = Some(
    ContactDetailsModel(
      "John Doe",
      "email@example.com",
      "11111111111",
      None,
      useAddressFromRegistrationDetails = false,
      None,
      None,
      None,
      None
    )
  )

  private val address           = Some(AddressViewModel("Street", "City", Some("Postcode"), "GB"))
  private val sicCode           = Some("00001")
  private val eori              = Some("ZZ123456789112")
  private val email             = Some("email@example.com")
  private val utr               = Some(Utr("UTRXXXXX"))
  private val nameIdOrg         = Some(NameIdOrganisationMatchModel("Name", utr.get.id))
  private val dateTime          = Some(LocalDate.now())
  private val nino              = Some(Nino("AB123456C"))
  private val nameDobMatchModel = Some(NameDobMatchModel("FName", None, "LName", LocalDate.parse("2003-04-08")))
  private val registeredCountry = Some(CompanyRegisteredCountry("GB"))

  "Start page" should {

    "display all details" when {

      "user is during UK Company journey" in {

        doc().body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        doc().body.getElementById("review-tbl__email").text mustBe "email@example.com"

        doc().body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        doc().body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        doc().body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        doc().body.getElementById("review-tbl__orgname_heading").text mustBe "Company name"
        doc().body.getElementById("review-tbl__orgname").text mustBe "Name"
        doc().body
          .getElementById("review-tbl__orgname_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/nameid/review"

        doc().body.getElementById("review-tbl__utr_heading").text mustBe "Corporation Tax UTR number"
        doc().body.getElementById("review-tbl__utr").text mustBe "UTRXXXXX"
        doc().body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/nameid/review"

        doc().body.getElementById("review-tbl__name-and-address_heading").text mustBe "Company address"
        doc().body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        doc().body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"

        doc().body.getElementById("review-tbl__date-established_heading").text mustBe "Date of establishment"
        doc().body.getElementById("review-tbl__date-established").text mustBe languageUtils.Dates.formatDate(
          dateTime.get
        )
        doc().body
          .getElementById("review-tbl__date-established_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/date-established/review"
      }

      "user is during UK Sole Trader UTR journey" in {

        val page = doc(true, nameIdOrganisationDetails = None)

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__full-name_heading").text mustBe "Full name"
        page.body.getElementById("review-tbl__full-name").text mustBe "FName LName"
        page.body
          .getElementById("review-tbl__full-name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__date-of-birth_heading").text mustBe "Date of birth"
        page.body.getElementById("review-tbl__date-of-birth").text mustBe "8 April 2003"
        page.body
          .getElementById("review-tbl__date-of-birth_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__utr_heading").text mustBe "UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe "UTRXXXXX"
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/chooseid/review"

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Your address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }

      "user is during UK Sole Trader NINo journey" in {

        val page = doc(true, customsId = nino, nameIdOrganisationDetails = None)

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__full-name_heading").text mustBe "Full name"
        page.body.getElementById("review-tbl__full-name").text mustBe "FName LName"
        page.body
          .getElementById("review-tbl__full-name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__date-of-birth_heading").text mustBe "Date of birth"
        page.body.getElementById("review-tbl__date-of-birth").text mustBe "8 April 2003"
        page.body
          .getElementById("review-tbl__date-of-birth_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__nino_heading").text mustBe "National Insurance number"
        page.body.getElementById("review-tbl__nino").text mustBe "AB123456C"
        page.body
          .getElementById("review-tbl__nino_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/chooseid/review"

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Your address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }

      "user is during ROW Organisation journey without UTR" in {

        val page = doc(
          customsId = None,
          isThirdCountrySubscription = true,
          nameIdOrganisationDetails = None,
          companyRegisteredCountry = registeredCountry
        )

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__org_name_heading").text mustBe "Organisation name"
        page.body.getElementById("review-tbl__org_name").text mustBe "Org name"
        page.body
          .getElementById("review-tbl__org_name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/name/review"

        page.body.getElementById("review-tbl__utr_heading").text mustBe "UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe "Not entered"
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-utr/review"

        page.body.getElementById("review-tbl__country-location_heading").text mustBe "Country location"
        page.body.getElementById("review-tbl__country-location").text mustBe "United Kingdom"
        page.body
          .getElementById("review-tbl__country-location_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-country/review"

        page.body.getElementById("review-tbl__contact-details_heading").text mustBe "Contact"
        page.body.getElementById("review-tbl__contact-details").text mustBe "John Doe 11111111111"
        page.body
          .getElementById("review-tbl__contact-details_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/contact-details/review"

        page.body.getElementById("review-tbl__date-established_heading").text mustBe "Date of establishment"
        page.body.getElementById("review-tbl__date-established").text mustBe languageUtils.Dates.formatDate(
          dateTime.get
        )
        page.body
          .getElementById("review-tbl__date-established_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/date-established/review"
      }

      "user is during ROW Organisation journey with UTR" in {

        val page = doc(isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__org_name_heading").text mustBe "Organisation name"
        page.body.getElementById("review-tbl__org_name").text mustBe "Org name"
        page.body
          .getElementById("review-tbl__org_name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/name/review"

        page.body.getElementById("review-tbl__utr_heading").text mustBe "Corporation Tax UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe "UTRXXXXX"
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-utr/review"

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Organisation address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"

        page.body.getElementById("review-tbl__date-established_heading").text mustBe "Date of establishment"
        page.body.getElementById("review-tbl__date-established").text mustBe languageUtils.Dates.formatDate(
          dateTime.get
        )
        page.body
          .getElementById("review-tbl__date-established_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/date-established/review"
      }

      "user is during ROW Individual journey without UTR and NINo" in {

        val page = doc(
          isIndividualSubscriptionFlow = true,
          customsId = None,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true,
          companyRegisteredCountry = registeredCountry
        )

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__full-name_heading").text mustBe "Full name"
        page.body.getElementById("review-tbl__full-name").text mustBe "FName LName"
        page.body
          .getElementById("review-tbl__full-name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__date-of-birth_heading").text mustBe "Date of birth"
        page.body.getElementById("review-tbl__date-of-birth").text mustBe "8 April 2003"
        page.body
          .getElementById("review-tbl__date-of-birth_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__utr_heading").text mustBe "UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe "Not entered"
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-utr/review"

        page.body.getElementById("review-tbl__nino_heading").text mustBe "National Insurance number"
        page.body.getElementById("review-tbl__nino").text mustBe "Not entered"
        page.body
          .getElementById("review-tbl__nino_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-nino/review"

        page.body.getElementById("review-tbl__country-location_heading").text mustBe "Country location"
        page.body.getElementById("review-tbl__country-location").text mustBe "United Kingdom"
        page.body
          .getElementById("review-tbl__country-location_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-country/review"

        page.body.getElementById("review-tbl__contact-details_heading").text mustBe "Contact"
        page.body.getElementById("review-tbl__contact-details").text mustBe "John Doe 11111111111"
        page.body
          .getElementById("review-tbl__contact-details_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/contact-details/review"
      }

      "user is during ROW Individual journey with UTR" in {

        val page =
          doc(isIndividualSubscriptionFlow = true, nameIdOrganisationDetails = None, isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__full-name_heading").text mustBe "Full name"
        page.body.getElementById("review-tbl__full-name").text mustBe "FName LName"
        page.body
          .getElementById("review-tbl__full-name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__date-of-birth_heading").text mustBe "Date of birth"
        page.body.getElementById("review-tbl__date-of-birth").text mustBe "8 April 2003"
        page.body
          .getElementById("review-tbl__date-of-birth_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__utr_heading").text mustBe "UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe "UTRXXXXX"
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-utr/review"

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Your address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }

      "user is during ROW Individual journey with NINo" in {

        val page = doc(
          isIndividualSubscriptionFlow = true,
          customsId = nino,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true
        )

        page.body.getElementById("review-tbl__email_heading").text mustBe "Email address"
        page.body.getElementById("review-tbl__email").text mustBe "email@example.com"

        page.body.getElementById("review-tbl__eori-number_heading").text mustBe "EORI number"
        page.body.getElementById("review-tbl__eori-number").text mustBe "ZZ123456789112"
        page.body
          .getElementById("review-tbl__eori-number_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/review"

        page.body.getElementById("review-tbl__full-name_heading").text mustBe "Full name"
        page.body.getElementById("review-tbl__full-name").text mustBe "FName LName"
        page.body
          .getElementById("review-tbl__full-name_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__date-of-birth_heading").text mustBe "Date of birth"
        page.body.getElementById("review-tbl__date-of-birth").text mustBe "8 April 2003"
        page.body
          .getElementById("review-tbl__date-of-birth_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/namedob/review"

        page.body.getElementById("review-tbl__utr_heading").text mustBe "UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe "Not entered"
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-utr/review"

        page.body.getElementById("review-tbl__nino_heading").text mustBe "National Insurance number"
        page.body.getElementById("review-tbl__nino").text mustBe "AB123456C"
        page.body
          .getElementById("review-tbl__nino_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-nino/review"

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Your address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }
    }

    "not display address" when {

      "user is during organisation ROW journey without UTR" in {

        val page = doc(
          customsId = None,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true,
          companyRegisteredCountry = registeredCountry
        )

        page.body.getElementById("review-tbl__country-location_heading").text mustBe "Country location"
        page.body.getElementById("review-tbl__country-location").text mustBe "United Kingdom"
        page.body
          .getElementById("review-tbl__country-location_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-country/review"

        page.body.getElementById("review-tbl__name-and-address_heading") mustBe null
        page.body.getElementById("review-tbl__name-and-address") mustBe null
        page.body.getElementById("review-tbl__name-and-address_change") mustBe null
      }

      "user is during individual ROW journey without UTR" in {

        val page = doc(
          isIndividualSubscriptionFlow = true,
          customsId = None,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true,
          companyRegisteredCountry = registeredCountry
        )

        page.body.getElementById("review-tbl__country-location_heading").text mustBe "Country location"
        page.body.getElementById("review-tbl__country-location").text mustBe "United Kingdom"
        page.body
          .getElementById("review-tbl__country-location_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/row-country/review"

        page.body.getElementById("review-tbl__name-and-address_heading") mustBe null
        page.body.getElementById("review-tbl__name-and-address") mustBe null
        page.body.getElementById("review-tbl__name-and-address_change") mustBe null
      }
    }

    "not display country location" when {

      "user is during organisation ROW journey with UTR" in {

        val page = doc(isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__country-location_heading") mustBe null
        page.body.getElementById("review-tbl__country-location") mustBe null
        page.body.getElementById("review-tbl__country-location_change") mustBe null

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Organisation address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }

      "user is during individual ROW journey with UTR" in {

        val page =
          doc(isIndividualSubscriptionFlow = true, nameIdOrganisationDetails = None, isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__country-location_heading") mustBe null
        page.body.getElementById("review-tbl__country-location") mustBe null
        page.body.getElementById("review-tbl__country-location_change") mustBe null

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Your address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }

      "user is during individual ROW journey with NINo" in {

        val page = doc(
          customsId = nino,
          isIndividualSubscriptionFlow = true,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true
        )

        page.body.getElementById("review-tbl__country-location_heading") mustBe null
        page.body.getElementById("review-tbl__country-location") mustBe null
        page.body.getElementById("review-tbl__country-location_change") mustBe null

        page.body.getElementById("review-tbl__name-and-address_heading").text mustBe "Your address"
        page.body.getElementById("review-tbl__name-and-address").text mustBe "Street City Postcode United Kingdom"
        page.body
          .getElementById("review-tbl__name-and-address_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
      }
    }

    "not display any sole trader information" when {

      "user is during UK company journey" in {

        doc().body.getElementById("review-tbl__full-name_heading") mustBe null
        doc().body.getElementById("review-tbl__full-name") mustBe null
        doc().body.getElementById("review-tbl__full-name_change") mustBe null

        doc().body.getElementById("review-tbl__date-of-birth_heading") mustBe null
        doc().body.getElementById("review-tbl__date-of-birth") mustBe null
        doc().body.getElementById("review-tbl__date-of-birth_change") mustBe null

        doc().body.getElementById("review-tbl__nino_heading") mustBe null
        doc().body.getElementById("review-tbl__nino") mustBe null
        doc().body.getElementById("review-tbl__nino_change") mustBe null
      }

      "user is during ROW organisation journey" in {

        val page = doc(isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__full-name_heading") mustBe null
        page.body.getElementById("review-tbl__full-name") mustBe null
        page.body.getElementById("review-tbl__full-name_change") mustBe null

        page.body.getElementById("review-tbl__date-of-birth_heading") mustBe null
        page.body.getElementById("review-tbl__date-of-birth") mustBe null
        page.body.getElementById("review-tbl__date-of-birth_change") mustBe null

        page.body.getElementById("review-tbl__nino_heading") mustBe null
        page.body.getElementById("review-tbl__nino") mustBe null
        page.body.getElementById("review-tbl__nino_change") mustBe null
      }
    }

    "not display any company information" when {

      "user is during UK individual journey" in {

        val page = doc(true, nameIdOrganisationDetails = None)

        page.body.getElementById("review-tbl__orgname_heading") mustBe null
        page.body.getElementById("review-tbl__orgname") mustBe null
        page.body.getElementById("review-tbl__orgname_change") mustBe null

        page.body.getElementById("review-tbl__date-established_heading") mustBe null
        page.body.getElementById("review-tbl__date-established") mustBe null
        page.body.getElementById("review-tbl__date-established_change") mustBe null
      }

      "user is during ROW individual journey" in {

        val page = doc(
          isIndividualSubscriptionFlow = true,
          customsId = nino,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true
        )

        page.body.getElementById("review-tbl__orgname_heading") mustBe null
        page.body.getElementById("review-tbl__orgname") mustBe null
        page.body.getElementById("review-tbl__orgname_change") mustBe null

        page.body.getElementById("review-tbl__date-established_heading") mustBe null
        page.body.getElementById("review-tbl__date-established") mustBe null
        page.body.getElementById("review-tbl__date-established_change") mustBe null
      }
    }

    "not display NINo" when {

      "UTR exists and user is during UK individual journey" in {

        val page = doc(true, nameIdOrganisationDetails = None)

        page.body.getElementById("review-tbl__nino_heading") mustBe null
        page.body.getElementById("review-tbl__nino") mustBe null
        page.body.getElementById("review-tbl__nino_change") mustBe null
      }

      "UTR exists and user is during ROW individual journey" in {
        val page =
          doc(isIndividualSubscriptionFlow = true, nameIdOrganisationDetails = None, isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__nino_heading") mustBe null
        page.body.getElementById("review-tbl__nino") mustBe null
        page.body.getElementById("review-tbl__nino_change") mustBe null
      }
    }

    "not display contact details for ROW journeys" when {

      "user is organisation with UTR" in {

        val page = doc(isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__contact-details_heading") mustBe null
        page.body.getElementById("review-tbl__contact-details") mustBe null
        page.body.getElementById("review-tbl__contact-details_change") mustBe null
      }

      "user is individual with UTR" in {

        val page =
          doc(isIndividualSubscriptionFlow = true, nameIdOrganisationDetails = None, isThirdCountrySubscription = true)

        page.body.getElementById("review-tbl__contact-details_heading") mustBe null
        page.body.getElementById("review-tbl__contact-details") mustBe null
        page.body.getElementById("review-tbl__contact-details_change") mustBe null
      }

      "user is individual with NINo" in {

        val page = doc(
          isIndividualSubscriptionFlow = true,
          customsId = nino,
          nameIdOrganisationDetails = None,
          isThirdCountrySubscription = true
        )

        page.body.getElementById("review-tbl__contact-details_heading") mustBe null
        page.body.getElementById("review-tbl__contact-details") mustBe null
        page.body.getElementById("review-tbl__contact-details_change") mustBe null
      }
    }

    "display note on the bottom of the page with 'Confirm and send' button" in {

      doc().body.getElementById("declaration").text mustBe "Declaration"
      doc().body.getElementById(
        "disclaimer"
      ).text mustBe "By sending this application you confirm that the information you are providing is correct and complete."
      doc().body.getElementById("continue").attr("value") mustBe "Confirm and send"
    }

    "should display individual UTR" when {

      "user has organisation UTR in cache" in {

        val individualUtr   = Utr("1111111111")
        val organisationUtr = "2222222222"

        val page = doc(
          isIndividualSubscriptionFlow = true,
          customsId = Some(individualUtr),
          nameIdOrganisationDetails = Some(NameIdOrganisationMatchModel("test", organisationUtr))
        )

        page.body.getElementById("review-tbl__utr_heading").text mustBe "UTR number"
        page.body.getElementById("review-tbl__utr").text mustBe individualUtr.id
        page.body
          .getElementById("review-tbl__utr_change")
          .attr("href") mustBe "/customs-enrolment-services/atar/subscribe/chooseid/review"

        page.body.toString mustNot contain(organisationUtr)
      }
    }
  }

  def doc(
    isIndividualSubscriptionFlow: Boolean = false,
    customsId: Option[CustomsId] = utr,
    orgType: Option[CdsOrganisationType] = organisationType,
    nameDobMatchModel: Option[NameDobMatchModel] = nameDobMatchModel,
    isThirdCountrySubscription: Boolean = false,
    nameIdOrganisationDetails: Option[NameIdOrganisationMatchModel] = nameIdOrg,
    existingEori: Option[ExistingEori] = None,
    companyRegisteredCountry: Option[CompanyRegisteredCountry] = None,
    addressLookupParams: Option[AddressLookupParams] = None
  ): Document = {

    implicit val request = withFakeCSRF(FakeRequest().withSession(("selected-user-location", "third-country")))
    val result = view(
      isThirdCountrySubscription,
      isIndividualSubscriptionFlow,
      orgType,
      contactDetails,
      address,
      sicCode,
      eori,
      existingEori,
      email,
      nameIdOrganisationDetails,
      Some(NameOrganisationMatchModel("Org name")),
      nameDobMatchModel,
      dateTime,
      None,
      customsId,
      companyRegisteredCountry,
      addressLookupParams,
      atarService,
      Journey.Subscribe
    )
    Jsoup.parse(contentAsString(result))
  }

}
