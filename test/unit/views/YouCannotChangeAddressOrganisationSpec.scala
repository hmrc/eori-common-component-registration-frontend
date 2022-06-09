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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{
  sub02_request_not_processed,
  you_cannot_change_address_organisation,
  you_cant_use_service
}
import util.ViewSpec

class YouCannotChangeAddressOrganisationSpec extends ViewSpec {

  private implicit val request            = withFakeCSRF(fakeAtarRegisterRequest)
  private val youCantChangeAddressOrgView = instanceOf[you_cannot_change_address_organisation]

  "You cannot change address for Organisation page" should {

    "display correct title" in {
      orgDoc.title must startWith("Please contact Companies House")
    }

    "display correct heading" in {
      orgDoc.body.getElementsByTag("h1").text mustBe "Please contact Companies House"
    }

    "display correct info" in {
      orgDoc.body
        .getElementById("contact-info")
        .text mustBe "Your company's registered address is held at Companies House (opens in a new tab) . You will need to update your details there and return to complete your EORI application."
    }

  }

  private lazy val orgDoc: Document = Jsoup.parse(contentAsString(youCantChangeAddressOrgView()))

}
