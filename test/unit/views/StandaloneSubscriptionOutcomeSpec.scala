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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.standalone_subscription_outcome
import util.ViewSpec

class StandaloneSubscriptionOutcomeSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarRegisterRequest)

  val eori       = "GB123445562"
  val orgName    = "Test Organisation Name"
  val issuedDate = "01 Jan 2019"

  private val view = instanceOf[standalone_subscription_outcome]

  private val doc: Document = Jsoup.parse(contentAsString(view(eori, orgName, issuedDate)))

  "'Standalone Subscription Outcome' Page with name" should {

    "display correct heading" in {
      doc.body.getElementsByTag("h1").text() must startWith(s"Your new EORI number for $orgName is $eori")
    }
    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").hasClass("govuk-panel__title") mustBe true
    }
    "have the correct processing date and text" in {
      doc.body.getElementById("issued-date").text mustBe s"issued by HMRC on 1 January 2019"
    }
  }

  "Subscription outcome page" should {

    "display additional information paragraph" in {

      val additionalInfoParagraph = doc.body().getElementById("additional-information")

      val listItems = additionalInfoParagraph.getElementsByTag("li")
      listItems.get(0).text() mustBe "Download a text file of your EORI number (12kb)"
      listItems.get(0).getElementsByTag("a").attr(
        "href"
      ) mustBe "/customs-registration-services/atar/register/download/text"
    }

    "display whats happens next paragraph" in {
      val whatsNextParagraph = doc.body().getElementById("when-you-can")
      whatsNextParagraph.getElementsByTag("h2").get(0).text() mustBe "What happens next"
      whatsNextParagraph.getElementsByTag("p").get(
        0
      ).text() mustBe "We will process your application. This can take up to 2 hours."
    }
  }
}
