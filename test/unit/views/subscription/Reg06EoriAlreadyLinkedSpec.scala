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

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.reg06_eori_already_linked
import uk.gov.hmrc.play.language.LanguageUtils
import util.ViewSpec

class Reg06EoriAlreadyLinkedSpec extends ViewSpec {

  private val name              = "John Doe"
  private val eori              = "GB123456789012"
  private val processedDate     = DateTime.now()
  private val expectedPageTitle = "The Advance Tariff Rulings subscription request has been unsuccessful"
  private val languageUtils     = instanceOf[LanguageUtils]

  private val pageHeadingExpectedText =
    s"The Advance Tariff Rulings subscription request for $name has been unsuccessful"

  private val processDateExpectedText =
    s"Application received on ${languageUtils.Dates.formatDate(processedDate.toLocalDate)}"

  private val view = instanceOf[reg06_eori_already_linked]

  "GYE EORI Already Linked outcome page" should {

    "have the correct page title" in {

      doc().title() must startWith(expectedPageTitle)
    }

    "have the right heading" in {

      doc().getElementById("page-heading").text() mustBe pageHeadingExpectedText
    }

    "have the right processed date" in {

      doc().getElementById("processed-date").text() mustBe processDateExpectedText
    }

    "have the right vat registered text" in {

      doc()
        .getElementById("use-cds-para")
        .text() mustBe "You will not be able to use Advance Tariff Rulings until this issue has been resolved."
    }

    "have the feedback link" in {

      doc()
        .getElementById("what-you-think")
        .text() must include("What did you think of this service?")
      doc().getElementById("feedback_link").attributes().get("href") must endWith(
        "/feedback/eori-common-component-subscribe-atar"
      )
    }

    "have a feedback 'continue' button" in {

      val link = doc().body.getElementById("feedback-continue")

      link.text mustBe "More about Advance Tariff Rulings"
      link.attr("href") mustBe "/test-atar/feedback?status=Failed"
    }

    "have a no feedback 'continue' button when config missing" in {

      val link = doc(service = atarService.copy(feedbackUrl = None)).body.getElementById("feedback-continue")

      link mustBe null
    }
  }

  "EORI already linked page" should {

    "has specific content for individual with UTR" in {

      val page = doc(isIndividual = true, hasUtr = true).body()

      val heading           = page.getElementById("why-heading")
      val individualHeading = page.getElementById("individual")
      val utrElement        = page.getElementById("individual-utr")
      val eoriElement       = page.getElementById("eori")

      heading.text() mustBe "Why the application was unsuccessful"
      individualHeading.text() mustBe "The following details do not match the name you entered:"
      utrElement.text() mustBe "Self Assessment Unique Taxpayer Reference (UTR)"
      eoriElement.text() mustBe "EORI number"

      page.getElementById("individual-nino") mustBe null
      page.getElementById("organisation") mustBe null
      page.getElementById("organisation-utr") mustBe null
    }

    "has specific content for individual with NINO" in {

      val page = doc(isIndividual = true, hasUtr = false).body()

      val heading           = page.getElementById("why-heading")
      val individualHeading = page.getElementById("individual")
      val ninoElement       = page.getElementById("individual-nino")
      val eoriElement       = page.getElementById("eori")

      heading.text() mustBe "Why the application was unsuccessful"
      individualHeading.text() mustBe "The following details do not match the name you entered:"
      ninoElement.text() mustBe "National Insurance number"
      eoriElement.text() mustBe "EORI number"

      page.getElementById("individual-utr") mustBe null
      page.getElementById("organisation") mustBe null
      page.getElementById("organisation-utr") mustBe null
    }

    "has specific content for organisation" in {

      val page = doc(isIndividual = false, hasUtr = false).body()

      val heading             = page.getElementById("why-heading")
      val organisationHeading = page.getElementById("organisation")
      val utrElement          = page.getElementById("organisation-utr")
      val eoriElement         = page.getElementById("eori")

      heading.text() mustBe "Why the application was unsuccessful"
      organisationHeading.text() mustBe "The following details do not match the company name you entered:"
      utrElement.text() mustBe "Corporation Tax Unique Taxpayer Reference (UTR)"
      eoriElement.text() mustBe "EORI number"

      page.getElementById("individual") mustBe null
      page.getElementById("individual-utr") mustBe null
      page.getElementById("individual-nino") mustBe null
    }

    "has link to start again" in {

      val link = doc().body().getElementById("again-link")

      link.toString must include(ApplicationController.startSubscription(atarService).url)
    }
  }

  implicit val request = withFakeCSRF(FakeRequest.apply("GET", "/atar/subscribe"))

  def doc(isIndividual: Boolean = true, hasUtr: Boolean = true, service: Service = atarService): Document =
    Jsoup.parse(contentAsString(view(name, eori, processedDate, service, isIndividual, hasUtr)))

}
