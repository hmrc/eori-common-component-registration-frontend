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

package unit.domain.messaging

import base.UnitSpec
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.IndividualResponse

class IndividualResponseFormatSpec extends UnitSpec {

  import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.IndividualResponse.formats

  val individualResponseFull          = IndividualResponse("John", Some("Middle"), "Doe", Some("1999-12-20"))
  val individualResponseNoMiddle      = individualResponseFull.copy(middleName = None)
  val individualResponseNoDate        = individualResponseFull.copy(dateOfBirth = None)
  val individualResponseOnlyMandatory = individualResponseFull.copy(middleName = None, dateOfBirth = None)

  val individualResponseJsonFull = Json.parse("""
      |      {
      |        "firstName": "John",
      |        "middleName": "Middle",
      |        "lastName": "Doe",
      |        "dateOfBirth": "1999-12-20"
      |      }
    """.stripMargin)

  val individualResponseJsonNoMiddle = Json.parse("""
      |      {
      |        "firstName": "John",
      |        "lastName": "Doe",
      |        "dateOfBirth": "1999-12-20"
      |      }
    """.stripMargin)

  val individualResponseJsonNoDate = Json.parse("""
      |      {
      |        "firstName": "John",
      |        "middleName": "Middle",
      |        "lastName": "Doe"
      |      }
    """.stripMargin)

  val individualResponseJsonOnlyMandatory = Json.parse("""
      |      {
      |        "firstName": "John",
      |        "lastName": "Doe"
      |      }
    """.stripMargin)

  "IndividualResponse formats" should {

    s"marshall full individual response" in {
      Json.prettyPrint(marshall(individualResponseFull)) shouldBe Json.prettyPrint(individualResponseJsonFull)
    }

    s"unmarshall full individual response" in {
      unmarshall(individualResponseJsonFull) shouldBe JsSuccess(individualResponseFull)
    }

    s"marshall individual response without middle name" in {
      Json.prettyPrint(marshall(individualResponseNoMiddle)) shouldBe Json.prettyPrint(individualResponseJsonNoMiddle)
    }

    s"unmarshall individual response without middle name" in {
      unmarshall(individualResponseJsonNoMiddle) shouldBe JsSuccess(individualResponseNoMiddle)
    }

    s"marshall individual response without date of birth" in {
      Json.prettyPrint(marshall(individualResponseNoDate)) shouldBe Json.prettyPrint(individualResponseJsonNoDate)
    }

    s"unmarshall individual response without date of birth" in {
      unmarshall(individualResponseJsonNoDate) shouldBe JsSuccess(individualResponseNoDate)
    }

    s"marshall individual response with mandatory fields only" in {
      Json.prettyPrint(marshall(individualResponseOnlyMandatory)) shouldBe Json.prettyPrint(
        individualResponseJsonOnlyMandatory
      )
    }

    s"unmarshall individual response with mandatory fields only" in {
      unmarshall(individualResponseJsonOnlyMandatory) shouldBe JsSuccess(individualResponseOnlyMandatory)
    }
  }

  private def marshall(data: IndividualResponse): JsValue = Json.toJson(data)

  private def unmarshall(js: JsValue): JsResult[IndividualResponse] = Json.fromJson(js)
}
