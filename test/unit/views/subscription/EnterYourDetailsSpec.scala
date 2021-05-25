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
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameDobMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.enter_your_details
import util.ViewSpec

class EnterYourDetailsSpec extends ViewSpec {
  val form: Form[NameDobMatchModel] = enterNameDobForm
  val isInReviewMode                = false
  val previousPageUrl               = "/"
  implicit val request              = withFakeCSRF(FakeRequest())

  private val view = instanceOf[enter_your_details]

  "Subscription Enter Your Details Page" should {
    "display correct title" in {
      doc.title() must startWith("Enter your details")
    }
    "have the correct h1 text" in {
      doc.body().getElementsByTag("h1").text() mustBe "Enter your details"
    }
    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }
    "have a correct label for First name for UK" in {
      doc.body().getElementById("first-name-outer").getElementsByClass("form-label").text() mustBe "First name"
    }
    "have a correct label for given name for Row" in {
      docRestOfWorld
        .body()
        .getElementById("first-name-outer")
        .getElementsByClass("form-label")
        .text() mustBe "Given name"
    }
    "have an input of type 'text' for given name" in {
      doc.body().getElementById("first-name").attr("type") mustBe "text"
    }
    "have an autocomplete for given name" in {
      doc.body().getElementById("first-name").attr("autocomplete") mustBe "given-name"
    }
    "have no specheck for given name" in {
      doc.body().getElementById("first-name").attr("spellcheck") mustBe "false"
    }
    "have a correct label for Last name for UK" in {
      doc.body().getElementById("last-name-outer").getElementsByClass("form-label").text() mustBe "Last name"
    }
    "have a correct label for family name for Row" in {
      docRestOfWorld
        .body()
        .getElementById("last-name-outer")
        .getElementsByClass("form-label")
        .text() mustBe "Family name"
    }
    "have an input of type 'text' for family name" in {
      doc.body().getElementById("last-name").attr("type") mustBe "text"
    }
    "have an autocomplete for family name" in {
      doc.body().getElementById("last-name").attr("autocomplete") mustBe "family-name"
    }
    "have no specheck for family name" in {
      doc.body().getElementById("last-name").attr("spellcheck") mustBe "false"
    }
    "have an input of type 'text' for day of birth" in {
      doc.body().getElementById("date-of-birth.day").attr("type") mustBe "text"
    }
    "have an input of type 'text' for month of birth" in {
      doc.body().getElementById("date-of-birth.month").attr("type") mustBe "text"
    }
    "have an input of type 'text' for year of birth" in {
      doc.body().getElementById("date-of-birth.year").attr("type") mustBe "text"
    }
  }

  lazy val doc: Document = {
    val result = view(form, isInReviewMode, atarService, Journey.Subscribe, Some("uk"))
    Jsoup.parse(contentAsString(result))
  }

  lazy val docRestOfWorld: Document = {
    val result = view(
      form,
      isInReviewMode,
      atarService,
      Journey.Subscribe,
      selectedUserLocationWithIslands = Some("third-country")
    )
    Jsoup.parse(contentAsString(result))
  }

}
