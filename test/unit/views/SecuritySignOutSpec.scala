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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.display_sign_out
import util.ViewSpec

class SecuritySignOutSpec extends ViewSpec {

  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(FakeRequest())

  private val view = inject[display_sign_out]

  "Security Sign Out Page" should {

    "display correct title" in {
      doc.title must startWith("For your security, we signed you out")
    }

    "display hint" in {
      doc.body.getElementById("hint").text mustBe "We did not save your answers."
    }

    "have a Sign in button with the correct href when journey is register" in {
      doc.body().getElementsByClass("govuk-button").attr("href") mustBe "/customs-registration-services/atar/register"
    }
  }

  private lazy val doc: Document = Jsoup.parse(contentAsString(view(atarService)))
}
