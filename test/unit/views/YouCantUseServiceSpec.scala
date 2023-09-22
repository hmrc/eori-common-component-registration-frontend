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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.you_cant_use_service
import util.ViewSpec

class YouCantUseServiceSpec extends ViewSpec {

  private implicit val request      = withFakeCSRF(fakeAtarRegisterRequest)
  private val youCantUseServiceView = instanceOf[you_cant_use_service]

  "You cannot use this service page for users of type standard org" should {

    "display correct title" in {
      standardOrgDoc.title must startWith("You cannot use this service")
    }

    "display correct heading" in {
      standardOrgDoc.body.getElementsByTag("h1").text mustBe "You cannot use this service"
    }

    "have a Sign out button with the correct href" in {
      standardOrgDoc.body().getElementsByClass("govuk-button").attr("href") must endWith("/register/logout")
    }
  }

  "You cannot use this service page for users of type agent" should {

    "display correct title" in {
      agentDoc.title must startWith("You cannot use this service")
    }

    "display correct heading" in {
      agentDoc.body.getElementsByTag("h1").text mustBe "You cannot use this service"
    }

    "have a Sign out button with the correct href" in {
      agentDoc.body().getElementsByClass("govuk-button").attr("href") must endWith("/register/logout")
    }
  }

  private lazy val standardOrgDoc: Document =
    Jsoup.parse(contentAsString(youCantUseServiceView(Some(Organisation), atarService)))

  private lazy val agentDoc: Document = Jsoup.parse(contentAsString(youCantUseServiceView(Some(Agent), atarService)))

}
