/*
 * Copyright 2026 HM Revenue & Customs
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

package unit.domain.messaging.subscription.txe13

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionResponse

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CreateEoriSubscriptionResponseSpec extends AnyWordSpec with Matchers with OptionValues {

  val successResponse: String =
    """
      |{
      |  "success": {
      |    "formBundleNumber": "93000022142",
      |    "position": "WORKLIST",
      |    "processingDate": "2023-11-28T10:15:10Z",
      |    "safeId": "XR0000100051093"
      |  }
      |}
      |""".stripMargin

  "reads" should {
    "read body" in {
      val createEoriSubscriptionResponse = Json.parse(successResponse).as[CreateEoriSubscriptionResponse]
      val innerBody = createEoriSubscriptionResponse.success
      innerBody.safeId mustEqual "XR0000100051093"
      innerBody.position mustEqual "WORKLIST"
      innerBody.formBundleNumber mustEqual "93000022142"
      innerBody.processingDate mustEqual LocalDateTime.parse("2023-11-28T10:15:10Z", DateTimeFormatter.ISO_DATE_TIME)
    }
  }
}
