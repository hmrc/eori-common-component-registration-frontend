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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sub02_request_not_processed
import util.ViewSpec

class Sub02RequestNotProcessedSpec extends ViewSpec {

  private val pageHeadingExpectedText = "You cannot use this service"

  private val view = instanceOf[sub02_request_not_processed]

  "GYE Request not processed outcome page" should {

    "have the correct title " in {
      doc.title() must startWith(pageHeadingExpectedText)
    }

    "have the correct heading" in {
      doc.getElementById("page-heading").text() mustBe pageHeadingExpectedText
    }
  }

  implicit val request = withFakeCSRF(FakeRequest())

  lazy val doc: Document = Jsoup.parse(contentAsString(view()))
}
