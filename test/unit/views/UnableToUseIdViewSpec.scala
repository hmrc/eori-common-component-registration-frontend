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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Request
import play.api.test.Helpers.contentAsString
import util.ViewSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.unable_to_use_id

class UnableToUseIdViewSpec extends ViewSpec {

  private val view                   = instanceOf[unable_to_use_id]
  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val doc: Document = Jsoup.parse(contentAsString(view(atarService, "GB123456789123")))

  "Unable to use id page" should {

    "display correct title" in {

      doc.title() must startWith("You cannot access Advance Tariff Rulings with this Government Gateway account")
    }

    "display correct header" in {

      doc.body().getElementsByTag(
        "h1"
      ).text() mustBe "You cannot access Advance Tariff Rulings with this Government Gateway account"
    }

    "display eori paragraph" in {

      val body = doc.body()

      body.getElementById("eori-number-text").text() mustBe "The EORI number:"
      body.getElementById("eori-number").text() mustBe "GB123456789123"
      body.getElementById(
        "para1"
      ).text() mustBe "which is linked to your Government Gateway account is already used by another account to access Advance Tariff Rulings. You will need to sign in with that account to access Advance Tariff Rulings."
    }

    "display additional paragraph" in {

      doc.body().getElementById(
        "para2"
      ).text() mustBe "Sign out and sign in again with a different Government Gateway account."
    }

    "display signout button" in {

      val signoutButton = doc.body().getElementById("signout-button")

      signoutButton.text() mustBe "Sign out"
      signoutButton.attr("href") mustBe "/customs-enrolment-services/atar/subscribe/logout"
    }
  }
}
