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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_registered_eu
import util.ViewSpec

class VatRegisteredEuSpec extends ViewSpec {
  val form: Form[YesNo]                     = vatRegisteredEuYesNoAnswerForm()
  val formWithError: Form[YesNo]            = vatRegisteredEuYesNoAnswerForm().bind(Map("yes-no-answer" -> ""))
  val partnershipForm: Form[YesNo]          = vatRegisteredEuYesNoAnswerForm(true)
  val partnershipFormWithError: Form[YesNo] = vatRegisteredEuYesNoAnswerForm(true).bind(Map("yes-no-answer" -> ""))
  implicit val request                      = withFakeCSRF(FakeRequest())

  private val view = instanceOf[vat_registered_eu]

  "Are you Vat Registered in EU Page" should {

    "display correct title" in {
      individualDoc.title must startWith("Are you VAT registered in other EU member countries?")
    }

    "have the correct class on the h1" in {
      individualDoc.body.getElementsByTag("h1") hasClass "heading-large"
    }

    "have the correct text in the h1" in {
      individualDoc.body.getElementsByTag("h1").text must be("Are you VAT registered in other EU member countries?")
    }

    "have 'yes' radio button" in {
      individualDoc.body.getElementById("yes-no-answer-true").attr("checked") mustBe empty
    }

    "have 'no' radio button" in {
      individualDoc.body.getElementById("yes-no-answer-false").attr("checked") mustBe empty
    }

    "have a page level error when no radio buttons are selected" in {
      docWithErrors.body
        .getElementsByClass("error-summary-list")
        .text mustBe "Tell us if your organisation is VAT registered in other EU countries"
    }

    "have standard submit button" in {
      individualDoc.body.getElementsByAttributeValue("type", "submit").attr("value") mustBe "Continue"
    }

    "have different title for nonIndividuals" in {
      nonIndividualDocInReview.title must startWith("Is your organisation VAT registered in other EU member countries?")
    }

    "have different heading for nonIndividuals" in {
      nonIndividualDocInReview.body.getElementsByTag("h1").text must be(
        "Is your organisation VAT registered in other EU member countries?"
      )
    }

    "have different submit button when in review mode" in {
      nonIndividualDocInReview.body.getElementsByAttributeValue("type", "submit").attr("value") mustBe "Save and review"
    }

    "have the correct title for a partnership" in {
      partnershipDoc.title must startWith("Is your partnership VAT registered in other EU member countries?")
    }

    "have the correct text for a partnership in the h1" in {
      partnershipDoc.body.getElementsByTag("h1").text must be(
        "Is your partnership VAT registered in other EU member countries?"
      )
    }

    "have the correct text in the error message when no radio buttons are selected and the organisation type is a partnership" in {
      partnershipDocWithErrors.body
        .getElementsByClass("error-summary-list")
        .text mustBe "Tell us if your partnership is VAT registered in other EU countries"
    }

  }

  private lazy val individualDoc: Document = Jsoup.parse(
    contentAsString(
      view(
        isInReviewMode = false,
        form,
        isIndividualSubscriptionFlow = true,
        isPartnership = false,
        atarService,
        Journey.Subscribe
      )
    )
  )

  private lazy val nonIndividualDocInReview: Document = Jsoup.parse(
    contentAsString(
      view(
        isInReviewMode = true,
        form,
        isIndividualSubscriptionFlow = false,
        isPartnership = false,
        atarService,
        Journey.Subscribe
      )
    )
  )

  private lazy val docWithErrors: Document = Jsoup.parse(
    contentAsString(
      view(
        isInReviewMode = true,
        formWithError,
        isIndividualSubscriptionFlow = true,
        isPartnership = false,
        atarService,
        Journey.Subscribe
      )
    )
  )

  private lazy val partnershipDoc: Document = Jsoup.parse(
    contentAsString(
      view(
        isInReviewMode = false,
        partnershipForm,
        isIndividualSubscriptionFlow = false,
        isPartnership = true,
        atarService,
        Journey.Subscribe
      )
    )
  )

  private lazy val partnershipDocWithErrors: Document = Jsoup.parse(
    contentAsString(
      view(
        isInReviewMode = false,
        partnershipFormWithError,
        isIndividualSubscriptionFlow = false,
        isPartnership = true,
        atarService,
        Journey.Subscribe
      )
    )
  )

}
