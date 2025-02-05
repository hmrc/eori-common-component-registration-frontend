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

package util

trait Txe13JsonErrors {

  val existingActiveSubscription: String =
    """
      |{
      |  "errorcode007": {
      |    "summary": "Business Partner already has an active Subscription",
      |    "value": {
      |      "errorDetail": {
      |        "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001",
      |        "errorCode": "422",
      |        "errorMessage": "Business Partner already has an active Subscription",
      |        "source": "Backend",
      |        "sourceFaultDetail": {
      |          "detail": [
      |            "007 - Business Partner already has an active Subscription"
      |          ]
      |        },
      |        "timestamp": "2020-09-25T21:54:12.015Z"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val applicationAlreadyInProgress: String =
    """
      |{
      |  "errorcode133": {
      |    "summary": "Application already in progress",
      |    "value": {
      |      "errorDetail": {
      |        "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001",
      |        "errorCode": "422",
      |        "errorMessage": "Application already in progress",
      |        "source": "Backend",
      |        "sourceFaultDetail": {
      |          "detail": [
      |            "133 - Application already in progress"
      |          ]
      |        },
      |        "timestamp": "2020-09-25T21:54:12.015Z"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val duplicateSubmission: String =
    """
      |{
      |  "errorcode135": {
      |    "summary": "Duplicate submission reference",
      |    "value": {
      |      "errorDetail": {
      |        "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001",
      |        "errorCode": "422",
      |        "errorMessage": "Duplicate submission reference",
      |        "source": "Backend",
      |        "sourceFaultDetail": {
      |          "detail": [
      |            "135 - Duplicate submission reference"
      |          ]
      |        },
      |        "timestamp": "2020-09-25T21:54:12.015Z"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val invalidEdgeCaseType: String =
    """
      |{
      |  "invalidEdgeCaseType": {
      |    "summary": "Invalid Edge Case Type",
      |    "value": {
      |      "errorDetail": {
      |        "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001",
      |        "errorCode": "422",
      |        "errorMessage": "Edge Case Type missing/invalid",
      |        "source": "Backend",
      |        "sourceFaultDetail": {
      |          "detail": [
      |            "131 - Edge Case Type missing/invalid"
      |          ]
      |        },
      |        "timestamp": "2020-09-25T21:54:12.015Z"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  val invalidRegime: String =
    """
      |{
      |  "invalidRegime": {
      |    "summary": "Invalid Regime",
      |    "value": {
      |      "errorDetail": {
      |        "correlationId": "1ae81b45-41b4-4642-ae1c-db1126900001",
      |        "errorCode": "422",
      |        "errorMessage": "REGIME missing or invalid",
      |        "source": "Backend",
      |        "sourceFaultDetail": {
      |          "detail": [
      |            "001 - REGIME missing or invalid"
      |          ]
      |        },
      |        "timestamp": "2020-09-25T21:54:12.015Z"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

}
