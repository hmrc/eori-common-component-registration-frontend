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
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.sub01_outcome_processing
import util.ViewSpec

class Sub01OutcomeProcessingSpec extends ViewSpec {

  implicit val request: Request[AnyContentAsEmpty.type] = withFakeCSRF(fakeAtarRegisterRequest)

  private val view = inject[sub01_outcome_processing]

  val orgName       = "Test Organisation Name"
  val processedDate = "01 Jan 2019"

  "Sub01 outcome pending Page" should {

    "display correct heading" in {
      docWithName.body.getElementsByTag("h1").text() must startWith(s"Application sent")
    }
  }

  lazy val docWithName: Document    = Jsoup.parse(contentAsString(view(processedDate, atarService)))
  lazy val docWithoutName: Document = Jsoup.parse(contentAsString(view(processedDate, atarService)))
}
