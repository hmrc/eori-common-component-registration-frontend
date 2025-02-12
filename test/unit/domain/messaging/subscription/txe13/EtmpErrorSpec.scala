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

package unit.domain.messaging.subscription.txe13

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.EtmpErrorCode
import util.Txe13JsonErrors

class EtmpErrorSpec extends AnyFreeSpec with Matchers with OptionValues with Txe13JsonErrors {

  "reads" - {
    "unprocessable entity" - {
      "should read Existing Active Subscription error" in {
        val etmpErrorCode: EtmpErrorCode = Json.parse(existingActiveSubscription).as[EtmpErrorCode]
        val etmpError                    = etmpErrorCode.errorCode
        val etmpErrorValue               = etmpError.value
        val etmpErrorDetail              = etmpErrorValue.errorDetail

        etmpError.summary mustEqual "Business Partner already has an active Subscription"
        etmpErrorDetail.errorCode mustEqual "422"
        etmpErrorDetail.errorMessage mustEqual "Business Partner already has an active Subscription"
        etmpErrorDetail.correlationId mustEqual "1ae81b45-41b4-4642-ae1c-db1126900001"
        etmpErrorDetail.source mustEqual "Backend"
        etmpErrorDetail.sourceFaultDetail.detail mustEqual Seq(
          "007 - Business Partner already has an active Subscription"
        )
        etmpErrorDetail.timestamp mustEqual "2020-09-25T21:54:12.015Z"
      }

      "should read Application Already In Progress error" in {
        val etmpErrorCode: EtmpErrorCode = Json.parse(applicationAlreadyInProgress).as[EtmpErrorCode]
        val etmpError                    = etmpErrorCode.errorCode
        val etmpErrorValue               = etmpError.value
        val etmpErrorDetail              = etmpErrorValue.errorDetail

        etmpError.summary mustEqual "Application already in progress"
        etmpErrorDetail.errorCode mustEqual "422"
        etmpErrorDetail.errorMessage mustEqual "Application already in progress"
        etmpErrorDetail.correlationId mustEqual "1ae81b45-41b4-4642-ae1c-db1126900001"
        etmpErrorDetail.source mustEqual "Backend"
        etmpErrorDetail.sourceFaultDetail.detail mustEqual Seq("133 - Application already in progress")
        etmpErrorDetail.timestamp mustEqual "2020-09-25T21:54:12.015Z"
      }

      "should read Duplicate Submission error" in {
        val etmpErrorCode: EtmpErrorCode = Json.parse(duplicateSubmission).as[EtmpErrorCode]
        val etmpError                    = etmpErrorCode.errorCode
        val etmpErrorValue               = etmpError.value
        val etmpErrorDetail              = etmpErrorValue.errorDetail

        etmpError.summary mustEqual "Duplicate submission reference"
        etmpErrorDetail.errorCode mustEqual "422"
        etmpErrorDetail.errorMessage mustEqual "Duplicate submission reference"
        etmpErrorDetail.correlationId mustEqual "1ae81b45-41b4-4642-ae1c-db1126900001"
        etmpErrorDetail.source mustEqual "Backend"
        etmpErrorDetail.sourceFaultDetail.detail mustEqual Seq("135 - Duplicate submission reference")
        etmpErrorDetail.timestamp mustEqual "2020-09-25T21:54:12.015Z"
      }

      "should read Invalid Edge Case Type error" in {
        val etmpErrorCode: EtmpErrorCode = Json.parse(invalidEdgeCaseType).as[EtmpErrorCode]
        val etmpError                    = etmpErrorCode.errorCode
        val etmpErrorValue               = etmpError.value
        val etmpErrorDetail              = etmpErrorValue.errorDetail

        etmpError.summary mustEqual "Invalid Edge Case Type"
        etmpErrorDetail.errorCode mustEqual "422"
        etmpErrorDetail.errorMessage mustEqual "Edge Case Type missing/invalid"
        etmpErrorDetail.correlationId mustEqual "1ae81b45-41b4-4642-ae1c-db1126900001"
        etmpErrorDetail.source mustEqual "Backend"
        etmpErrorDetail.sourceFaultDetail.detail mustEqual Seq("131 - Edge Case Type missing/invalid")
        etmpErrorDetail.timestamp mustEqual "2020-09-25T21:54:12.015Z"
      }

      "should read Invalid Regime error" in {
        val etmpErrorCode: EtmpErrorCode = Json.parse(invalidRegime).as[EtmpErrorCode]
        val etmpError                    = etmpErrorCode.errorCode
        val etmpErrorValue               = etmpError.value
        val etmpErrorDetail              = etmpErrorValue.errorDetail

        etmpError.summary mustEqual "Invalid Regime"
        etmpErrorDetail.errorCode mustEqual "422"
        etmpErrorDetail.errorMessage mustEqual "REGIME missing or invalid"
        etmpErrorDetail.correlationId mustEqual "1ae81b45-41b4-4642-ae1c-db1126900001"
        etmpErrorDetail.source mustEqual "Backend"
        etmpErrorDetail.sourceFaultDetail.detail mustEqual Seq("001 - REGIME missing or invalid")
        etmpErrorDetail.timestamp mustEqual "2020-09-25T21:54:12.015Z"
      }
    }
  }
}
