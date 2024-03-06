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
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.sign_in_with_different_details
import util.ViewSpec

class SignInWithDifferentDetailsSpec extends ViewSpec {

  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(fakeAtarRegisterRequest)

  private val view = instanceOf[sign_in_with_different_details]

  val orgName = "Test Organisation Name"

  "'You need to sign in with different details' Page without name" should {

    "display correct heading" in {
      doc.body.getElementsByTag("h1").text() must startWith(
        "Someone in your organisation has already applied for an EORI number"
      )
    }

    "include the heading in the title" in {
      doc.title must startWith(doc.body().getElementsByTag("h1").text())
    }

    "have the correct class on the h1" in {
      doc.body.getElementsByTag("h1").hasClass("govuk-heading-l") mustBe true
    }

  }

  lazy val doc: Document = Jsoup.parse(contentAsString(view(atarService)))
}
