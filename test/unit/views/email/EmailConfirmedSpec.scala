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

package unit.views.email

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.email_confirmed
import util.ViewSpec

class EmailConfirmedSpec extends ViewSpec {
  implicit val request = withFakeCSRF(FakeRequest())

  val view = instanceOf[email_confirmed]

  "Email Address Confirmed page" should {
    "display correct title" in {
      getEoriDoc.title must startWith("You have confirmed your email address")
    }
    "have the correct h1 text" in {
      getEoriDoc.body.getElementsByTag("h1").text() mustBe "You have confirmed your email address"
    }
    "have the correct class on the h1" in {
      getEoriDoc.body.getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }
    "have a continue button" in {
      getEoriDoc.body.getElementById("continue-button").text() mustBe "Continue"
    }
  }

  lazy val getEoriDoc: Document = {
    val result = view(atarService)
    Jsoup.parse(contentAsString(result))
  }

}
