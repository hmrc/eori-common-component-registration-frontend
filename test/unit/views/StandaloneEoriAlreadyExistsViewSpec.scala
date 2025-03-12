/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.StandaloneAlreadyHaveEoriViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.standalone_already_have_eori
import util.ViewSpec

class StandaloneEoriAlreadyExistsViewSpec extends ViewSpec {

  private val view                   = inject[standalone_already_have_eori]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Standalone Eori Already Exists page" should {

    "Non Individual user" should {
      "display correct title" in {
        orgDoc.title() must startWith("Your business or organisation already has an EORI number")
      }

      "display correct heading" in {
        orgDoc.body().getElementsByTag("h1").text() mustBe "Your business or organisation already has an EORI number"
      }

      "have the correct class on the h1" in {
        orgDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
      }

      "display EORI Number" in {
        orgDoc.body().getElementById("eori-number").text() mustBe "EORI number: GBXXXXXXXX"
      }

      "not display EORI Number if EORI number is None" in {
        adminDocNoEORI.body().getElementById("eori-number") mustBe null
      }

      "display sub heading for CDS" in {
        orgDoc.body().getElementsByTag("h2").text() must startWith(
          "Start using the Customs Declaration Service (CDS) Support links"
        )
      }

      "display tell user about CDS access and link for accessing CDS services" in {
        val link = orgDoc.body.getElementById("info2")
        link.text mustBe "You can subscribe to CDS and give access to other team members."
        link
          .getElementsByTag("a")
          .attr(
            "href"
          ) mustBe "https://www.gov.uk/guidance/get-access-to-the-customs-declaration-service"
      }

    }

    "Individual user" should {
      "display correct title" in {
        individualDoc.title() must startWith("You already have an EORI number")
      }

      "display correct heading" in {
        individualDoc.body().getElementsByTag("h1").text() mustBe "You already have an EORI number"
      }

      "have the correct class on the h1" in {
        individualDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
      }

      "display EORI Number" in {
        individualDoc.body().getElementById("eori-number").text() mustBe "EORI number: GBXXXXXXXX"
      }

      "not display EORI Number if EORI number is None" in {
        standardDocNoEori.body().getElementById("eori-number") mustBe null
      }

    }

  }

  private lazy val individualDoc: Document =
    Jsoup.parse(
      contentAsString(
        view(
          Some("GBXXXXXXXX"),
          isAdminUser = false,
          eoriOnlyService,
          StandaloneAlreadyHaveEoriViewModel(isAdminUser = false)
        )
      )
    )

  private lazy val orgDoc: Document =
    Jsoup.parse(
      contentAsString(
        view(
          Some("GBXXXXXXXX"),
          isAdminUser = true,
          eoriOnlyService,
          StandaloneAlreadyHaveEoriViewModel(isAdminUser = true)
        )
      )
    )

  private lazy val standardDocNoEori: Document =
    Jsoup.parse(
      contentAsString(
        view(None, isAdminUser = true, eoriOnlyService, StandaloneAlreadyHaveEoriViewModel(isAdminUser = true))
      )
    )

  private lazy val adminDocNoEORI: Document =
    Jsoup.parse(
      contentAsString(
        view(None, isAdminUser = true, eoriOnlyService, StandaloneAlreadyHaveEoriViewModel(isAdminUser = true))
      )
    )

}
