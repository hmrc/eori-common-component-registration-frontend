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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sub02_subscription_in_progress
import util.ViewSpec

class Sub02SubscriptionInProgressSpec extends ViewSpec {

  private val name                    = "Test Name"
  private val processedDate           = "1 March 2019"
  private val pageTitleExpectedText   = "Your EORI application is being processed"
  private val pageHeadingExpectedText = s"The EORI application for $name is being processed"
  private val processDateExpectedText = s"Application received by HMRC on $processedDate"

  private val view = instanceOf[sub02_subscription_in_progress]

  "GYE Subscription in progress outcome page" should {

    "have the correct title " in {
      doc.title() must startWith(pageTitleExpectedText)
    }

    "have the correct heading" in {
      doc.getElementById("page-heading").text() mustBe pageHeadingExpectedText
    }

    "have the correct processed date" in {
      doc.getElementById("processed-date").text() mustBe processDateExpectedText
    }

    "have what happens next text" in {
      doc.getElementById("what-happens-next").text() mustBe "What happens next"
    }

    "have we are processing text" in {
      doc
        .getElementById("we-are-processing")
        .text() mustBe "We are processing your EORI application, this can take up to 5 working days. We will send you an email when your application has been processed."
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
