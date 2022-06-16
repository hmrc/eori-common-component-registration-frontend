/*
 * Copyright 2022 HM Revenue & Customs
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

package unit.views

import common.pages.subscription.VatDetailsEuConfirmPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_details_eu_confirm
import util.ViewSpec
import util.builders.YesNoFormBuilder._

class VatDetailsEuConfirmSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[vat_details_eu_confirm]

  private val emptyFormUnderLimit: Form[YesNo] = vatRegisteredEuYesNoAnswerForm().bind(invalidRequest)
  private val formUnderLimit: Form[YesNo]      = vatRegisteredEuYesNoAnswerForm().bind(ValidRequest)

  private def removeLink(index: Int) = s"customs-registration-services/atar/register/vat-details-eu-remove/$index"

  private def reviewRemoveLink(index: Int) =
    s"customs-registration-services/atar/register/vat-details-eu-remove/$index/review"

  private def updateLink(index: Int) = s"customs-registration-services/atar/register/vat-details-eu/update/$index"

  private def reviewUpdateLink(index: Int) =
    s"customs-registration-services/atar/register/vat-details-eu/update/$index/review"

  private val VatEuDetailUnderLimit = Seq(VatEUDetailsModel("FR", "12345"))

  private val VatEuDetailsOnLimit = VatEuDetailUnderLimit ++ Seq(
    VatEUDetailsModel("CZ", "12345"),
    VatEUDetailsModel("ES", "12345"),
    VatEUDetailsModel("DK", "12345"),
    VatEUDetailsModel("DE", "12345")
  )

  "Vat Details EU Page" should {
    "display correct title for single vat euDetails" in {
      docUnderLimit.title must startWith(VatDetailsEuConfirmPage(VatEuDetailUnderLimit.size.toString).title)
    }

    "display correct title for multiple vat euDetails" in {
      docOnLimit.title must startWith(VatDetailsEuConfirmPage(VatEuDetailsOnLimit.size.toString).title)
    }

    "should radio buttons" in {
      docUnderLimit.body.getElementById("yes-no-answer-true").attr("value") mustBe "true"
      docUnderLimit.body.getElementById("yes-no-answer-false").attr("value") mustBe "false"
    }

    "have the correct class on the h1" in {
      docUnderLimit.body().getElementsByTag("h1").attr("class") mustBe "govuk-fieldset__heading"
    }

    "have the correct text on the h1 for 1 vat details provided" in {
      docUnderLimit.body.getElementsByTag("h1").text must include(
        VatDetailsEuConfirmPage(VatEuDetailUnderLimit.size.toString).title
      )
      docUnderLimit.body.getElementsByTag("h1").text must include
      "Do you need to add VAT details for another EU member country?"
    }

    "have the correct text on the h1 for multiple vat details provided" in {
      docOnLimit.body.getElementsByTag("h1").text must include(
        VatDetailsEuConfirmPage(VatEuDetailsOnLimit.size.toString).title
      )
      docOnLimit.body
        .getElementsByTag("h1")
        .text must not include "Do you need to add VAT details for another EU member country?"
    }

    "have vat details and links for remove and edit for each of them" in {
      VatEuDetailsOnLimit.zipWithIndex.foreach {
        case (details: VatEUDetailsModel, index: Int) =>
          docOnLimit.body.getElementsByClass("govuk-summary-list__value").get(index).text mustBe details.vatNumber
          docOnLimit.body.getElementsByClass("govuk-summary-list__key").get(index).text mustBe Messages(
            s"cds.country.${details.vatCountry}"
          )
          docOnLimit.body.getElementsByClass("govuk-summary-list__actions-list").get(index)
            .getElementsByClass("govuk-link").get(0)
            .attr("href") must endWith(updateLink(details.index))
          docOnLimit.body.getElementsByClass("govuk-summary-list__actions-list").get(index)
            .getElementsByClass("govuk-link").get(1)
            .attr("href") must endWith(removeLink(details.index))
      }
    }

    "have message saying that vat detail limit was reached" in {
      docOnLimit.body.getElementById("limit-reached").text mustBe "You have added the maximum number of VAT details"
    }

    "have flag isInReviewMode set to true for remove and update links" in {
      VatEuDetailsOnLimit.zipWithIndex.foreach {
        case (details: VatEUDetailsModel, index: Int) =>
          docOnLimitInReview.body.getElementsByClass("govuk-summary-list__value").get(
            index
          ).text mustBe details.vatNumber
          docOnLimitInReview.body.getElementsByClass("govuk-summary-list__key").get(index).text mustBe Messages(
            s"cds.country.${details.vatCountry}"
          )
          docOnLimitInReview.body.getElementsByClass("govuk-summary-list__actions-list").get(index)
            .getElementsByClass("govuk-link").get(0)
            .attr("href") must endWith(reviewUpdateLink(details.index))
          docOnLimitInReview.body.getElementsByClass("govuk-summary-list__actions-list").get(index)
            .getElementsByClass("govuk-link").get(1)
            .attr("href") must endWith(reviewRemoveLink(details.index))
      }
    }

    "display error for document with no option selected" in {
      emptyDocUnderLimit.body
        .getElementsByClass("govuk-error-summary__list")
        .text mustBe "Tell us if your organisation is VAT registered in other EU countries"
      emptyDocUnderLimit.body
        .getElementsByClass("govuk-error-message")
        .text mustBe "Error: Tell us if your organisation is VAT registered in other EU countries"
    }
  }

  private lazy val emptyDocUnderLimit: Document = Jsoup.parse(
    contentAsString(
      view(emptyFormUnderLimit, isInReviewMode = false, VatEuDetailUnderLimit, atarService, vatLimitNotReached = true)
    )
  )

  private lazy val docUnderLimit: Document = Jsoup.parse(
    contentAsString(
      view(formUnderLimit, isInReviewMode = false, VatEuDetailUnderLimit, atarService, vatLimitNotReached = true)
    )
  )

  private lazy val docOnLimit: Document = Jsoup.parse(
    contentAsString(
      view(formUnderLimit, isInReviewMode = false, VatEuDetailsOnLimit, atarService, vatLimitNotReached = false)
    )
  )

  private lazy val docOnLimitInReview: Document = Jsoup.parse(
    contentAsString(
      view(formUnderLimit, isInReviewMode = true, VatEuDetailsOnLimit, atarService, vatLimitNotReached = false)
    )
  )

}
