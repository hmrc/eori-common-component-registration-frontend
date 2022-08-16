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

import java.time
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, LLP}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.date_of_establishment
import util.ViewSpec

class DateOfEstablishmentSpec extends ViewSpec with MockitoSugar {
  val form: Form[time.LocalDate] = SubscriptionForm.subscriptionDateOfEstablishmentForm
  val isInReviewMode             = false
  implicit val request           = withFakeCSRF(FakeRequest())

  private val view = instanceOf[date_of_establishment]

  "On a UK journey the 'When was the organisation established?' page" should {
    "display correct title" in {
      doc.title() must startWith("When was the company established?")
    }
    "have the correct h1 text" in {
      doc.body.getElementsByTag("legend").text() mustBe "When was the company established?"
    }
    "have the correct class on the legend" in {
      doc.body.getElementsByTag("legend").hasClass("govuk-fieldset__legend") mustBe true
    }
    "have the correct text in the hint" in {
      doc.body.getElementById("date-of-establishment-hint").text() must include("Enter the date shown on the organisation’s certificate of incorporation.")
    }
    "have the correct text in the intro paragraph" in {
      doc.body
        .getElementById("date-of-establishment-label")
        .text() mustBe "Enter the date shown on the organisation’s certificate of incorporation. You can find the date your organisation was established on the Companies House register (opens in new tab)"
    }
  }

  "On a RoW journey the 'When was the organisation established?' page" should {
    "display correct title" in {
      docRestOfWorld.title must startWith("When was the organisation established?")
    }
    "have the correct h1 text" in {
      docRestOfWorld.body.getElementsByTag("legend").text() mustBe "When was the organisation established?"
    }
    "have the correct class on the legend" in {
      docRestOfWorld.body.getElementsByTag("legend").hasClass("govuk-fieldset__legend") mustBe true
    }
    "have the correct text in the description" in {
      docRestOfWorld.body
        .getElementById("date-of-establishment-hint")
        .text() mustBe "Enter the date shown on the organisation’s certificate of incorporation. You can find the date your organisation was established on the Companies House register (opens in new tab)"
    }
  }

  "On an LLP org type journey Date Established page" should {
    "display correct title" in {
      docLlp.title must startWith("When was the partnership established?")
    }
    "have the correct legend text" in {
      docLlp.body.getElementsByTag("legend").text() mustBe "When was the partnership established?"
    }
    "have the correct class on the legend" in {
      docLlp.body.getElementsByTag("legend").hasClass("govuk-fieldset__legend") mustBe true
    }
  }

  lazy val doc: Document = {
    val result =
      view(form, isInReviewMode, orgType = CorporateBody, isRestOfWorldJourney = false, atarService)
    Jsoup.parse(contentAsString(result))
  }

  lazy val docRestOfWorld: Document = {
    val result =
      view(form, isInReviewMode, orgType = CorporateBody, isRestOfWorldJourney = true, atarService)
    Jsoup.parse(contentAsString(result))
  }

  lazy val docLlp: Document = {
    val result =
      view(form, isInReviewMode, orgType = LLP, isRestOfWorldJourney = false, atarService)
    Jsoup.parse(contentAsString(result))
  }

}
