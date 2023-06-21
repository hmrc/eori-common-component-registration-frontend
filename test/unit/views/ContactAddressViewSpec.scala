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
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.contact_address
import util.ViewSpec

class ContactAddressViewSpec extends ViewSpec {

  private val form: Form[YesNo] = MatchingForms.contactAddressDetailsYesNoAnswerForm()
  private implicit val request  = withFakeCSRF(FakeRequest())
  private val view              = instanceOf[contact_address]

  private val addressViewModel =
    AddressViewModel(street = "Line 1", city = "city name", postcode = Some("SE28 1AA"), countryCode = "ZZ")

  "Confirm Contact Details" should {
    "display correct title" in {
      doc.title() must startWith(
        "Do you want us to use this address to send you information about your EORI number application?"
      )
    }
    "have the correct h1 text" in {
      doc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "Do you want us to use this address to send you information about your EORI number application?"
    }
    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("govuk-fieldset__heading") mustBe true
    }
    "have the address" in {
      doc.body().getElementById("yes-no-answer-hint").text() mustBe "Line 1 city name SE28 1AA ZZ"
    }
    "have the right legend" in {
      doc
        .body()
        .getElementsByTag("legend")
        .text() mustBe "Do you want us to use this address to send you information about your EORI number application?"
    }
    "have an input of type 'radio' for Yes option" in {
      doc.body().getElementById("yes-no-answer-true").attr("type") mustBe "radio"
    }
    "have the right text on the Yes option" in {
      doc
        .body()
        .getElementsByAttributeValue("for", "yes-no-answer-true")
        .text() mustBe "Yes"
    }
    "have an input of type 'radio' for No Address option" in {
      doc.body().getElementById("yes-no-answer-false").attr("type") mustBe "radio"
    }
    "have the right text on the No Address option" in {
      doc
        .body()
        .getElementsByAttributeValue("for", "yes-no-answer-false")
        .text() mustBe "I want to enter the contact address manually"
    }
  }

  private lazy val doc: Document = {
    val result = view(addressViewModel, false, form, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
