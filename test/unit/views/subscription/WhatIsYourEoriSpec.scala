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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.EoriNumberViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.what_is_your_eori
import util.ViewSpec

class WhatIsYourEoriSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[what_is_your_eori]

  private def doc(form: Form[EoriNumberViewModel] = SubscriptionForm.eoriNumberForm): Document = {
    val result = view(form, isInReviewMode = false, isRestOfWorldJourney = false, atarService)
    Jsoup.parse(contentAsString(result))
  }

  "What Is Your EORI page" should {

    "display correct title" in {

      doc().title must startWith("What is your GB Economic Operator Registration and Identification (EORI) number?")
    }

    "have the correct heading text" in {

      doc().body.getElementsByClass(
        "heading-large"
      ).text() mustBe "What is your GB Economic Operator Registration and Identification (EORI) number?"
    }

    "have the correct text in the label" in {

      doc().body
        .getElementById("eori-number-hint")
        .text() mustBe "The number starts with GB and is then followed by 12 digits, For example, GB345834921000."
    }

    "have an input of type 'text'" in {

      doc().body.getElementById("eori-number").attr("type") mustBe "text"
    }

    "have a link to 'Get EORI'" in {

      doc().body.getElementsByAttributeValue(
        "href",
        routes.ApplicationController.startRegister(atarService).url
      ).text() mustBe "get an EORI number"
    }

    "display a field level error message" when {

      "EORI field is empty" in {

        val formWithEmptyFieldError: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> ""))

        doc(formWithEmptyFieldError).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: Enter your EORI number"
      }

      "EORI field has only numbers and less than 12 digits" in {

        val formWithTooShortNumberEori: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> "123456"))

        doc(formWithTooShortNumberEori).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: The EORI number must be more than 11 digits"
      }

      "GB EORI has less than 14 characters" in {

        val formWithTooShortEori: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> "GB123456"))

        doc(formWithTooShortEori).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: The EORI number must be more than 13 characters"
      }

      "EORI field has only numbers and more than 15 digits" in {

        val formWithTooShortEori: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> "1234567890123456"))

        doc(formWithTooShortEori).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: The EORI number must be 15 digits or less"
      }

      "GB EORI has more than 17 characters" in {

        val formWithTooShortEori: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> "GB1234567890123456"))

        doc(formWithTooShortEori).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: The EORI number must be 17 characters or less"
      }

      "EORI Starts with letters and two first letters are not GB" in {

        val formWithTooShortEori: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> "FR123456789012"))

        doc(formWithTooShortEori).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: Enter an EORI number that starts with GB"
      }

      "EORI has letters in the middle" in {

        val formWithTooShortEori: Form[EoriNumberViewModel] =
          SubscriptionForm.eoriNumberForm.bind(Map("eori-number" -> "GB123asd789012"))

        doc(formWithTooShortEori).body
          .getElementById("eori-number-outer")
          .getElementsByClass("error-message")
          .text mustBe "Error: Enter an EORI number in the right format"
      }
    }
  }
}
