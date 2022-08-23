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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm.addressDetailsCreateForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.address
import util.ViewSpec

class AddressSpec extends ViewSpec {

  private val form: Form[AddressViewModel] = addressDetailsCreateForm
  private val isInReviewMode               = false
  private implicit val request             = withFakeCSRF(FakeRequest())

  private val aFewCountries = List(
    Country("France", "country:FR"),
    Country("Germany", "country:DE"),
    Country("Italy", "country:IT"),
    Country("Japan", "country:JP")
  )

  private val view = instanceOf[address]

  "Address Page" should {
    "display correct title" in {
      doc.title() must startWith("EORI application contact address")
    }
    "have the correct h1 text" in {
      doc.body().getElementsByTag("h1").text() mustBe "EORI application contact address"
    }

    "have an input of type 'text' for street" in {
      doc.body().getElementById("street").attr("type") mustBe "text"
    }
    "have an input of type 'text' for city" in {
      doc.body().getElementById("city").attr("type") mustBe "text"
    }

    "have an input of type 'text' for postcode" in {
      doc.body().getElementById("postcode").attr("type") mustBe "text"
    }
    "have an input of type 'text' for countryCode" in {
      doc.body().getElementById("countryCode").tagName() mustBe "select"
    }

    "have an label for line-1 of street" in {
      doc.body().getElementsByAttributeValue("for", "street").text mustBe "Address line 1"
    }

    "have an label for line-1 of city (optional)" in {
      doc.body().getElementsByAttributeValue("for", "city").text mustBe "Town or city"
    }

    "have an label for line-1 of Postcode (optional)" in {
      doc.body().getElementsByAttributeValue("for", "postcode").text mustBe "Postcode"
    }

    "have an label for line-1 of Country" in {
      doc
        .body()
        .getElementsByAttributeValue("for", "countryCode")
        .text mustBe "Country"
    }
  }

  "Address Page in review mode" should {

    "have the continue button say 'Save and review'" in {
      docWithErrors.body().getElementsByClass("govuk-button").first().text mustBe "Save and review"
    }
  }

  private lazy val doc: Document = {
    val result =
      view(form, aFewCountries, isInReviewMode, atarService)
    Jsoup.parse(contentAsString(result))
  }

  private lazy val docWithErrors = {
    val result = view(form, aFewCountries, isInReviewMode = true, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
