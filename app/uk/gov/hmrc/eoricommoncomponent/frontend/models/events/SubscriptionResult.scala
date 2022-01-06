/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.MessagingServiceParam
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionResponse

case class SubscriptionResult(
  status: String,
  processingDate: String,
  statusText: Option[String],
  eori: Option[String],
  returnParameters: List[MessagingServiceParam]
)

object SubscriptionResult {
  implicit val format = Json.format[SubscriptionResult]

  def apply(response: SubscriptionResponse): SubscriptionResult =
    SubscriptionResult(
      status = response.subscriptionCreateResponse.responseCommon.status,
      processingDate = response.subscriptionCreateResponse.responseCommon.processingDate.toString,
      statusText = response.subscriptionCreateResponse.responseCommon.statusText,
      eori = response.subscriptionCreateResponse.responseDetail.map(_.EORINo),
      returnParameters = response.subscriptionCreateResponse.responseCommon.returnParameters
    )

}
