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
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sub02_eori_already_associated
import util.ViewSpec

class Sub02EoriAlreadyAssociatedSpec extends ViewSpec {

  private val name                    = "John Doe"
  private val processedDate           = "1 March 2019"
  private val expectedPageTitle       = "Our records show that you already have an EORI number"
  private val pageHeadingExpectedText = s"Our records show that $name already has an EORI number."
  private val processDateExpectedText = s"Application received by HMRC on $processedDate"

  private val view = instanceOf[sub02_eori_already_associated]

  "GYE EORI Already Associated outcome page" should {
    "have the correct page title" in {
      doc.title() must startWith(expectedPageTitle)
    }

    "have the right heading" in {
      doc.getElementById("page-heading").text() mustBe pageHeadingExpectedText
    }

    "have the right processed date" in {
      doc.getElementById("processed-date").text() mustBe processDateExpectedText
    }

    "have the right vat registered text" in {
      doc.getElementById("vatRegisteredHeading").text() mustBe "If you are VAT registered"
      doc.getElementById("vatRegisteredPara1").text() mustBe "Your EORI number will be in the following format:"
      doc.getElementById("vatRegisteredPara2").text() mustBe "GB XXXXXXXXX 000 Where XXXXXXXXX is your VAT number."
    }

    "have the feedback link" in {
      doc
        .getElementById("what-you-think")
        .text() must include("What did you think of this service?")
      doc.getElementById("feedback_link").attributes().get("href") must endWith(
        "/feedback/eori-common-component-subscribe-atar"
      )
    }
  }

  implicit val request = withFakeCSRF(FakeRequest.apply("GET", "/atar/subscribe"))

  lazy val doc: Document = Jsoup.parse(contentAsString(view(name, processedDate)))
}
