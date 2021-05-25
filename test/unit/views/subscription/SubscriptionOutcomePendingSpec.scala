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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.subscription_outcome_pending
import util.ViewSpec

class SubscriptionOutcomePendingSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarSubscribeRequest)

  private val view = instanceOf[subscription_outcome_pending]

  val orgName       = "Test Organisation Name"
  val eoriNumber    = "EORI123"
  val processedDate = "01 Feb 2020"

  "'Subscription Pending' Page" should {

    "display correct heading" in {
      doc().body.getElementsByTag("h1").text() must startWith(
        s"We are processing the subscription request for $orgName"
      )
    }
    "have the correct class on the h1" in {
      doc().body.getElementsByTag("h1").hasClass("heading-xlarge") mustBe true
    }
    "have the correct class on the message" in {
      doc().body.getElementById("active-from").hasClass("heading-medium") mustBe true
      doc().body.getElementById("eori-number").hasClass("heading-medium") mustBe true
    }
    "have the correct processing date and text" in {
      doc().body.getElementById("active-from").text mustBe s"Application received by HMRC on 1 February 2020"
    }
    "have the correct eori number" in {
      doc().body.getElementById("eori-number").text mustBe s"EORI number: $eoriNumber"
    }
    "have the correct 'what happens next' text" in {
      doc().body
        .getElementById("what-happens-next")
        .text mustBe "What happens next We are processing your Advance Tariff Rulings subscription request. This can take up to 5 working days. You will need to sign back in to see the result of your request. You will not be able to use Advance Tariff Rulings until this issue has been resolved."
    }

    "have a feedback 'continue' button" in {
      val link = doc().body.getElementById("feedback-continue")
      link.text mustBe "More about Advance Tariff Rulings"
      link.attr("href") mustBe "/test-atar/feedback?status=Processing"
    }

    "have a no feedback 'continue' button when config missing" in {
      val link = doc(atarService.copy(feedbackUrl = None)).body.getElementById("feedback-continue")
      link mustBe null
    }
  }

  def doc(service: Service = atarService): Document =
    Jsoup.parse(contentAsString(view(eoriNumber, processedDate, orgName, service)))

}
