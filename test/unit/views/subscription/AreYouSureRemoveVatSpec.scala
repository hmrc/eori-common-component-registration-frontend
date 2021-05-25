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

import common.pages.RemoveVatDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.removeVatYesNoAnswer
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.are_you_sure_remove_vat
import util.ViewSpec

class AreYouSureRemoveVatSpec extends ViewSpec {
  private val form: Form[YesNo]             = removeVatYesNoAnswer
  private val formWithError: Form[YesNo]    = removeVatYesNoAnswer.bind(Map("yes-no-answer" -> ""))
  private val vatDetails: VatEUDetailsModel = VatEUDetailsModel("FR", "12345")
  implicit val request                      = withFakeCSRF(FakeRequest())

  private val view = instanceOf[are_you_sure_remove_vat]

  "Are you Vat Registered in EU Page" should {

    "display correct title" in {
      doc.title must startWith(RemoveVatDetails.title)
    }

    "should display correct values for vat details" in {
      doc.body.getElementById("vatCountry").text mustBe "France"
      doc.body.getElementById("vatNumber").text mustBe "12345"
    }

    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1") hasClass "heading-large"
    }

    "have the correct text on the h1" in {
      doc.body.getElementsByTag("h1").text must be(RemoveVatDetails.title)
    }

    "have 'yes' radio button" in {
      doc.body.getElementById("yes-no-answer-true").attr("checked") mustBe empty
    }

    "have 'no' radio button" in {
      doc.body.getElementById("yes-no-answer-false").attr("checked") mustBe empty
    }

    "have a page level error when no radio buttons are selected" in {
      docWithErrors.body.getElementsByClass("error-summary-list").text mustBe RemoveVatDetails.pageLevelErrorMessage
    }
  }

  private lazy val doc: Document =
    Jsoup.parse(contentAsString(view(form, atarService, Journey.Subscribe, vatDetails, isInReviewMode = false)))

  private lazy val docWithErrors: Document =
    Jsoup.parse(
      contentAsString(view(formWithError, atarService, Journey.Subscribe, vatDetails, isInReviewMode = false))
    )

}
