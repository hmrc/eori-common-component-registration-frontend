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

package unit.views.subscription

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_groups_cannot_register_using_this_service
import util.ViewSpec

class YouCannotRegisterUsingThisServiceSpec extends ViewSpec {

  private val view     = instanceOf[vat_groups_cannot_register_using_this_service]
  implicit val request = withFakeCSRF(FakeRequest())

  "The 'You need to use a different service' Page" should {

    "display correct heading" in {
      doc.body().getElementsByTag("h1").text() mustBe "You need to use a different service"
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
        .getElementById("explanation")
        .text mustBe "This service is not currently available to applicants that are part of a VAT group. You will need to use a different Get an EORI service."
    }
  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view()))
}
