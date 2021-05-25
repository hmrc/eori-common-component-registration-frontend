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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sign_in_with_different_details
import util.ViewSpec

class SignInWithDifferentDetailsSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarSubscribeRequest)

  private val view = instanceOf[sign_in_with_different_details]

  val orgName = "Test Organisation Name"

  "'You need to sign in with different details' Page with name" should {

    "display correct heading" in {
      docWithName.body.getElementsByTag("h1").text() must startWith("You need to sign in with different details")
    }

    "include the heading in the title" in {
      docWithName.title must startWith(docWithName.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      docWithName.body.getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have the correct explanation text" in {
      docWithName.body
        .getElementById("para1")
        .text mustBe s"$orgName has already registered for Advance Tariff Rulings with a different Government Gateway."
      docWithName.body
        .getElementById("para2")
        .text mustBe "You need to sign in with the Government Gateway you used to register."
    }

    "have a sign out button" in {
      docWithName.body.getElementById("sign-out-button").text mustBe "Sign out"
      docWithName.body.getElementsByClass("button").attr("href") must endWith("/subscribe/logout")
    }
  }

  "'You need to sign in with different details' Page without name" should {

    "display correct heading" in {
      docWithoutName.body.getElementsByTag("h1").text() must startWith("You need to sign in with different details")
    }

    "include the heading in the title" in {
      docWithoutName.title must startWith(docWithName.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      docWithoutName.body.getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have the correct explanation text" in {
      docWithoutName.body
        .getElementById("para1")
        .text mustBe s"You have already registered for Advance Tariff Rulings with a different Government Gateway."
      docWithoutName.body
        .getElementById("para2")
        .text mustBe "You need to sign in with the Government Gateway you used to register."
    }

    "have a sign out button" in {
      docWithoutName.body.getElementById("sign-out-button").text mustBe "Sign out"
      docWithoutName.body.getElementsByClass("button").attr("href") must endWith("/subscribe/logout")
    }
  }

  lazy val docWithName: Document    = Jsoup.parse(contentAsString(view(Some(orgName))))
  lazy val docWithoutName: Document = Jsoup.parse(contentAsString(view(None)))
}
