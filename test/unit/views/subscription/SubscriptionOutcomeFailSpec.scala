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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.subscription_outcome_fail
import util.ViewSpec

class SubscriptionOutcomeFailSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarSubscribeRequest)

  private val view = instanceOf[subscription_outcome_fail]

  val orgName       = "Test Organisation Name"
  val processedDate = "01 Jan 2019"

  "'Subscription Fail' Page" should {

    "have the correct title " in {
      doc().title() must startWith(s"The subscription request has been unsuccessful")
    }

    "display correct heading" in {
      doc().body.getElementsByTag("h1").text() must startWith(
        s"The subscription request for $orgName has been unsuccessful"
      )
    }
    "have the correct class on the h1" in {
      doc().body.getElementsByTag("h1").hasClass("heading-xlarge") mustBe true
    }
    "have the correct class on the message" in {
      doc().body.getElementById("active-from").hasClass("heading-medium") mustBe true
    }
    "have the correct processing date and text" in {
      doc().body.getElementById("active-from").text mustBe s"Application received by HMRC on 1 January 2019"
    }

    "have a feedback 'continue' button" in {
      val link = doc().body.getElementById("feedback-continue")
      link.text mustBe "More about Advance Tariff Rulings"
      link.attr("href") mustBe "/test-atar/feedback?status=Failed"
    }

    "have a no feedback 'continue' button when config missing" in {
      val link = doc(atarService.copy(feedbackUrl = None)).body.getElementById("feedback-continue")
      link mustBe null
    }
  }

  def doc(service: Service = atarService): Document =
    Jsoup.parse(contentAsString(view(processedDate, orgName, service)))

}
