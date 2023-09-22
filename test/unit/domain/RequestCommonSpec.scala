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

package unit.domain

import base.UnitSpec
import play.api.libs.json.{JsResultException, JsValue, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RequestCommon

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

class RequestCommonSpec extends UnitSpec {

  val regime           = "CDS"
  val validReceiptDate = "2016-07-08T08:35:13Z"

  val validReceiptDateBst =
    ZonedDateTime.parse(validReceiptDate).withZoneSameInstant(ZoneId.of("Europe/London")).toLocalDateTime

  val invalidReceiptDate       = "2016-07-08 08:35:13Z"
  val acknowledgementReference = UUID.randomUUID().toString

  def requestCommonAsJson(receiptDate: String = validReceiptDate): JsValue =
    Json.parse(s"""|{
          |  "regime": "$regime",
          |  "receiptDate": "$receiptDate",
          |  "acknowledgementReference": "$acknowledgementReference"
          |}
    """.stripMargin)

  "RequestCommon" should {

    "deserialise from valid json" in {
      val requestCommon = requestCommonAsJson().as[RequestCommon]
      requestCommon.acknowledgementReference should be(acknowledgementReference)
      requestCommon.receiptDate should equal(validReceiptDateBst)
      requestCommon.regime should be(regime)
    }

    "serialise receiptDate to ISO format" in {
      val requestCommon = RequestCommon(regime, validReceiptDateBst, acknowledgementReference)
      val json          = Json.toJson[RequestCommon](requestCommon)
      json should be(requestCommonAsJson())
    }

    "throw exception when receiptDate in Json is not in expected format" in {
      val caught         = intercept[JsResultException](requestCommonAsJson(invalidReceiptDate).as[RequestCommon])
      val expectedPrefix = s"Could not parse "
      val expectedSuffix = s" as an ISO date"
      caught.getMessage should include(expectedPrefix)
      caught.getMessage should include(invalidReceiptDate)
      caught.getMessage should include(expectedSuffix)
    }
  }
}
