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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.BusinessShortName
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CorporateBody,
  EtmpOrganisationType,
  Partnership,
  UnincorporatedBody
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.registration.BusinessShortNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_short_name
import util.ViewSpec

class BusinessShortNameViewSpec extends ViewSpec {

  private val view = instanceOf[business_short_name]

  implicit val request: Request[Any] = withFakeCSRF(fakeAtarSubscribeRequest)

  private def form(orgType: EtmpOrganisationType, isRow: Boolean): Form[BusinessShortName] =
    BusinessShortNameForm.form(orgType, isRow)

  private def formWithError(orgType: EtmpOrganisationType, isRow: Boolean): Form[BusinessShortName] =
    form(orgType, isRow).bind(Map("short-name" -> ""))

  private def doc(orgType: EtmpOrganisationType = CorporateBody, isRow: Boolean = false): Document =
    Jsoup.parse(contentAsString(view(form(orgType, isRow), orgType, isRow, false, atarService)))

  private def docWithErrorSummary(orgType: EtmpOrganisationType = CorporateBody, isRow: Boolean = false): Document =
    Jsoup.parse(contentAsString(view(formWithError(orgType, isRow), orgType, isRow, false, atarService)))

  "Business Short Name page" should {

    "display title" when {

      "user is during company journey" in {

        doc().title() must startWith("What is your company's short name?")
      }

      "user is during partnership journey" in {

        doc(Partnership).title() must startWith("What is your partnership's short name?")
      }

      "user is during charity journey" in {

        doc(UnincorporatedBody).title() must startWith("What is your charity's short name?")
      }

      "user is during RoW organisation journey" in {

        doc(isRow = true).title() must startWith("What is your organisation's short name?")
      }
    }

    "display header" when {

      "user is during company journey" in {

        doc().getElementsByTag("h1").text() must startWith("What is your company's short name?")
      }

      "user is during partnership journey" in {

        doc(Partnership).getElementsByTag("h1").text() must startWith("What is your partnership's short name?")
      }

      "user is during charity journey" in {

        doc(UnincorporatedBody).getElementsByTag("h1").text() must startWith("What is your charity's short name?")
      }

      "user is during RoW organisation journey" in {

        doc(isRow = true).getElementsByTag("h1").text() must startWith("What is your organisation's short name?")
      }
    }

    "display short name input with label" in {

      val shortNameInput = doc().body().getElementById("short-name-outer")

      shortNameInput.getElementsByTag("label").get(0).text() must startWith("Short name")
    }

    "display error summary" when {

      "user is during company journey" in {

        docWithErrorSummary().getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary().getElementsByClass("error-list").get(
          0
        ).text() mustBe "Enter your company's shortened name"
      }

      "user is during partnership journey" in {

        docWithErrorSummary(Partnership).getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary(Partnership).getElementsByClass("error-list").get(
          0
        ).text() mustBe "Enter your partnership's shortened name"
      }

      "user is during charity journey" in {

        docWithErrorSummary(UnincorporatedBody).getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary(UnincorporatedBody).getElementsByClass("error-list").get(
          0
        ).text() mustBe "Enter your charity's shortened name"
      }

      "user is during RoW organisation journey" in {

        docWithErrorSummary(isRow = true).getElementById("form-error-heading").text() mustBe "There is a problem"
        docWithErrorSummary(isRow = true).getElementsByClass("error-list").get(
          0
        ).text() mustBe "Enter your organisation's shortened name"
      }
    }
  }
}
