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

package unit.views.registration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_details_recovery
import util.ViewSpec

class BusinessDetailsRecoveryViewSpec extends ViewSpec {

  private val name             = "Org Name"
  private val address          = AddressViewModel("street", "city", Some("SE28 1AA"), "GB")
  private implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[business_details_recovery]

  "Confirm Contact Details" should {
    "display correct title" in {
      CorporateBodyDoc.title() must startWith("We have saved some of the details you gave us")
      SoleTraderOrIndividualDoc.title() must startWith("We have saved some of the details you gave us")
    }
    "have the correct h1 text" in {
      CorporateBodyDoc.body().getElementsByTag("h1").text() mustBe "We have saved some of the details you gave us:"
      SoleTraderOrIndividualDoc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "We have saved some of the details you gave us:"
    }
    "have the correct class on the h1" in {
      CorporateBodyDoc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
      SoleTraderOrIndividualDoc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }
    "have the right labels in the definition list" in {
      CorporateBodyDoc.body().getElementById("name").text() mustBe "Registered organisation name"
      CorporateBodyDoc.body().getElementById("address").text() mustBe "Registered organisation address"

      SoleTraderOrIndividualDoc.body().getElementById("name").text() mustBe "Name"
      SoleTraderOrIndividualDoc.body().getElementById("address").text() mustBe "Address"
    }
    "have the right recovery problem message" in {
      CorporateBodyDoc
        .body()
        .getElementById("recovery-problem")
        .text() mustBe "There was a problem with the service when you last signed in. To try your application again, you need to answer a few more questions."
      SoleTraderOrIndividualDoc
        .body()
        .getElementById("recovery-problem")
        .text() mustBe "There was a problem with the service when you last signed in. To try your application again, you need to answer a few more questions."
    }
  }

  private lazy val CorporateBodyDoc: Document = {
    val result = view(name, address, false, atarService)
    Jsoup.parse(contentAsString(result))
  }

  private lazy val SoleTraderOrIndividualDoc: Document = {
    val result = view(name, address, true, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
