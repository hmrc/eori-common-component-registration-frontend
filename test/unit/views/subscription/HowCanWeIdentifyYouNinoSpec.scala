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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.IdMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_nino
import util.ViewSpec

class HowCanWeIdentifyYouNinoSpec extends ViewSpec {
  val form: Form[IdMatchModel]                   = subscriptionNinoForm
  val formWithNothingEntered: Form[IdMatchModel] = subscriptionNinoForm.bind(Map("nino" -> ""))

  val isInReviewMode   = false
  val previousPageUrl  = "/"
  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[how_can_we_identify_you_nino]

  "Subscription Enter Your Nino Page" should {

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "Enter your National Insurance number"
    }

    "include the heading in the title" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }

    "display an page level error if no nino entered" in {
      docWithNoNinoError
        .body()
        .getElementsByClass("error-summary-list")
        .text() mustBe "Enter your National Insurance number"
    }

    "display an field level error if no nino entered" in {
      docWithNoNinoError.body().getElementsByClass(
        "error-message"
      ).text() mustBe "Error: Enter your National Insurance number"
    }

  }

  lazy val doc: Document = Jsoup.parse(
    contentAsString(
      view(
        form,
        isInReviewMode,
        routes.HowCanWeIdentifyYouNinoController.submit(isInReviewMode, atarService, Journey.Subscribe)
      )
    )
  )

  lazy val docWithNoNinoError: Document =
    Jsoup.parse(
      contentAsString(
        view(
          formWithNothingEntered,
          isInReviewMode,
          routes.HowCanWeIdentifyYouNinoController.submit(isInReviewMode, atarService, Journey.Subscribe)
        )
      )
    )

}
