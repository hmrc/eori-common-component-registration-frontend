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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sub02_eori_already_exists
import util.ViewSpec

class Sub02EoriAlreadyExistsSpec extends ViewSpec {

  private val name                    = "John Doe"
  private val processedDate           = "1 March 2019"
  private val pageTitleExpectedText   = "You already have an EORI"
  private val pageHeadingExpectedText = s"Our records show that $name already has an EORI number."
  private val processDateExpectedText = s"Application received by HMRC on $processedDate"

  private val view = instanceOf[sub02_eori_already_exists]

  "GYE EORI Already Exists outcome page" should {

    "have the correct title " in {
      doc.title() must startWith(pageTitleExpectedText)
    }

    "have the correct heading" in {
      doc.getElementById("page-heading").text() mustBe pageHeadingExpectedText
    }

    "have the correct processed date" in {
      doc.getElementById("processed-date").text() mustBe processDateExpectedText
    }

    "have the correct vat registered text" in {
      doc.getElementById("vatRegisteredHeading").text() mustBe "If you are VAT registered"
      doc.getElementById("vatRegisteredPara1").text() mustBe "Your EORI number will be in the following format:"
      doc.getElementById("vatRegisteredPara2").text() mustBe "GB XXXXXXXXX 000 Where XXXXXXXXX is your VAT number."
    }
  }

  implicit val request = withFakeCSRF(FakeRequest())

  lazy val doc: Document = Jsoup.parse(contentAsString(view(name, processedDate)))
}
