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
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.YesNoWrongAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.confirm_contact_details
import util.ViewSpec

class ConfirmContactDetailsViewSpec extends ViewSpec {

  private val name                          = "Org Name"
  private val address                       = AddressViewModel("street", "city", Some("SE28 1AA"), "GB")
  private val customsIdUtr                  = Some(Utr("2108834503"))
  private val customsIdNino                 = Some(Nino("ZZ123456Z"))
  private val corporateBody                 = Some(CorporateBody)
  private val partnership                   = Some(Partnership)
  private val form: Form[YesNoWrongAddress] = YesNoWrongAddress.createForm()
  private implicit val request              = withFakeCSRF(FakeRequest())

  private val view = instanceOf[confirm_contact_details]

  "Confirm Contact Details" should {
    "display correct title" in {
      CorporateBodyDoc.title() must startWith("Is this your registered address")
      SoleTraderOrIndividualWithNinoDoc.title() must startWith("Is this your address")
      PartnershipBodyDoc.title() must startWith("Is this your registered address")
    }
    "have the correct h1 text" in {
      CorporateBodyDoc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "Is this your registered address?"
      SoleTraderOrIndividualWithNinoDoc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "Is this your address?"
      PartnershipBodyDoc
        .body()
        .getElementsByTag("h1")
        .text() mustBe "Is this your registered address?"
    }
    "have the correct class on the h1" in {
      CorporateBodyDoc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }
    "have the address" in {
      CorporateBodyDoc.body().getElementById("address").text() mustBe "street city SE28 1AA United Kingdom"

      SoleTraderOrIndividualWithNinoDoc.body().getElementById(
        "address"
      ).text() mustBe "street city SE28 1AA United Kingdom"

      PartnershipBodyDoc.body().getElementById("address").text() mustBe "street city SE28 1AA United Kingdom"

    }
    "have the right legend" in {
      CorporateBodyDoc
        .body()
        .getElementsByTag("legend")
        .text() mustBe "Is this your registered address?"
      SoleTraderOrIndividualWithNinoDoc
        .body()
        .getElementsByTag("legend")
        .text() mustBe "Is this your address?"
      PartnershipBodyDoc
        .body()
        .getElementsByTag("legend")
        .text() mustBe "Is this your registered address?"
    }
    "have an input of type 'radio' for Yes option" in {
      CorporateBodyDoc.body().getElementById("yes-no-wrong-address-yes").attr("type") mustBe "radio"
    }
    "have the right text on the Yes option" in {
      CorporateBodyDoc
        .body()
        .getElementsByAttributeValue("for", "yes-no-wrong-address-yes")
        .text() mustBe "Yes"
      SoleTraderOrIndividualWithNinoDoc
        .body()
        .getElementsByAttributeValue("for", "yes-no-wrong-address-yes")
        .text() mustBe "Yes"
      PartnershipBodyDoc
        .body()
        .getElementsByAttributeValue("for", "yes-no-wrong-address-yes")
        .text() mustBe "Yes"
    }
    "have an input of type 'radio' for Wrong Address option" in {
      CorporateBodyDoc.body().getElementById("yes-no-wrong-address-wrong-address").attr("type") mustBe "radio"
    }
    "have the right text on the Wrong Address option" in {
      CorporateBodyDoc
        .body()
        .getElementsByAttributeValue("for", "yes-no-wrong-address-wrong-address")
        .text() mustBe "No, I want to change the address"
      SoleTraderOrIndividualWithNinoDoc
        .body()
        .getElementsByAttributeValue("for", "yes-no-wrong-address-wrong-address")
        .text() mustBe "No, I want to change my address"
      PartnershipBodyDoc
        .body()
        .getElementsByAttributeValue("for", "yes-no-wrong-address-wrong-address")
        .text() mustBe "No, I want to change the address"
    }
  }

  private lazy val CorporateBodyDoc: Document = {
    val result = view(name, address, customsIdUtr, corporateBody, form, atarService, Journey.Register)
    Jsoup.parse(contentAsString(result))
  }

  private lazy val SoleTraderOrIndividualWithNinoDoc: Document = {
    val result = view(name, address, customsIdNino, None, form, atarService, Journey.Register)
    Jsoup.parse(contentAsString(result))
  }

  private lazy val PartnershipBodyDoc: Document = {
    val result = view(name, address, customsIdUtr, partnership, form, atarService, Journey.Register)
    Jsoup.parse(contentAsString(result))
  }

}
