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
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.you_cannot_change_address_individual
import util.ViewSpec

class YouCannotChangeAddressIndividualSpec extends ViewSpec {

  private implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(fakeAtarRegisterRequest)
  private val youCantChangeAddressIndView                       = inject[you_cannot_change_address_individual]

  "You cannot change address for individual" should {

    "display correct title" in {
      indDoc.title must startWith("Your answers do not match our records")
    }

    "display correct heading" in {
      indDoc.body.getElementsByTag("h1").text mustBe "Your answers do not match our records"
    }

    "display correct info" in {
      indDoc.body
        .getElementById("info-do-no-match")
        .text mustBe "The information you provided does not match the records HMRC holds about you."

      indDoc.body
        .getElementById("try-again")
        .text mustBe "You can try entering your details again."

      indDoc.body
        .getElementById("try_again_link").attr(
          "href"
        ) mustBe "/customs-registration-services/atar/register/matching/organisation-type"

      indDoc.body
        .getElementById("contact-info")
        .text mustBe "If any of your personal details have changed, you need to tell HMRC what has changed (opens in new tab). You can then continue with your EORI number application."

      indDoc.body
        .getElementById("contact_link").attr("href") mustBe "https://www.gov.uk/tell-hmrc-change-of-details"
    }

  }

  private lazy val indDoc: Document = Jsoup.parse(contentAsString(youCantChangeAddressIndView(atarService)))

}
