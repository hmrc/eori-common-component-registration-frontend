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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sub01_outcome_processing
import util.ViewSpec

class Sub01OutcomeProcessingSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarSubscribeRequest)

  private val view = instanceOf[sub01_outcome_processing]

  val orgName       = "Test Organisation Name"
  val processedDate = "01 Jan 2019"

  "Sub01 outcome pending Page with name" should {

    "display correct heading" in {
      docWithName.body.getElementsByTag("h1").text() must startWith(
        s"The ATaR application for $orgName is being processed"
      )
    }
    "have the correct class on the h1" in {
      docWithName.body.getElementsByTag("h1").hasClass("heading-xlarge") mustBe true
    }
    "have the correct class on the message" in {
      docWithName.body.getElementById("processed-date").hasClass("heading-medium") mustBe true
    }
    "have the correct processing date and text" in {
      docWithName.body.getElementById("processed-date").text mustBe s"Application received by HMRC on 1 January 2019"
    }
    "have the correct 'what happens next' text" in {
      docWithName.body
        .getElementById("what-happens-next")
        .text mustBe "What happens next We are processing your ATaR application. This can take up to 5 working days. You will need to sign back in to see the result of your registration."
    }

  }

  "Sub01 outcome pending Page without name" should {

    "display correct heading" in {
      docWithoutName.body.getElementsByTag("h1").text() must startWith("The ATaR application is being processed")
    }
    "have the correct class on the h1" in {
      docWithoutName.body.getElementsByTag("h1").hasClass("heading-xlarge") mustBe true
    }
    "have the correct class on the message" in {
      docWithoutName.body.getElementById("processed-date").hasClass("heading-medium") mustBe true
    }
    "have the correct processing date and text" in {
      docWithoutName.body.getElementById("processed-date").text mustBe s"Application received by HMRC on 1 January 2019"
    }
    "have the correct 'what happens next' text" in {
      docWithoutName.body
        .getElementById("what-happens-next")
        .text mustBe "What happens next We are processing your ATaR application. This can take up to 5 working days. You will need to sign back in to see the result of your registration."
    }
  }

  lazy val docWithName: Document    = Jsoup.parse(contentAsString(view(Some(orgName), processedDate)))
  lazy val docWithoutName: Document = Jsoup.parse(contentAsString(view(None, processedDate)))
}
