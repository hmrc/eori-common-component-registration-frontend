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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_group
import util.ViewSpec

class VatGroupSpec extends ViewSpec {
  val form: Form[YesNo]          = vatGroupYesNoAnswerForm()
  val formWithError: Form[YesNo] = vatGroupYesNoAnswerForm().bind(Map("yes-no-answer" -> ""))
  implicit val request           = withFakeCSRF(FakeRequest())

  private val view = instanceOf[vat_group]

  "The 'Is the organisation you are registering part of a VAT group?' Page" should {

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "Is your organisation part of a VAT group in the UK?"
    }

    "include the heading in the title" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have the correct intro text" in {
      doc
        .body()
        .getElementsByClass("form-hint")
        .text mustBe "VAT groups are when two or more companies or limited liability partnerships register as one taxable 'person' for VAT purposes in the UK."
    }

    "have 'yes' radio button" in {
      doc.body().getElementById("yes-no-answer-true").attr("checked") mustBe empty
    }

    "have 'no' radio button" in {
      doc.body().getElementById("yes-no-answer-false").attr("checked") mustBe empty
    }

    "have a page level error when no radio buttons are selected" in {
      docWithErrors
        .body()
        .getElementsByClass("error-summary-list")
        .text mustBe "Tell us if your organisation is part of a VAT group in the UK"
    }

    "have a field level error when no radio buttons are selected" in {
      docWithErrors
        .body()
        .getElementsByClass("error-message")
        .text mustBe "Error: Tell us if your organisation is part of a VAT group in the UK"
    }
  }

  lazy val doc: Document           = Jsoup.parse(contentAsString(view(form, atarService, Journey.Subscribe)))
  lazy val docWithErrors: Document = Jsoup.parse(contentAsString(view(formWithError, atarService, Journey.Subscribe)))

}
