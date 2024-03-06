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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{
  ThirdCountryIndividualId,
  ThirdCountrySoleTraderId
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SixLineAddressMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries._
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.SixLineAddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.six_line_address
import util.ViewSpec

class SixLineAddressSpec extends ViewSpec {

  private val form: Form[SixLineAddressMatchModel]              = thirdCountrySixLineAddressForm
  private val isInReviewMode                                    = false
  private implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())
  private val ThirdCountryOrganisationId                        = "third-country-organisation"
  private val viewSixLineAddress                                = SixLineAddressViewModel

  private val servicesToTest   = Seq(ThirdCountryIndividualId, ThirdCountrySoleTraderId)
  private val anyOtherServices = ThirdCountryOrganisationId

  private val aFewCountries = List(
    Country("France", "country:FR"),
    Country("Germany", "country:DE"),
    Country("Italy", "country:IT"),
    Country("Japan", "country:JP")
  )

  private val view = instanceOf[six_line_address]

  "header Label" should {
    "return correct header for individual " in servicesToTest.foreach { service =>
      viewSixLineAddress.headerLabel(service) mustBe messages("cds.matching.individual-address.header")
    }
    "return correct header for organisation" in {
      viewSixLineAddress.headerLabel(anyOtherServices) mustBe messages("cds.matching.organisation-address.header")
    }
  }

  "Rest of World (ROW) Enter your organisation address Page" should {
    val doc = generatePage(ThirdCountriesInCountryPicker, ThirdCountryOrganisationId)
    "display correct title" in {
      doc.title() must startWith("Enter your organisation address")
    }
    "have the correct h1 text" in {
      doc.body().getElementsByTag("h1").text() mustBe "Enter your organisation address"
    }
    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }
    "have an input of type 'text' for line-1" in {
      doc.body().getElementById("line-1").attr("type") mustBe "text"
    }
    "have an input of type 'text' for line-2" in {
      doc.body().getElementById("line-2").attr("type") mustBe "text"
    }
    "have an input of type 'text' for line-3" in {
      doc.body().getElementById("line-3").attr("type") mustBe "text"
    }
    "have an input of type 'text' for line-4" in {
      doc.body().getElementById("line-4").attr("type") mustBe "text"
    }
    "have an input of type 'text' for postcode" in {
      doc.body().getElementById("postcode").attr("type") mustBe "text"
    }
    "have an input of type 'text' for countryCode" in {
      doc.body().getElementById("countryCode").tagName() mustBe "select"
    }

    "have an label for line-1 of Address line 1" in {
      doc.body().getElementsByAttributeValue("for", "line-1").text mustBe "Address line 1"
    }

    "have an label for line-1 of Address line 2 (optional)" in {
      doc.body().getElementsByAttributeValue("for", "line-2").text mustBe "Address line 2 (optional)"
    }

    "have an label for line-1 of Town or city" in {
      doc.body().getElementsByAttributeValue("for", "line-3").text mustBe "Town or city"
    }

    "have an label for line-1 of Region or state (optional)" in {
      doc.body().getElementsByAttributeValue("for", "line-4").text mustBe "Region or state (optional)"
    }

    "have an label for line-1 of Postcode (optional)" in {
      doc.body().getElementsByAttributeValue("for", "postcode").text mustBe "Postal code (optional)"
    }

    "have an label for line-1 of Country" in {
      doc
        .body()
        .getElementsByAttributeValue("for", "countryCode")
        .text mustBe "Country"
    }
  }

  "Rest of World (ROW) Enter your organisation address Page in review mode" should {

    "have the continue button say 'Save and review'" in {
      docWithErrors.body().getElementById("continue-button").text mustBe "Save and review"
    }
  }

  "Chanel Islands Enter your organisation address Page" should {
    val doc = generatePage(IslandsInCountryPicker, ThirdCountryOrganisationId)
    "display correct title" in {
      doc.title() must startWith("Enter your organisation address")
    }
    "have the correct h1 text" in {
      doc.body().getElementsByTag("h1").text() mustBe "Enter your organisation address"
    }
    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }
    "have an input of type 'text' for line-1" in {
      doc.body().getElementById("line-1").attr("type") mustBe "text"
    }
    "have an input of type 'text' for line-2" in {
      doc.body().getElementById("line-2").attr("type") mustBe "text"
    }
    "have an input of type 'text' for line-3" in {
      doc.body().getElementById("line-3").attr("type") mustBe "text"
    }
    "have an input of type 'text' for line-4" in {
      doc.body().getElementById("line-4").attr("type") mustBe "text"
    }
    "have an input of type 'text' for postcode" in {
      doc.body().getElementById("postcode").attr("type") mustBe "text"
    }
    "have an input of type 'text' for countryCode" in {
      doc.body().getElementById("countryCode").tagName() mustBe "select"
    }
    "have an label for line-1 of Address line 1" in {
      doc.body().getElementsByAttributeValue("for", "line-1").text mustBe "Address line 1"
    }

    "have an label for line-1 of Address line 2 (optional)" in {
      doc.body().getElementsByAttributeValue("for", "line-2").text mustBe "Address line 2 (optional)"
    }

    "have an label for line-1 of Town or city" in {
      doc.body().getElementsByAttributeValue("for", "line-3").text mustBe "Town or city"
    }

    "have an label for line-1 of Region or state (optional)" in {
      doc.body().getElementsByAttributeValue("for", "line-4").text mustBe "Region or state (optional)"
    }

    "have an label for Postcode" in {
      doc.body().getElementsByAttributeValue("for", "postcode").text mustBe messages(
        "cds.matching.organisation-address.postcode"
      )
    }

    "have an label for line-1 of Country" in {
      doc
        .body()
        .getElementsByAttributeValue("for", "countryCode")
        .text mustBe "Country"
    }
  }

  def generatePage(countriesInCountryPicker: CountriesInCountryPicker, cdsOrgType: String): Document = {
    val result =
      view(isInReviewMode, form, aFewCountries, countriesInCountryPicker, cdsOrgType, atarService)
    Jsoup.parse(contentAsString(result))
  }

  private lazy val docWithErrors = {
    val result = view(
      isInReviewMode = true,
      form,
      aFewCountries,
      ThirdCountriesInCountryPicker,
      ThirdCountryOrganisationId,
      atarService
    )
    Jsoup.parse(contentAsString(result))
  }

}
