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
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.sub02_request_not_processed
import util.ViewSpec

class Sub02RequestNotProcessedSpec extends ViewSpec {

  private val pageHeadingExpectedText = messages("cds.request-not-processed.heading")

  private val view = instanceOf[sub02_request_not_processed]

  "GYE Request not processed outcome page" should {

    "have the correct title " in {
      doc.title() must startWith(pageHeadingExpectedText)
    }

    "have the correct heading" in {
      doc.getElementById("page-heading").text() mustBe pageHeadingExpectedText
    }

    "have the correct para1" in {
      doc.getElementById("para1").text() mustBe messages("cds.error.message.part1")
    }

    "have the correct para2" in {
      doc.getElementById("para2").text() mustBe messages("cds.error.message.part2")
    }

    "have the correct h2" in {
      doc.getElementById("page-heading2").text() mustBe messages("cds.request-not-processed.heading2")
    }

    "have the correct link text" in {
      doc.getElementById("contact-us-text-and-link").text() must startWith(
        messages("cds.request-not-processed.contact-us")
      )
    }
  }

  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  lazy val doc: Document = Jsoup.parse(contentAsString(view(atarService)))
}
