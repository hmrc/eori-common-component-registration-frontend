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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.enrolment_pending_against_group_id
import util.ViewSpec

class EnrolmentPendingAgainstGroupIdViewSpec extends ViewSpec {

  private val view                   = instanceOf[enrolment_pending_against_group_id]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Enrolment Pending against group id page" should {
    "display correct title" in {
      gyeDoc.title() must startWith("Someone in your organisation has already applied")
    }

    "display correct heading" in {
      gyeDoc.body().getElementsByTag("h1").text() mustBe "Someone in your organisation has already applied"
    }

    "have the correct class on the h1" in {
      gyeDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }
  }

  private lazy val gyeDoc: Document = Jsoup.parse(contentAsString(view()))

}
