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

import org.jsoup.Jsoup
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.start
import util.ViewSpec

class StartViewSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarRegisterRequest)

  val startPage = instanceOf[start]
  val doc       = Jsoup.parse(contentAsString(startPage(atarService)))

  "Start page" should {

    "have correct title" in {

      doc.title must startWith("To access Advance Tariff Rulings, you need a GB EORI number.")
    }

    "have correct first paragraph" in {

      doc.getElementById("start-heading").text() mustBe "To access Advance Tariff Rulings, you need a GB EORI number."
      doc.getElementById(
        "para1"
      ).text() mustBe "We will ask you to enter some details and, if approved, we'll give you a GB EORI number and subscription to Advance Tariff Rulings."
      doc.getElementById(
        "para2"
      ).text() mustBe "If approved, your subscription will be ready to use to within 2 hours. If we need to make more checks it may take up to 5 working days."
      doc.getElementById("para3").text() mustBe "You only need to do this once."
    }

    "have correct GB EORI section" in {

      doc.getElementById("gb-eori-heading").text() mustBe "What you will need to subscribe and get a GB EORI number"
      doc.getElementById("gb-eori-para1-heading").text() mustBe "If you are UK VAT registered we will ask you for:"
      doc.getElementById("gb-eori-para1-bullet1").text() mustBe "VAT number"
      doc.getElementById("gb-eori-para1-bullet2").text() mustBe "VAT registered address postcode"
      doc.getElementById("gb-eori-para1-bullet3").text() mustBe "VAT effective date"
      doc.getElementById("gb-eori-para2").text() mustBe "Find your VAT details (opens in new tab)"
      doc.getElementById("gb-eori-para3-heading").text() mustBe "We also ask for:"
      doc.getElementById(
        "gb-eori-para3-bullet1"
      ).text() mustBe "Standard Industrial Classification (SIC) code. Find your SIC code (opens in new tab)."
    }

    "have correct company section" in {

      doc.getElementById("company-heading").text() mustBe "Company or other organisation details"
      doc.getElementById(
        "company-para-heading"
      ).text() mustBe "If you are a limited company, partnership or charity, we will also ask for:"
      doc.getElementById(
        "company-para-bullet1"
      ).text() mustBe "Corporation Tax Unique Tax Reference (UTR) if you pay corporation Tax in the UK. You can find a UTR number (opens in new tab)."
      doc.getElementById("company-para-bullet2").text() mustBe "Registered company name"
      doc.getElementById("company-para-bullet3").text() mustBe "Registered company address"
      doc.getElementById("company-para-bullet4").text() mustBe "Date of establishment"
    }

    "have correct sole trader section" in {

      doc.getElementById("sole-trader-heading").text() mustBe "Sole trader or individual details"
      doc.getElementById(
        "sole-trader-para-heading"
      ).text() mustBe "If you have worked in the UK or registered for self-assessment, we will ask for one of the following:"
      doc.getElementById("sole-trader-para-bullet1").text() mustBe "National Insurance number"
      doc.getElementById(
        "sole-trader-para-bullet2"
      ).text() mustBe "Self Assessment Unique Taxpayer Reference (UTR). You can find a lost UTR number (opens in new tab)."
    }
  }
}
