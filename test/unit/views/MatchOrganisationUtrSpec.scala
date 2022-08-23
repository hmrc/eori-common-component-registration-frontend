/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.UtrMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_organisation_utr
import util.ViewSpec

class MatchOrganisationUtrSpec extends ViewSpec {
  val form: Form[UtrMatchModel]                     = haveUtrForm
  val formWithNoSelectionError: Form[UtrMatchModel] = haveUtrForm.bind(Map.empty[String, String])
  val isInReviewMode                                = false
  val previousPageUrl                               = "/"
  val nonSoleTraderType                             = "charity-public-body-not-for-profit"
  val soleTraderType                                = "sole-trader"
  implicit val request                              = withFakeCSRF(FakeRequest())

  private val view = instanceOf[match_organisation_utr]

  "Match UTR page in the non sole trader case" should {
    "display correct title" in {
      doc.title must startWith("Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) number?")
    }
    "have the correct h1 text" in {
      doc.body
        .getElementsByTag("h1")
        .text() mustBe "Does your organisation have a Corporation Tax Unique Taxpayer Reference (UTR) number?"
    }
    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").hasClass("govuk-fieldset__heading") mustBe true
    }
    "have an input of type 'radio' for Yes I have a UTR" in {
      doc.body.getElementById("have-utr-true").attr("type") mustBe "radio"
    }
    "have an input of type 'radio' for No I don't have a UTR" in {
      doc.body.getElementById("have-utr-false").attr("type") mustBe "radio"
    }
    "display correct intro paragraph" in {
      doc.body
        .getElementById("have-utr-hint")
        .text() mustBe "This is 10 numbers, for example 1234567890, which may be followed by a K. It will be on tax returns and other letters about Corporation Tax. It may be called reference, UTR or official use. You can find a lost UTR number ."
    }
    "have aria-described-by on the fieldset" in {
      doc.body
        .getElementsByClass("govuk-fieldset")
        .attr("aria-describedby") mustBe "have-utr-hint"

    }
  }

  "Match UTR page in the sole trader case" should {
    "have the correct h1 text" in {
      docAsSoleTraderIndividual.body
        .getElementsByTag("h1")
        .text mustBe "Do you have a Self Assessment Unique Taxpayer Reference (UTR) issued in the UK?"
    }
    "not show the link for corporation tax UTR number, for sole traders" in {
      docAsSoleTraderIndividual.body.getElementsByTag("summary").text mustBe ""
    }

    "not have any content for sole trader" in {
      docAsSoleTraderIndividual.body.getElementById("details-content-1") mustBe null
    }

  }

  "Match UTR page without selecting any radio button in the non sole trader case" should {
    "display a field level error message" in {
      docWithNoSelectionError.body
        .getElementById("have-utr-error")
        .getElementsByClass("govuk-error-message")
        .text mustBe "Error: Select yes if you have a UTR number"
    }
    "display a page level error message" in {
      docWithNoSelectionError.body
        .getElementsByClass("govuk-error-summary__list")
        .text mustBe "Select yes if you have a UTR number"
    }
    "display the correct problem message at the top of the page" in {
      docWithNoSelectionError.body
        .getElementsByClass("govuk-list govuk-error-summary__list")
        .get(0)
        .getElementsByAttributeValue("href", "#have-utr-true")
        .text mustBe "Select yes if you have a UTR number"
    }
  }

  "Match Organisation UTR page without selecting any radio button in the sole trader case" should {
    "display a field level error message" in {
      docWithNoSelectionErrorAsSoleTrader.body
        .getElementById("have-utr-error")
        .getElementsByClass("govuk-error-message")
        .text mustBe "Error: Select yes if you have a UTR number"
    }
    "display a page level error message" in {
      docWithNoSelectionErrorAsSoleTrader.body
        .getElementsByClass("govuk-error-summary__list")
        .text mustBe "Select yes if you have a UTR number"
    }
    "display the correct problem message at the top of the page" in {
      docWithNoSelectionErrorAsSoleTrader.body
        .getElementById("error-summary-title")
        .text mustBe "There is a problem"
      docWithNoSelectionErrorAsSoleTrader.body
        .getElementsByClass("govuk-list govuk-error-summary__list")
        .get(0)
        .getElementsByAttributeValue("href", "#have-utr-true")
        .text mustBe "Select yes if you have a UTR number"
    }
  }

  lazy val doc: Document = getDoc(form)

  private def getDoc(form: Form[UtrMatchModel]) = {
    val result = view(form, nonSoleTraderType, "", atarService)
    val doc    = Jsoup.parse(contentAsString(result))
    doc
  }

  lazy val docWithNoSelectionError: Document = {
    val result = view(formWithNoSelectionError, nonSoleTraderType, "", atarService)
    Jsoup.parse(contentAsString(result))
  }

  lazy val docAsSoleTraderIndividual: Document = {
    val result = view(form, soleTraderType, "", atarService)
    Jsoup.parse(contentAsString(result))
  }

  lazy val docWithNoSelectionErrorAsSoleTrader: Document = {
    val result = view(formWithNoSelectionError, soleTraderType, "", atarService)
    Jsoup.parse(contentAsString(result))
  }

}
