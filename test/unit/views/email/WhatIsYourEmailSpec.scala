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

package unit.views.email

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.{EmailForm, EmailViewModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.what_is_your_email
import util.ViewSpec

class WhatIsYourEmailSpec extends ViewSpec {
  val form: Form[EmailViewModel]          = EmailForm.emailForm
  val formWithError: Form[EmailViewModel] = EmailForm.emailForm.bind(Map("email" -> "invalid"))
  val previousPageUrl                     = "/"
  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  val view: what_is_your_email = instanceOf[what_is_your_email]

  "What Is Your Email Address page for CDS access" should {
    "display correct title" in {
      GYEDoc.title() must startWith("What is your email address?")
    }
    "have the correct h1 text" in {
      GYEDoc
        .body()
        .getElementsByClass("govuk-label govuk-label--l")
        .attr("for", "email")
        .text() mustBe "What is your email address?"
    }
    "have the correct hint text" in {
      GYEDoc.body().getElementById(
        "email-hint"
      ).text() mustBe "We will use this to send you the result of your application."
    }
    "have an input of type 'email'" in {
      GYEDoc.body().getElementById("email").attr("type") mustBe "email"
    }
    "have an autocomplet of type 'email'" in {
      GYEDoc.body().getElementById("email").attr("autocomplete") mustBe "email"
    }
    "associate hint with input field" in {
      GYEDoc.body().getElementById("email").attr("aria-describedby") mustBe "email-hint"
    }
  }
  "What Is Your Email Address page with errors" should {
    "display a field level error message" in {
      docWithErrors
        .body()
        .getElementById("email-error")
        .getElementsByClass("govuk-error-message")
        .text() mustBe s"Error: ${messages("cds.subscription.contact-details.form-error.email.wrong-format")}"
    }

    "associate error with input field" in {
      docWithErrors
        .body()
        .getElementById("email")
        .attr("aria-describedby") mustBe "email-hint email-error"
    }
  }

  lazy val GYEDoc: Document = {
    val result = view(form, atarService)
    Jsoup.parse(contentAsString(result))
  }

  lazy val docWithErrors: Document = {
    val result = view(formWithError, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
