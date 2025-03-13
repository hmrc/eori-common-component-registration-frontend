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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SafeId
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionResponse

import java.time.LocalDateTime

sealed trait EoriHttpResponse

case object NotFoundResponse extends EoriHttpResponse

case object InvalidResponse extends EoriHttpResponse

case object ServiceUnavailableResponse extends EoriHttpResponse

case class SuccessResponse(formBundleNumber: String, safeId: SafeId, processingDate: LocalDateTime) extends EoriHttpResponse

object SuccessResponse {
  def apply(response: CreateEoriSubscriptionResponse): SuccessResponse = {
    SuccessResponse(response.success.formBundleNumber, SafeId(response.success.safeId), response.success.processingDate)
  }
}

case object ErrorResponse extends EoriHttpResponse
