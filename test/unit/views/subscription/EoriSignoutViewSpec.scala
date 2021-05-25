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
import play.api.mvc.Request
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.eoriSignoutYesNoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_signout
import util.ViewSpec

class EoriSignoutViewSpec extends ViewSpec {

  private val view                   = instanceOf[eori_signout]
  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val form = eoriSignoutYesNoForm()

  private val formWithError = form.bind(Map("yes-no-answer" -> "incorrect"))

  private val doc: Document = Jsoup.parse(contentAsString(view(atarService, form)))

  private val docWithErrorSummary: Document = Jsoup.parse(contentAsString(view(atarService, formWithError)))

  "Eori signout page" should {

    "display correct title" in {

      doc.title() must startWith("You are about to sign out")
    }

    "display correct header" in {

      doc.body().getElementsByTag("h1").text() mustBe "You are about to sign out"
    }

    "display question with two radio buttons" in {

      val questionElement = doc.body().getElementById("yes-no-answer-fieldset")

      questionElement.getElementsByClass("heading-medium").text() mustBe "Is this what you want to do?"

      questionElement.getElementsByTag("label").get(0).text() mustBe "Yes"
      questionElement.getElementsByTag("label").get(1).text() mustBe "No"
    }

    "display continue button" in {

      val continueButton = doc.body().getElementById("continue-button")

      continueButton.attr("value") mustBe "Continue"
    }

    "display error summary" in {

      docWithErrorSummary.getElementById("form-error-heading").text() mustBe "There is a problem"
      docWithErrorSummary.getElementsByClass("error-list").get(0).text() mustBe "Select what you would like to do"
    }
  }

}
