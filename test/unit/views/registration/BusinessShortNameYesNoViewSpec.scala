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

package unit.views.registration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Request
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CorporateBody,
  EtmpOrganisationType,
  Partnership,
  UnincorporatedBody,
  YesNo
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_short_name_yes_no
import util.ViewSpec

class BusinessShortNameYesNoViewSpec extends ViewSpec {

  private val view = instanceOf[business_short_name_yes_no]

  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private def form(errorMessage: String): Form[YesNo] =
    MatchingForms.businessShortNameYesNoForm(errorMessage)

  private def formWithError(errorMessage: String): Form[YesNo] =
    form(errorMessage).bind(Map("yes-no-answer" -> ""))

  private def doc(
    orgType: EtmpOrganisationType = CorporateBody,
    isRow: Boolean = false,
    errorMessage: String = ""
  ): Document =
    Jsoup.parse(contentAsString(view(form(errorMessage), orgType, isRow, false, atarService)))

  private def docWithErrorSummary(
    orgType: EtmpOrganisationType = CorporateBody,
    isRow: Boolean = false,
    errorMessage: String
  ): Document =
    Jsoup.parse(contentAsString(view(formWithError(errorMessage), orgType, isRow, false, atarService)))

  "Business Short Name Yes No page" should {

    "display title" when {

      "user is during company journey" in {

        doc().title() must startWith("Does your company use a shortened name?")
      }

      "user is during partnership journey" in {

        doc(Partnership).title() must startWith("Does your partnership use a shortened name?")
      }

      "user is during charity journey" in {

        doc(UnincorporatedBody).title() must startWith("Does you charity use a shortened name?")
      }

      "user is during RoW organisation journey" in {

        doc(isRow = true).title() must startWith("Does you organisation use a shortened name?")
      }
    }

    "display header" when {

      "user is during company journey" in {

        doc().getElementsByTag("h1").text() must startWith("Does your company use a shortened name?")
      }

      "user is during partnership journey" in {

        doc(Partnership).getElementsByTag("h1").text() must startWith("Does your partnership use a shortened name?")
      }

      "user is during charity journey" in {

        doc(UnincorporatedBody).getElementsByTag("h1").text() must startWith("Does you charity use a shortened name?")
      }

      "user is during RoW organisation journey" in {

        doc(isRow = true).getElementsByTag("h1").text() must startWith("Does you organisation use a shortened name?")
      }
    }

    "display hint" in {

      doc().body.getElementById(
        "yes-no-answer-hint"
      ).text() mustBe "For example, Her Majesty's Revenue and Customs is known as HMRC."
    }

    "display yes no radio buttons" in {

      doc().body.getElementById("yes-no-answer-true").attr("checked") mustBe empty
      doc().body.getElementById("yes-no-answer-false").attr("checked") mustBe empty
    }

    "display error summary" when {

      "user is during company journey" in {

        docWithErrorSummary(errorMessage = "ecc.business-short-name-yes-no.company.empty").getElementById(
          "form-error-heading"
        ).text() mustBe "There is a problem"
        docWithErrorSummary(errorMessage = "ecc.business-short-name-yes-no.company.empty").getElementsByClass(
          "error-list"
        ).get(0).text() mustBe "Tell us if your company uses a shortened name"
      }

      "user is during partnership journey" in {

        docWithErrorSummary(
          Partnership,
          errorMessage = "ecc.business-short-name-yes-no.partnership.empty"
        ).getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary(
          Partnership,
          errorMessage = "ecc.business-short-name-yes-no.partnership.empty"
        ).getElementsByClass("error-list").get(0).text() mustBe "Tell us if your partnership uses a shortened name"
      }

      "user is during charity journey" in {

        docWithErrorSummary(
          UnincorporatedBody,
          errorMessage = "ecc.business-short-name-yes-no.charity.empty"
        ).getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary(
          UnincorporatedBody,
          errorMessage = "ecc.business-short-name-yes-no.charity.empty"
        ).getElementsByClass("error-list").get(0).text() mustBe "Tell us if your charity uses a shortened name"
      }

      "user is during RoW organisation journey" in {

        docWithErrorSummary(
          isRow = true,
          errorMessage = "ecc.business-short-name-yes-no.organisation.empty"
        ).getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary(
          isRow = true,
          errorMessage = "ecc.business-short-name-yes-no.organisation.empty"
        ).getElementsByClass("error-list").get(0).text() mustBe "Tell us if your organisation uses a shortened name"
      }
    }
  }
}
