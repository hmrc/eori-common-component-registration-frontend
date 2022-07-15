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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{
  enrolment_exists_group_standalone,
  enrolment_exists_user_standalone
}
import util.ViewSpec

class StandaloneRegistrationExistsForGroupViewSpec extends ViewSpec {

  private val view                   = instanceOf[enrolment_exists_group_standalone]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Standalong Registration Exists page" should {

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

      "display sub heading for CDS" in {
        adminDoc.body().getElementsByTag("h2").text() must startWith(
          "Start using the Customs Declaration Service (CDS)"
        )
      }

      "display tell user about CDS access and link for accessing CDS services" in {
        val info = adminDoc.body.getElementById("cds-info1")
        info.text mustBe "If you want access to CDS, you need to ask the person who set up your Government Gateway user ID, or another person with administrator permissions in your Government Gateway team."

      }

      "display tell admin user on how to add CDS access for the team members" in {
        val link = adminDoc.body.getElementById("cds-info2")
        link.text mustBe "They will need to sign in and follow steps 6,7,8 and 9 to give a team member access to a tax duty or scheme (opens in new tab) ."
        link.getElementsByTag("a").attr("href") must endWith("/use-hmrcs-business-tax-account#adding-a-team-member")
      }
    }

    "standard user" should {
      "display correct title" in {
        standardDoc.title() must startWith("Your business or organisation already has an EORI number")
      }

      "display correct heading" in {
        standardDoc.body().getElementsByTag(
          "h1"
        ).text() mustBe "Your business or organisation already has an EORI number"
      }

      "have the correct class on the h1" in {
        standardDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
      }

      "display EORI Number" in {
        standardDoc.body().getElementById("eori-number").text() mustBe "EORI number: testEORI"
      }

      "display sub heading for CDS" in {
        standardDoc.body().getElementsByTag("h2").text() must startWith(
          "Start using the Customs Declaration Service (CDS)"
        )
      }

      "display tell user about CDS access and link for accessing CDS services" in {
        val link = standardDoc.body.getElementById("cds-info1")
        link.text mustBe "If you want access to CDS, you need to go to manage account, and follow steps 6, 7, 8 and 9 to give yourself access to a tax, duty or scheme (opens in new tab) ."
        link.getElementsByTag("a").attr("href") must endWith("/use-hmrcs-business-tax-account#adding-a-team-member")
      }

    }

  }

  private lazy val adminDoc: Document    = Jsoup.parse(contentAsString(view(Some("testEORI"), true)))
  private lazy val standardDoc: Document = Jsoup.parse(contentAsString(view(Some("testEORI"), false)))

}
