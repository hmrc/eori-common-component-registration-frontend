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

package unit.views.migration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.nameOrg
import util.ViewSpec

class NameOrganisationSpec extends ViewSpec {
  val form: Form[NameOrganisationMatchModel]          = nameOrganisationForm
  val formWithError: Form[NameOrganisationMatchModel] = nameOrganisationForm.bind(Map("name" -> ""))
  val isInReviewMode                                  = false
  val previousPageUrl                                 = "/"
  val organisationType                                = "charity-public-body-not-for-profit"
  implicit val request                                = withFakeCSRF(FakeRequest())

  private val view = instanceOf[nameOrg]

  "Match Organisation Name page" should {
    "display correct title" in {
      doc.title() must startWith("What is your registered organisation name?")
    }
    "have the correct h1 text" in {
      doc.body().getElementsByTag("h1").text() mustBe "What is your registered organisation name?"
    }
    "have an input of type 'text' for organisation name" in {
      doc.body().getElementById("name").attr("type") mustBe "text"
    }
  }
  "Match Organisation Name page with errors" should {
    "display a field level error message" in {
      docWithErrors
        .body()
        .getElementById("name-outer")
        .getElementsByClass("error-message")
        .text() mustBe "Error: Enter your registered organisation name"
    }
    "display a page level error message" in {
      docWithErrors
        .body()
        .getElementsByClass("error-summary-list")
        .text() mustBe "Enter your registered organisation name"
    }
  }

  lazy val doc: Document = getDoc(form)

  private def getDoc(form: Form[NameOrganisationMatchModel]) = {
    val result = view(form, RegistrationDetailsOrganisation(), false, atarService, Journey.Subscribe)
    val doc    = Jsoup.parse(contentAsString(result))
    doc
  }

  lazy val docWithErrors: Document = {
    val result = view(formWithError, RegistrationDetailsOrganisation(), false, atarService, Journey.Subscribe)
    Jsoup.parse(contentAsString(result))
  }

}
