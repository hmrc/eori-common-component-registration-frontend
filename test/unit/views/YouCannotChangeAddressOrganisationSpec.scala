/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.you_cannot_change_address_organisation
import util.ViewSpec

class YouCannotChangeAddressOrganisationSpec extends ViewSpec {

  private implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(fakeAtarRegisterRequest)
  private val youCantChangeAddressOrgView                       = inject[you_cannot_change_address_organisation]

  "You cannot change address for Organisation page" should {

    "display correct title" in {
      orgDoc.title must startWith("Contact Companies House")
    }

    "display correct heading" in {
      orgDoc.body.getElementsByTag("h1").text mustBe "Contact Companies House"
    }

    "display correct info" in {
      orgDoc.body
        .getElementById("contact-info")
        .text mustBe "Your registered address is held at Companies House (opens in new tab) . You will need to update your details there and return to complete your EORI number application."
    }

  }

  private lazy val orgDoc: Document = Jsoup.parse(contentAsString(youCantChangeAddressOrgView(atarService)))

}
