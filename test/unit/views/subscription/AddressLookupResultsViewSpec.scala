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

package unit.views.subscription

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Request
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{AddressLookupParams, AddressResultsForm}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.AddressLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.address_lookup_results
import util.ViewSpec

class AddressLookupResultsViewSpec extends ViewSpec {

  private val view = instanceOf[address_lookup_results]

  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val params         = AddressLookupParams("AA11 1AA", Some("Flat 1"))
  private val allowedAddress = Seq(AddressLookup("Line 1", "City", "BB11 1BB", "GB"))

  private val form = AddressResultsForm.form(allowedAddress.map(_.dropDownView))

  private val formWithError =
    AddressResultsForm.form(allowedAddress.map(_.dropDownView)).bind(Map("address" -> "invalid"))

  private def doc(selectedOrganisationType: CdsOrganisationType = Company): Document =
    Jsoup.parse(contentAsString(view(form, params, allowedAddress, false, selectedOrganisationType, atarService)))

  private val reviewDoc: Document =
    Jsoup.parse(contentAsString(view(form, params, allowedAddress, true, Company, atarService)))

  private val docWithErrorSummary =
    Jsoup.parse(contentAsString(view(formWithError, params, allowedAddress, false, Company, atarService)))

  "Address Lookup Postcode page" should {

    "display title for company" in {

      doc().title() must startWith("What is your registered company address?")
    }

    "display header for company" in {

      doc().body().getElementsByTag("h1").text() mustBe "What is your registered company address?"
    }

    "display title for individual" in {

      doc(SoleTrader).title() must startWith("What is your registered business address?")
    }

    "display header for individual" in {

      doc(SoleTrader).body().getElementsByTag("h1").text() mustBe "What is your registered business address?"
    }

    "display title for partnership" in {

      doc(Partnership).title() must startWith("What is your registered partnership address?")
    }

    "display header for partnership" in {

      doc(Partnership).body().getElementsByTag("h1").text() mustBe "What is your registered partnership address?"
    }

    "display title for charity" in {

      doc(CharityPublicBodyNotForProfit).title() must startWith("What is your registered organisation address?")
    }

    "display header for charity" in {

      doc(CharityPublicBodyNotForProfit).body().getElementsByTag(
        "h1"
      ).text() mustBe "What is your registered organisation address?"
    }

    "display summary of params" in {

      doc().body().getElementById("review-tbl__postcode_heading").text mustBe "Postcode"
      doc().body().getElementById("review-tbl__postcode").text mustBe "AA11 1AA"

      doc().body().getElementById("review-tbl__line1_heading").text mustBe "Property name or number"
      doc().body().getElementById("review-tbl__line1").text mustBe "Flat 1"
    }

    "display summary of params with 'Not found' for property name or number" in {

      val docWithNotFound =
        Jsoup.parse(
          contentAsString(view(form, params.copy(skippedLine1 = true), allowedAddress, false, Company, atarService))
        )

      docWithNotFound.body().getElementById("review-tbl__line1_heading").text mustBe "Property name or number"
      docWithNotFound.body().getElementById("review-tbl__line1").text mustBe "Not found"
    }

    "display change link to params page" in {

      val postcodeChangeLink = doc().body().getElementById("review-tbl__postcode_change")
      val line1ChangeLink    = doc().body().getElementById("review-tbl__line1_change")

      postcodeChangeLink.text() must startWith("Change")
      postcodeChangeLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address-postcode"

      line1ChangeLink.text() must startWith("Change")
      line1ChangeLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address-postcode"
    }

    "display change link to params page - review mode" in {

      val postcodeChangeLink = reviewDoc.body().getElementById("review-tbl__postcode_change")
      val line1ChangeLink    = reviewDoc.body().getElementById("review-tbl__line1_change")

      postcodeChangeLink.text() must startWith("Change")
      postcodeChangeLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address-postcode/review"

      line1ChangeLink.text() must startWith("Change")
      line1ChangeLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address-postcode/review"
    }

    "display dropdown with label" in {

      val addressContainer = doc().body().getElementById("address-container")

      addressContainer.getElementsByTag("label").get(0).text() mustBe "Select your address"

      val dropdown = addressContainer.getElementById("address")
      val options  = dropdown.getElementsByTag("option")

      options.get(0).text() mustBe empty
      options.get(1).text() mustBe "Line 1, City, BB11 1BB"
    }

    "display manual address link" in {

      val manualAddressLink = doc().body().getElementById("cannot-find-address")

      manualAddressLink.text() mustBe "I can't find my address in the list"
      manualAddressLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address"
    }

    "display review manual address link" in {

      val manualAddressLink = reviewDoc.body().getElementById("cannot-find-address")

      manualAddressLink.text() mustBe "I can't find my address in the list"
      manualAddressLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
    }

    "display Continue button" in {

      val continueButton = doc().body().getElementById("continue-button")

      continueButton.attr("value") mustBe "Continue"
    }

    "display error summary" in {

      docWithErrorSummary.getElementById("form-error-heading").text() mustBe "There is a problem"
      docWithErrorSummary.getElementsByClass("error-list").get(0).text() mustBe "Please select address from the list"
    }
  }
}
