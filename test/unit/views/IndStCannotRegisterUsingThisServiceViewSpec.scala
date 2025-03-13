/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.ind_st_cannot_register_using_this_service
import util.ViewSpec

class IndStCannotRegisterUsingThisServiceViewSpec extends ViewSpec {

  private val view = inject[ind_st_cannot_register_using_this_service]

  implicit val request: Request[Any] = withFakeCSRF(FakeRequest())

  private def doc(): Document =
    Jsoup.parse(contentAsString(view(atarService)))

  "Individual Sole trader Cannot Use this service page" should {

    "display title" in {

      doc().title() must startWith("You need to use a different service")
    }

    "display header for individual" in {

      doc().body().getElementsByTag("h1").text() mustBe "You need to use a different service"
    }

    "display line 1 with text" in {

      doc()
        .body()
        .getElementById("para1")
        .text() mustBe "This service is not available to individuals or sole traders."
    }

    "display line 2 with text" in {

      doc()
        .body()
        .getElementById("para2")
        .text() mustBe "You can:"
    }

    "display link 1 with KANA form VAT Registered" in {

      doc()
        .body()
        .getElementById("link1")
        .text() mustBe "apply for an EORI number if you are VAT registered"

      doc()
        .body()
        .getElementById("link1")
        .attr("href") mustBe "https://www.tax.service.gov.uk/shortforms/form/EORIVAT?details=&vat=yes"
    }

    "display link 1 with KANA form non-VAT Registered" in {

      doc()
        .body()
        .getElementById("link2")
        .text() mustBe "apply for an EORI number if you are not VAT registered"

      doc()
        .body()
        .getElementById("link2")
        .attr("href") mustBe "https://www.tax.service.gov.uk/shortforms/form/EORINonVATImport?details=&vat=no"
    }
  }
}
