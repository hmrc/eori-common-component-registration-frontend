/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_groups_cannot_register_using_this_service
import util.ViewSpec

class YouCannotRegisterUsingThisServiceSpec extends ViewSpec {

  private val view = inject[vat_groups_cannot_register_using_this_service]
  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  "The 'You need to use a different service' Page" should {

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "You need to use a different service"
    }

    "have the heading in the title" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }

    "have the correct explanation text" in {
      doc
        .body()
        .getElementById("explanation")
        .text mustBe messages("cds.matching.cannot-register-using-service.para1")
    }

    "display the correct link text and have the correct href" in {
      doc
        .body()
        .getElementById("link")
        .text mustBe messages("cds.matching.cannot-register-using-service.link.text")

      doc
        .body()
        .getElementById("link")
        .attr("href") mustBe "https://www.tax.service.gov.uk/shortforms/form/EORIVAT"

    }

  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view(atarService)))
}
