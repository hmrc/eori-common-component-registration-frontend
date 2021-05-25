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

package unit.views.registration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.you_need_different_service_iom
import util.ViewSpec

class YouNeedDifferentServiceIomSpec extends ViewSpec {

  private val view     = instanceOf[you_need_different_service_iom]
  implicit val request = withFakeCSRF(FakeRequest())

  "The 'You need to use a different service' Page" should {

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "You need to use a different online service"
    }

    "have the heading in the title" in {
      doc.title() must startWith(doc.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      doc.body().getElementsByTag("h1").hasClass("heading-large") mustBe true
    }

    "have the correct explanation text" in {
      doc
        .body()
        .getElementById("info")
        .text mustBe "To apply for an EORI number as a trader from the Isle of Man you need to use an online form, depending on whether you're:"
    }

    "have the correct 'vat registered' link" in {
      doc.body().getElementById("vat-registered").text() mustBe "VAT-registered"
      doc
        .body()
        .getElementById("vat-registered")
        .attr("href") startsWith "https://www.tax.service.gov.uk/shortforms/form/EORIVAT?details=&vat=yes"
    }

    "have the correct 'not vat registered' link" in {
      doc.body().getElementById("not-vat-registered").text() mustBe "not VAT-registered"
      doc
        .body()
        .getElementById("not-vat-registered")
        .attr("href") startsWith "https://www.tax.service.gov.uk/shortforms/form/EORINonVATImport?details=&vat=no"
    }
  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view()))
}
