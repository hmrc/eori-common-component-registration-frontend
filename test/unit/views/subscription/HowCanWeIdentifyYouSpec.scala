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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NinoOrUtrChoice
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you
import util.ViewSpec

class HowCanWeIdentifyYouSpec extends ViewSpec {
  val form: Form[NinoOrUtrChoice]                    = ninoOrUtrChoiceForm
  val formWithNothingSelected: Form[NinoOrUtrChoice] = ninoOrUtrChoiceForm.bind(Map("ninoOrUtrRadio" -> ""))

  val isInReviewMode   = false
  val previousPageUrl  = "/"
  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[how_can_we_identify_you]

  "How Can We Identify You Page" should {

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "What information can we use to confirm your identity?"
    }

    "include the heading in the title" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have nino displayed but not selected" in {
      doc.body().getElementById("ninoOrUtrRadio-nino").attr("checked") mustBe empty
    }

    "have utr displayed but not selected" in {
      doc.body().getElementById("ninoOrUtrRadio-utr").attr("checked") mustBe empty
    }

    "have an input of type 'radio' for nino" in {
      doc.body().getElementById("ninoOrUtrRadio-nino").attr("type") mustBe "radio"
    }

    "have an input of type 'radio' for utr" in {
      doc.body().getElementById("ninoOrUtrRadio-utr").attr("type") mustBe "radio"
    }

    "display an page level error if no radio button is selected" in {
      docWithRadioButtonsError
        .body()
        .getElementsByClass("error-summary-list")
        .text() mustBe "Select how we can identify you"
    }

    "display an field level error if no radio button is selected" in {
      docWithRadioButtonsError
        .body()
        .getElementsByClass("error-message")
        .text() mustBe "Error: Select how we can identify you"
    }
  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view(form, isInReviewMode, atarService, Journey.Subscribe)))

  lazy val docWithRadioButtonsError: Document =
    Jsoup.parse(contentAsString(view(formWithNothingSelected, isInReviewMode, atarService, Journey.Subscribe)))

}
