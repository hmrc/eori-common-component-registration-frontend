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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.we_cannot_confirm_your_identity
import util.ViewSpec

class WeCannotConfirmYourIdentitySpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[we_cannot_confirm_your_identity]

  "The 'We cannot confirm your identity' Page" should {

    "display correct title" in {
      doc.body().getElementsByTag("h1").text() mustBe "We cannot verify your VAT details"
    }

    "have a matching title and heading" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have the correct explanation text" in {
      doc
        .body()
        .getElementById("explanation")
        .text mustBe "The UK VAT details you have provided do not match what we hold."
    }

    "have the option to try again" in {
      doc.body().getElementById("try-again").text mustBe "Try again"
    }

    "have the VAT Details link for the try again button" in {
      doc
        .body()
        .getElementById("try-again")
        .attr("href") mustBe "/customs-enrolment-services/atar/register/what-are-your-uk-vat-details"
    }

    "have the VAT Details link for the try again button in review mode" in {
      val doc = Jsoup.parse(contentAsString(view(true, atarService)))
      doc
        .body()
        .getElementById("try-again")
        .attr("href") mustBe "/customs-enrolment-services/atar/register/what-are-your-uk-vat-details/review"
    }
  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view(false, atarService)))
}
