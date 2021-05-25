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

import common.pages.VatDetailsEuPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm.euVatForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_details_eu
import util.ViewSpec

class VatDetailsEuSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[vat_details_eu]

  val form: Form[VatEUDetailsModel]          = euVatForm
  val updateValues                           = Map("vatNumber" -> "2DSFA311", "vatCountry" -> "DK")
  val formForUpdate: Form[VatEUDetailsModel] = euVatForm.bind(updateValues)

  val formWithNoSelectionError: Form[VatEUDetailsModel] = euVatForm.bind(Map("vatNumber" -> "", "vatCountry" -> ""))

  val formWithIncorrectVatNumber: Form[VatEUDetailsModel] =
    euVatForm.bind(Map("vatNumber" -> "1234/23-1b", "vatCountry" -> "FR"))

  val formWithLongVatDetails: Form[VatEUDetailsModel] =
    euVatForm.bind(Map("vatNumber" -> "1123134234234231", "vatCountry" -> "SADF"))

  val isInReviewMode        = false
  val vatEuDetailsForUpdate = Some(VatEUDetailsModel("12345", "DE"))

  "Vat Details EU Page" should {
    "display correct title" in {
      doc.title must startWith(VatDetailsEuPage.title)
    }

    "should display correct inputs and labels" in {
      doc.body.getElementById("vatCountry").hasText mustBe true
      doc.body.getElementById("vatNumber").hasText mustBe false

      doc.body.getElementById("country-outer").text must include("Country")
      //the full stop adds a pause after the label is read out by screenreaders
      doc.body.getElementById("country-outer").text must include(
        ". Start to type the name of the country and then use up and down arrows to review and enter to select a country"
      )
      doc.body.getElementById("vatNumber-outer").text mustBe "VAT Number"
    }

    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1") hasClass "heading-large"
    }

    "have the correct text on the h1" in {
      doc.body.getElementsByTag("h1").text must be(VatDetailsEuPage.title)
    }

    "have inputs for vatNumber and vatCountry" in {
      doc.body.getElementById("vatNumber").attr("type") mustBe "text"
      doc.body.getElementById("vatCountry") hasClass "autocomplete__input"
    }

    "have input filled with values when in edit mode for vatNumber" in {
      docForEdit.body.getElementById("vatNumber").attr("value") mustBe updateValues.get("vatNumber").get
    }
    "display error for document with empty values" in {
      docWithEmptyErrors.body.getElementsByClass("error-summary-list").text must include("Enter your VAT number")
      docWithEmptyErrors.body.getElementsByClass("error-summary-list").text must include(
        "Enter a country in the EU other than the UK"
      )
    }

    "display errors for incorrect formats for field inputs" in {
      vatNumErrorIllegalCharacters.body.getElementsByClass("error-list").text must include(
        "Enter a VAT Number without invalid characters"
      )
      vatNumErrorIllegalCharacters.body.getElementsByClass("error-message").text must include(
        "Enter a VAT Number without invalid characters"
      )

      vatNumErrorLong.body.getElementsByClass("error-list").text must include(
        "The VAT Number must be 15 characters or less"
      )
      vatNumErrorLong.body.getElementsByClass("error-list").text must include(
        "Enter a country in the EU other than the UK"
      )
      vatNumErrorLong.body.getElementsByClass("error-message").text must include(
        "The VAT Number must be 15 characters or less"
      )
    }
  }

  private lazy val doc: Document =
    Jsoup.parse(
      contentAsString(view(form, Countries.eu, updateDetails = false, atarService, Journey.Register, isInReviewMode))
    )

  private lazy val docForEdit: Document = Jsoup.parse(
    contentAsString(
      view(formForUpdate, Countries.eu, updateDetails = true, atarService, Journey.Register, isInReviewMode = false)
    )
  )

  private lazy val docWithEmptyErrors: Document = Jsoup.parse(
    contentAsString(
      view(formWithNoSelectionError, Countries.eu, updateDetails = false, atarService, Journey.Register, isInReviewMode)
    )
  )

  private lazy val vatNumErrorIllegalCharacters: Document = Jsoup.parse(
    contentAsString(
      view(
        formWithIncorrectVatNumber,
        Countries.eu,
        updateDetails = false,
        atarService,
        Journey.Register,
        isInReviewMode
      )
    )
  )

  private lazy val vatNumErrorLong: Document = Jsoup.parse(
    contentAsString(
      view(formWithLongVatDetails, Countries.eu, updateDetails = false, atarService, Journey.Register, isInReviewMode)
    )
  )

}
