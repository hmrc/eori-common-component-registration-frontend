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

package unit.views.partials

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.partials.footer
import util.ViewSpec

class FooterSpec extends ViewSpec {
  implicit val request = withFakeCSRF(FakeRequest.apply("GET", "/customs-enrolment-services/atar/subscribe"))

  private val view = instanceOf[footer]

  "Footer" should {
    "have correct licence logo link" in {
      doc
        .getElementById("licence-logo")
        .attr("href") mustBe "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"
    }

    "have correct licence text link" in {
      doc
        .getElementById("licence-text")
        .attr("href") mustBe "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"
    }

    "have correct Privacy Policy text link" in {
      doc.getElementById("privacy").attr("href") mustBe
        "https://www.gov.uk/government/publications/data-protection-act-dpa-information-hm-revenue-and-customs-hold-about-you"
    }

    "have correct Terms And Conditions link" in {
      doc.getElementById("conditions").attr("href") mustBe "https://www.tax.service.gov.uk/help/terms-and-conditions"
    }

    "have correct Accessibility link" in {
      doc.getElementById("accessibility").attr(
        "href"
      ) mustBe "http://localhost:9582/advance-tariff-application/accessibility"
    }
  }

  lazy val doc: Document = {
    val result = view(messages, request)
    val doc    = Jsoup.parse(contentAsString(result))
    doc
  }

}
