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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.xi_eori_guidance
import util.ViewSpec

class XiEoriGuidanceViewSpec extends ViewSpec {

  implicit val request = withFakeCSRF(FakeRequest())

  private val view = instanceOf[xi_eori_guidance]

  private val doc: Document = Jsoup.parse(contentAsString(view()))

  "XI Eori Guidance" should {

    "display title" in {

      doc.title() must startWith("Get an XI EORI number")
    }

    "display header" in {

      doc.body().getElementsByTag("h1").text() mustBe "How to get an XI EORI number"
    }

    "display XI eori paragraph" in {

      doc.body().getElementById("xi-how-to-register").text() mustBe "If you need an EORI starting with XI"
      doc.body().getElementById(
        "xi-how-to-register-text"
      ).text() mustBe "You need to fill in an application form. To fill in your application correctly and ensure you get your EORI number as soon as possible you must answer 'Yes' to the questions:"

      val bulletList = doc.body().getElementsByTag("ul").get(2)

      bulletList.getElementsByTag("li").get(
        0
      ).text() mustBe "Are you, or will you be, trading with Northern Ireland or are you a business established in Northern Ireland?"
      bulletList.getElementsByTag("li").get(1).text() mustBe "Query regarding a current EORI number application"
    }

    "display warning" in {

      val warning     = doc.body().getElementsByClass("govuk-warning-text").get(0)
      val warningMark = warning.getElementsByClass("govuk-warning-text__icon").get(0)
      val warningText = warning.getElementsByClass("govuk-warning-text__text").get(0)

      warningMark.attr("aria-hidden") mustBe "true"
      warningMark.text() mustBe "!"

      warningText.text() mustBe "Do not tick the box \"Query regarding an EORI number already issued\" or \"EORI number to register for customs system access\"."

    }

    "display apply for an eori link" in {

      doc.body().getElementById("apply-for-eori").text() mustBe "Apply for an XI EORI."
    }
  }
}
