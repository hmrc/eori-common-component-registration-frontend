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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.address_invalid_individual
import util.ViewSpec

class AddressInvalidIndividualSpec extends ViewSpec {

  private implicit val request      = withFakeCSRF(fakeAtarRegisterRequest)
  private val addressInvalidIndView = instanceOf[address_invalid_individual]

  "Address invalid Individual page" should {

    "display correct title" in {
      indDoc.title must startWith("Contact HMRC")
    }

    "display correct heading" in {
      indDoc.body.getElementsByTag("h1").text mustBe "Contact HMRC"
    }

    "display correct info" in {
      indDoc.body
        .getElementById("contact-info")
        .text mustBe "The registered address we hold for you seems to be incorrect. You will need to contact HMRC (opens in new tab) to update your details and return to complete your EORI application."
    }

  }

  private lazy val indDoc: Document = Jsoup.parse(contentAsString(addressInvalidIndView()))

}
