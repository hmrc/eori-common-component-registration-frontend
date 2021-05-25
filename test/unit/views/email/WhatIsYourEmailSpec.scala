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

package unit.views.email

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.{EmailForm, EmailViewModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.what_is_your_email
import util.ViewSpec

class WhatIsYourEmailSpec extends ViewSpec {
  val form: Form[EmailViewModel]          = EmailForm.emailForm
  val formWithError: Form[EmailViewModel] = EmailForm.emailForm.bind(Map("email" -> "invalid"))
  val previousPageUrl                     = "/"
  implicit val request                    = withFakeCSRF(FakeRequest())

  val view = instanceOf[what_is_your_email]

  "What Is Your Email Address page for CDS access" should {
    "display correct title" in {
      MigrateDoc.title() must startWith("What is your email address?")
    }
    "have the correct h1 text" in {
      MigrateDoc.body().getElementsByClass("heading-large").text() mustBe "What is your email address?"
    }
    "have the correct hint text" in {
      MigrateDoc.body().getElementById(
        "email-hint"
      ).text() mustBe "We will use this to send you the result of your application."
    }
    "have an input of type 'email'" in {
      MigrateDoc.body().getElementById("email").attr("type") mustBe "email"
    }
    "have an autocomplet of type 'email'" in {
      MigrateDoc.body().getElementById("email").attr("autocomplete") mustBe "email"
    }
    "associate hint with input field" in {
      MigrateDoc.body().getElementById("email").attr("aria-describedby") mustBe "email-hint"
    }
  }
  "What Is Your Email Address page with errors" should {
    "display a field level error message" in {
      docWithErrors
        .body()
        .getElementById("email-outer")
        .getElementsByClass("error-message")
        .text() mustBe "Error: Enter a valid email address"
    }

    "associate error with input field" in {
      docWithErrors
        .body()
        .getElementById("email")
        .attr("aria-describedby") mustBe "email-hint email-error"
    }
  }

  lazy val MigrateDoc: Document = {
    val result = view(form, atarService, Journey.Subscribe)
    Jsoup.parse(contentAsString(result))
  }

  lazy val GYEDoc: Document = {
    val result = view(form, atarService, Journey.Register)
    Jsoup.parse(contentAsString(result))
  }

  lazy val docWithErrors: Document = {
    val result = view(formWithError, atarService, Journey.Subscribe)
    Jsoup.parse(contentAsString(result))
  }

}
