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

package unit.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.enrolment_pending_against_group_id
import util.ViewSpec

class EnrolmentPendingAgainstGroupIdViewSpec extends ViewSpec {

  private val view                   = instanceOf[enrolment_pending_against_group_id]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Enrolment Pending against group id page" should {
    "display correct title" in {
      gyeDoc.title() must startWith("There is a problem")
    }

    "display correct heading" in {
      gyeDoc.body().getElementsByTag("h1").text() mustBe "There is a problem"
    }

    "have the correct class on the h1" in {
      gyeDoc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "display the correct text for Gye" in {
      gyeDoc
        .body()
        .getElementById("info")
        .text mustBe "The Government Gateway ID you used to sign in is part of a team that has already applied for an EORI number. This application is being processed."
    }

    "display the correct text for Subscribe to same service" in {
      migrateDoc(atarService)
        .body()
        .getElementById("info")
        .text mustBe "Our records show that someone in your organisation has already applied for this service."
    }

    "display the correct text for Subscribe to different service" in {
      migrateDoc(otherService)
        .body()
        .getElementById("info")
        .text mustBe "We are currently processing a subscription request to Other Service from someone in your organisation."
    }
  }

  private lazy val gyeDoc: Document =
    Jsoup.parse(contentAsString(view(atarService, Journey.Register, Some(otherService))))

  private def migrateDoc(otherService: Service): Document =
    Jsoup.parse(contentAsString(view(atarService, Journey.Subscribe, Some(otherService))))

}
