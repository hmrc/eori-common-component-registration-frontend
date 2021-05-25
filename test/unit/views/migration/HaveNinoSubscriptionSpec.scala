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

package unit.views.migration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NinoMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.haveRowIndividualsNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.match_nino_subscription
import util.ViewSpec

class HaveNinoSubscriptionSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val standardForm: Form[NinoMatchModel] = haveRowIndividualsNinoForm
  private val noOptionSelectedForm               = haveRowIndividualsNinoForm.bind(Map.empty[String, String])

  private val view = instanceOf[match_nino_subscription]

  "Fresh Subscription Have Nino Page" should {
    "display correct heading" in {
      doc.body.getElementsByTag("h1").text must startWith("Do you have a National Insurance number issued in the UK?")
    }

    "display correct title" in {
      doc.title must startWith("Do you have a National Insurance number issued in the UK?")
    }

    "have 'yes' radio button" in {
      doc.body.getElementById("have-nino-true").attr("value") mustBe "true"
    }

    "have 'no' radio button" in {
      doc.body.getElementById("have-nino-false").attr("value") mustBe "false"
    }

    "have description with proper content" in {
      doc.body
        .getElementById("have-nino-hintHtml")
        .text must include("You will have a National Insurance number if you have worked in the UK.")
    }

    "Have correct hint for nino field" in {
      doc.body.getElementById("have-nino-hintHtml").text must include(
        "It’s on your National Insurance card, benefit letter, payslip or P60."
      )
      doc.body.getElementById("have-nino-hintHtml").text must include("For example, 'QQ123456C'")
    }

  }

  "No option selected Subscription Have Nino Page" should {
    "have page level error with correct message" in {
      docWithNoOptionSelected.body.getElementById("form-error-heading").text mustBe "There is a problem"
      docWithNoOptionSelected.body
        .getElementsByAttributeValue("href", "#have-nino-true")
        .text mustBe "Select yes if you have a National Insurance number"
    }
  }

  lazy val doc: Document =
    Jsoup.parse(contentAsString(view(standardForm, isInReviewMode = false, atarService, Journey.Subscribe)))

  lazy val docWithNoOptionSelected: Document =
    Jsoup.parse(contentAsString(view(noOptionSelectedForm, isInReviewMode = false, atarService, Journey.Subscribe)))

}
