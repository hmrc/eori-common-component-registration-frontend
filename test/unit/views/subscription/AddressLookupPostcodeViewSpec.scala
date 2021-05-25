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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressLookupParams
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.address_lookup_postcode
import util.ViewSpec

class AddressLookupPostcodeViewSpec extends ViewSpec {

  private val view = instanceOf[address_lookup_postcode]

  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val form = AddressLookupParams.form()

  private val formWithError = form.bind(Map("postcode" -> "invalid"))

  private def doc(selectedOrganisationType: CdsOrganisationType = Company): Document =
    Jsoup.parse(contentAsString(view(form, false, selectedOrganisationType, atarService)))

  private val reviewDoc: Document = Jsoup.parse(contentAsString(view(form, true, Company, atarService)))

  private val docWithErrorSummary = Jsoup.parse(contentAsString(view(formWithError, false, Company, atarService)))

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

    "display hint for company" in {

      doc().body().getElementById(
        "hint"
      ).text() mustBe "This is the address registered with Companies House which may not be your business address."
    }

    "display default hint" in {

      doc(SoleTrader).body().getElementById("hint").text() mustBe "This may not be your business address."
    }

    "display postcode input with label" in {

      val postcodeInputDiv = doc().body().getElementById("postcode-outer")

      postcodeInputDiv.getElementsByTag("label").get(0).text() must startWith("Postcode")
    }

    "display line 1 input with label" in {

      val line1InputDiv = doc().body().getElementById("line1-outer")

      line1InputDiv.getElementsByTag("label").get(0).text() must startWith("Property name or number (optional)")
    }

    "display Find Address button" in {

      val continueButton = doc().body().getElementById("find-address-button")

      continueButton.attr("value") mustBe "Find address"
    }

    "display address is outside UK link" in {

      val outsideUkAddressLink = doc().body().getElementById("cannot-find-address")

      outsideUkAddressLink.text() mustBe "The registered address is outside the UK"
      outsideUkAddressLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address"
    }

    "display address is outside UK link for review mode" in {

      val outsideUkAddressLink = reviewDoc.body().getElementById("cannot-find-address")

      outsideUkAddressLink.text() mustBe "The registered address is outside the UK"
      outsideUkAddressLink.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/address/review"
    }

    "display error summary" in {

      docWithErrorSummary.getElementById("form-error-heading").text() mustBe "There is a problem"
      docWithErrorSummary.getElementsByClass("error-list").get(0).text() mustBe "Enter a valid postcode"
    }
  }
}
