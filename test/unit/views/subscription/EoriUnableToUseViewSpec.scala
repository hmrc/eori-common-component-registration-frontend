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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.EoriUnableToUse
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_unable_to_use
import util.ViewSpec

class EoriUnableToUseViewSpec extends ViewSpec {

  private val view                   = instanceOf[eori_unable_to_use]
  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val form = EoriUnableToUse.form()

  private val formWithError = form.bind(Map("answer" -> "incorrect"))

  private val doc: Document = Jsoup.parse(contentAsString(view(atarService, "GB123456789123", form)))

  private val docWithErrorSummary: Document =
    Jsoup.parse(contentAsString(view(atarService, "GB123456789123", formWithError)))

  "Eori unable to use page" should {

    "display correct title" in {

      doc.title() must startWith("Your Government Gateway account is not linked to this EORI")
    }

    "display correct header" in {

      doc.body().getElementsByTag("h1").text() mustBe "Your Government Gateway account is not linked to this EORI"
    }

    "display eori" in {

      doc.body().getElementById("eori-number").text() mustBe "GB123456789123"
    }

    "display question with two radio buttons" in {

      val questionElement = doc.body().getElementById("answer-fieldset")

      questionElement.getElementsByClass("heading-medium").text() mustBe "What would you like to do?"

      questionElement.getElementsByTag("label").get(0).text() mustBe "Change the EORI number"
      questionElement.getElementsByTag("label").get(
        1
      ).text() mustBe "Sign out and sign in again with a different Government Gateway ID"
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
