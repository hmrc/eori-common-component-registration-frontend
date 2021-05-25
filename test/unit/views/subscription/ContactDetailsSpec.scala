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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.ContactDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.contact_details
import util.ViewSpec

class ContactDetailsSpec extends ViewSpec {

  private val view = instanceOf[contact_details]

  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private val email = "john.doe@example.com"
  private val form  = ContactDetailsForm.form()

  private val formWithError = form.bind(Map("full-name" -> "", "telephone" -> ""))

  private val doc: Document = Jsoup.parse(contentAsString(view(form, email, false, atarService)))

  private val docWithErrorSummary: Document =
    Jsoup.parse(contentAsString(view(formWithError, email, false, atarService)))

  "Contact details view" should {

    "display correct title" in {

      doc.title() must startWith("Who can we contact?")
    }

    "display correct header" in {

      doc.body().getElementsByTag("h1").text() mustBe "Who can we contact?"
    }

    "display email" in {

      val emailDiv = doc.body().getElementById("email-outer")

      emailDiv.getElementsByTag("label").get(0).text() must startWith("Email address")
      emailDiv.getElementById("email").text() mustBe email
    }

    "display full name input" in {

      val fullNameDiv = doc.body().getElementById("full-name-outer")

      fullNameDiv.getElementsByTag("label").get(0).text() must startWith("Full name")
    }

    "display telephone input" in {

      val telephoneDiv = doc.body().getElementById("telephone-outer")

      telephoneDiv.getElementsByTag("label").get(0).text() must startWith("Telephone")
      telephoneDiv.getElementById("telephone-hint").text() mustBe "Only enter numbers, for example 01632 960 001"
    }

    "display continue button" in {

      val continueButton = doc.body().getElementById("continue-button")

      continueButton.attr("value") mustBe "Continue"
    }

    "display error summary" in {

      docWithErrorSummary.getElementById("form-error-heading").text() mustBe "There is a problem"
      docWithErrorSummary.getElementsByClass("error-list").get(0).text() mustBe "Enter your contact name"
      docWithErrorSummary.getElementsByClass("error-list").get(1).text() mustBe "Enter your contact telephone number"
    }
  }
}
