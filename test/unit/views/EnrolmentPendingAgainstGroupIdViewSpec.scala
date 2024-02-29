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
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.enrolment_pending_against_group_id
import util.ViewSpec

class EnrolmentPendingAgainstGroupIdViewSpec extends ViewSpec {

  private val view                   = instanceOf[enrolment_pending_against_group_id]
  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  "Enrolment Pending against group id page for different service" should {
    "display correct title" in {
      otherServiceDoc.title() must startWith(messages("cds.enrolment.pending.group.title.different.service"))
    }

    "display correct heading" in {
      otherServiceDoc.body().getElementsByTag("h1").text() mustBe messages(
        "cds.enrolment.pending.group.title.different.service"
      )
    }

    "display correct paragraph 1" in {
      otherServiceDoc.body().getElementById("info-para1").text() mustBe messages(
        "cds.enrolment.pending.group.different.service.paragraph1"
      )
    }

    "display correct paragraph 2" in {
      otherServiceDoc.body().getElementById("info-para2").text() mustBe messages(
        "cds.enrolment.pending.group.different.service.paragraph2"
      )
    }

    "display correct h2" in {
      otherServiceDoc.body().getElementById("heading-2").text() mustBe messages(
        "cds.enrolment.pending.group.different.service.heading2"
      )
    }

    "display correct text for link" in {
      otherServiceDoc.body().getElementById("link-1").text() mustBe messages("ecc.address-invalid-individual.title")
    }

    "have the correct class on the link" in {
      otherServiceDoc.body().getElementById("link-1").hasClass("govuk-link") mustBe true
    }

    "have the correct class on the h1" in {
      otherServiceDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }
  }

  "Enrolment Pending against group id page for same service" should {
    "display correct title" in {
      sameServiceDoc.title() must startWith(messages("cds.enrolment.pending.group.title"))
    }

    "display correct heading" in {
      sameServiceDoc.body().getElementsByTag("h1").text() mustBe messages("cds.enrolment.pending.group.title")
    }

    "have the correct class on the h1" in {
      sameServiceDoc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }

    "display correct paragraph" in {
      sameServiceDoc.body().getElementById("info-para1").text() mustBe messages(
        "cds.enrolment.pending.group.paragraph1"
      )
    }

    "have the correct class on paragraph" in {
      sameServiceDoc.body().getElementsByTag("p").hasClass("govuk-body") mustBe true
    }
  }

  private lazy val otherServiceDoc: Document = Jsoup.parse(contentAsString(view(atarService, Some(otherService))))
  private lazy val sameServiceDoc: Document  = Jsoup.parse(contentAsString(view(atarService, Some(atarService))))

}
