/*
 * Copyright 2025 HM Revenue & Customs
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

package unit.domain.email

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.RequestDetail

import java.time.LocalDateTime

class RequestDetailSpec extends AnyFreeSpec with Matchers {

  private val dateTime: LocalDateTime = LocalDateTime.of(2001, 12, 17, 9, 30, 47, 0)
  "RequestDetail" - {
    "serialise to json" in {

      val requestDetail = RequestDetail("idType", "idNumber", "test@email.com", dateTime)
      Json
        .toJson(
          requestDetail
        )
        .toString() mustBe """{"IDType":"idType","IDNumber":"idNumber","emailAddress":"test@email.com","emailVerificationTimestamp":"2001-12-17T09:30:47Z"}"""
    }

    "de-serialise from json" in {
      val json = Json.parse(
        """{"IDType":"idType","IDNumber":"idNumber","emailAddress":"test@email.com","emailVerificationTimestamp":"2001-12-17T09:30:47Z"}""".stripMargin
      )

      json.as[RequestDetail] mustBe RequestDetail("idType", "idNumber", "test@email.com", dateTime)
    }

    "expect exception on failing to read json" in {
      val json = Json.parse(
        """{"IDType":"idType","IDNumber":"idNumber","emailAddress":"test@email.com","emailVerificationTimestamp":"2001-12-17111T09:30:47Z"}""".stripMargin
      )

      val exception = intercept[JsResultException](json.as[RequestDetail])
      exception.getMessage mustBe "JsResultException(errors:List((/emailVerificationTimestamp,List(JsonValidationError(List(Could not parse '\"2001-12-17111T09:30:47Z\"' as an ISO date. Reason: Text '2001-12-17111T09:30:47Z' could not be parsed at index 10),ArraySeq())))))"
    }
  }
}
