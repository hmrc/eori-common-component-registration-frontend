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
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_org_name
import util.ViewSpec

class MatchOrganisationNameSpec extends ViewSpec {
  val form: Form[NameMatchModel]                        = organisationNameForm
  val formWithError: Form[NameMatchModel]               = organisationNameForm.bind(Map("name" -> ""))
  val isInReviewMode                                    = false
  val previousPageUrl                                   = "/"
  val organisationType                                  = "charity-public-body-not-for-profit"
  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  private val view = instanceOf[what_is_your_org_name]

  "Match Organisation Name page" should {
    "include the heading in the title" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }
    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "What is the name of your organisation?"
    }
    "have the correct h1 text" in {
      doc.body().getElementsByTag("h1").text() mustBe "What is the name of your organisation?"
    }
    "have an input of type 'text' for organisation name" in {
      doc.body().getElementById("name").attr("type") mustBe "text"
    }
  }
  "Match Organisation Name page with errors" should {
    "display a field level error message" in {
      docWithErrors
        .body()
        .getElementById("name-error")
        .getElementsByClass("govuk-error-message")
        .text() mustBe "Error: Enter your registered organisation name"
    }
    "diplay a page level error message" in {
      docWithErrors
        .body()
        .getElementsByClass("govuk-error-summary__list")
        .text() mustBe "Enter your registered organisation name"
    }
  }

  lazy val doc: Document = getDoc(form)

  private def getDoc(form: Form[NameMatchModel]) = {
    val result = view(isInReviewMode = false, form, organisationType, atarService)
    val doc    = Jsoup.parse(contentAsString(result))
    doc
  }

  lazy val docWithErrors: Document = {
    val result = view(isInReviewMode = false, formWithError, organisationType, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
