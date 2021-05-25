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
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.has_existing_eori
import util.ViewSpec

class HasExistingEoriSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val service = atarService
  private val eori    = "GB234532132435"

  private val view = instanceOf[has_existing_eori]

  "Has existing EORI Page" should {

    "display correct title" in {
      doc.title must startWith("Your Government Gateway user ID is linked to an EORI")
    }

    "display correct heading" in {
      doc.getElementsByTag("h1").text() must startWith("Your Government Gateway user ID is linked to an EORI")
    }

    "display eori" in {
      doc.body.getElementById("eoriNum").text mustBe eori
    }

  }

  private lazy val doc: Document = Jsoup.parse(contentAsString(view(service, eori)))
}
