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
import org.jsoup.nodes.Document
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.enrolment_exists_user_standalone
import util.ViewSpec

class StandaloneRegistrationExistsViewSpec extends ViewSpec {

  private val view                   = instanceOf[enrolment_exists_user_standalone]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Standalone Registration Exists page" should {

    "admin user" should {
      "display correct title" in {
        adminDoc.title() must startWith("Your business or organisation already has an EORI number")
      }

      "display correct heading" in {
        adminDoc.body().getElementsByTag("h1").text() mustBe "Your business or organisation already has an EORI number"
      }

      "have the correct class on the h1" in {
        adminDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
      }

      "display EORI Number" in {
        adminDoc.body().getElementById("eori-number").text() mustBe "EORI number: testEORI"
      }

      "not display EORI Number if EORI number is None" in {
        adminNoEORIDoc.body().getElementById("eori-number") mustBe null
      }

      "display sub heading for CDS" in {
        adminDoc.body().getElementsByTag("h2").text() must startWith("Customs Declaration Service")
      }

      "display tell user about CDS access and link for accessing CDS services" in {
        val link = adminDoc.body.getElementById("cds-info1")
        link.text mustBe "You can now use any of the online customs services for the Customs Declaration Service (CDS) ."
        link.getElementsByTag("a").attr(
          "href"
        ) mustBe "https://www.gov.uk/government/collections/customs-declaration-service"
      }

      "display tell admin user on how to add CDS access for the team members" in {
        val link = adminDoc.body.getElementById("cds-info2")
        link.text mustBe "You can also add team members and give them access to CDS (opens in new tab) ."
        link.getElementsByTag("a").attr("href") must endWith("/use-hmrcs-business-tax-account#adding-a-team-member")
      }
    }

    "standard user" should {
      "display correct title" in {
        standardDoc.title() must startWith("You already have an EORI number")
      }

      "display correct heading" in {
        standardDoc.body().getElementsByTag("h1").text() mustBe "You already have an EORI number"
      }

      "have the correct class on the h1" in {
        standardDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
      }

      "display EORI Number" in {
        standardDoc.body().getElementById("eori-number").text() mustBe "EORI number: testEORI"
      }

      "not display EORI Number if EORI number is None" in {
        standardNoEORIDoc.body().getElementById("eori-number") mustBe null
      }

      "display sub heading for CDS" in {
        standardDoc.body().getElementsByTag("h2").text() must startWith("Customs Declaration Service")
      }

      "display tell user about CDS access and link for accessing CDS services" in {
        val link = standardDoc.body.getElementById("cds-info1")
        link.text mustBe "You can now use any of the online customs services for the Customs Declaration Service (CDS) ."
        link.getElementsByTag("a").attr(
          "href"
        ) mustBe "https://www.gov.uk/government/collections/customs-declaration-service"
      }

    }

  }

  private lazy val adminDoc: Document          = Jsoup.parse(contentAsString(view(Some("testEORI"), true)))
  private lazy val standardDoc: Document       = Jsoup.parse(contentAsString(view(Some("testEORI"), false)))
  private lazy val adminNoEORIDoc: Document    = Jsoup.parse(contentAsString(view(None, true)))
  private lazy val standardNoEORIDoc: Document = Jsoup.parse(contentAsString(view(None, false)))

}
