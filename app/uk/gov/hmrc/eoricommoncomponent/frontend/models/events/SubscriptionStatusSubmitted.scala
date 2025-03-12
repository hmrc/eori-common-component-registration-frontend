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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.events

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RegistrationInfoRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CustomsId, SubscriptionStatusQueryParams}

case class SubscriptionStatusSubmitted(
  taxPayerId: Option[String],
  safeId: Option[String],
  receiptDate: String,
  regime: String,
  originatingService: String
)

object SubscriptionStatusSubmitted {
  implicit val format: OFormat[SubscriptionStatusSubmitted] = Json.format[SubscriptionStatusSubmitted]

  def apply(request: SubscriptionStatusQueryParams, originatingService: String): SubscriptionStatusSubmitted =
    SubscriptionStatusSubmitted(
      taxPayerId = if (request.idType == CustomsId.taxPayerId) Some(request.id) else None,
      safeId = if (request.idType == RegistrationInfoRequest.SAFE) Some(request.id) else None,
      receiptDate = request.receiptDate.toString,
      regime = request.regime,
      originatingService = originatingService
    )

}
