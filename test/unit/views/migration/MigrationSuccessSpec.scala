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

package unit.views.migration

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.migration_success
import util.ViewSpec

class MigrationSuccessSpec extends ViewSpec {

  implicit val request = withFakeCSRF(fakeAtarSubscribeRequest)

  private val view = instanceOf[migration_success]

  "'Migration Success' Page" should {

    "have a feedback 'continue' button" in {
      val link = doc().body.getElementById("feedback-continue")
      link.text mustBe "More about Advance Tariff Rulings"
      link.attr("href") mustBe "/test-atar/feedback?status=Processing"
    }

    "have a no feedback 'continue' button when config missing" in {
      val link = doc(atarService.copy(feedbackUrl = None)).body.getElementById("feedback-continue")
      link mustBe null
    }
  }

  def doc(service: Service = atarService): Document =
    Jsoup.parse(contentAsString(view(Some("GB1231233122"), "name", "", service)))

}
