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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.ind_st_cannot_register_using_this_service
import util.ViewSpec

class IndStCannotRegisterUsingThisServiceSpec extends ViewSpec {

  private val view                                      = instanceOf[ind_st_cannot_register_using_this_service]
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
        .getElementById("para1")
        .text mustBe "This service is not available to individuals or sole traders."
      doc
        .body()
        .getElementById("para2")
        .text must include("You can:")
    }

    "have the correct 'vat registered' link" in {
      doc.body().getElementById("link1").text() mustBe "apply for an EORI number if you are VAT registered"
      doc
        .body()
        .getElementById("link1")
        .attr("href") startsWith "https://www.tax.service.gov.uk/shortforms/form/EORIVAT?details=&vat=yes"
    }

    "have the correct 'not vat registered' link" in {
      doc.body().getElementById("link2").text() mustBe "apply for an EORI number if you are not VAT registered"
      doc
        .body()
        .getElementById("link2")
        .attr("href") startsWith "https://www.tax.service.gov.uk/shortforms/form/EORINonVATImport?details=&vat=no"
    }
  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view(atarService)))
}
