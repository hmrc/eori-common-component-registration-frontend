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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_registered_uk_kana
import util.ViewSpec

class VatRegisteredUkKanaSpec extends ViewSpec {
  val form: Form[YesNo]          = vatRegisteredUkYesNoAnswerForm(false)
  val formWithError: Form[YesNo] = vatRegisteredUkYesNoAnswerForm(false).bind(Map("yes-no-answer" -> ""))

  private val view     = instanceOf[vat_registered_uk_kana]
  implicit val request = withFakeCSRF(FakeRequest())

  lazy val doc: Document           = Jsoup.parse(contentAsString(view(form, atarService)))
  lazy val docWithErrors: Document = Jsoup.parse(contentAsString(view(formWithError, atarService)))

  "The 'Is your organisation VAT registered in the UK?' Page" should {

    "display correct title" in {
      doc.title must startWith("Is your organisation VAT registered in the UK?")
    }

    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").hasClass("govuk-fieldset__heading") mustBe true
    }

    "have 'yes' radio button" in {
      doc.body.getElementById("yes-no-answer-true").attr("checked") mustBe empty
    }

    "have 'no' radio button" in {
      doc.body.getElementById("yes-no-answer-false").attr("checked") mustBe empty
    }

    "have a page level error when no radio buttons are selected" in {
      docWithErrors.body
        .getElementsByClass("govuk-error-summary__list")
        .text mustBe "Select yes if your organisation is VAT registered in the UK"
    }

    "have a field level error when no radio buttons are selected" in {
      docWithErrors.body
        .getElementsByClass("govuk-error-message")
        .text mustBe "Error: Select yes if your organisation is VAT registered in the UK"
    }
  }
}
