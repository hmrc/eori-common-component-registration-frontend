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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.enrolment_pending_for_user
import util.ViewSpec

class EnrolmentPendingForUserViewSpec extends ViewSpec {

  private val view                   = instanceOf[enrolment_pending_for_user]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Enrolment Pending against group id page" should {
    "display correct title" in {
      doc.title() must startWith("There is a problem")
    }

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "There is a problem"
    }

    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "display the correct text for Subscribe to different service" in {
      doc
        .body()
        .getElementById("info")
        .text mustBe "We are currently processing your subscription request to Other Service."
    }
  }

  private lazy val doc: Document = Jsoup.parse(contentAsString(view(atarService, Some(otherService))))

}
