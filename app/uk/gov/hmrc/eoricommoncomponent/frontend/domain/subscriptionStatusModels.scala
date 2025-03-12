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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.CommonHeader

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset}

case class SubscriptionStatusQueryParams(receiptDate: LocalDateTime, regime: String, idType: String, id: String) {

  def queryParams: Seq[(String, String)] = {
    val receiptDateAsString =
      receiptDate.atZone(ZoneId.of("Europe/London")).withZoneSameInstant(ZoneOffset.UTC).withNano(0).format(
        DateTimeFormatter.ISO_DATE_TIME
      )

    Seq("receiptDate" -> receiptDateAsString, "regime" -> regime, idType -> id)
  }

}

case class SubscriptionStatusResponseCommon(status: String, processingDate: LocalDateTime)

object SubscriptionStatusResponseCommon extends CommonHeader {
  implicit val jsonFormat: OFormat[SubscriptionStatusResponseCommon] = Json.format[SubscriptionStatusResponseCommon]
}

case class SubscriptionStatusResponseDetail(subscriptionStatus: String, idValue: Option[String])

object SubscriptionStatusResponseDetail {
  implicit val jsonFormat: OFormat[SubscriptionStatusResponseDetail] = Json.format[SubscriptionStatusResponseDetail]
}

case class SubscriptionStatusResponse(
  responseCommon: SubscriptionStatusResponseCommon,
  responseDetail: SubscriptionStatusResponseDetail
)

object SubscriptionStatusResponse {
  implicit val jsonFormat: OFormat[SubscriptionStatusResponse] = Json.format[SubscriptionStatusResponse]
}

case class SubscriptionStatusResponseHolder(subscriptionStatusResponse: SubscriptionStatusResponse)

object SubscriptionStatusResponseHolder {
  implicit val jsonFormat: OFormat[SubscriptionStatusResponseHolder] = Json.format[SubscriptionStatusResponseHolder]
}
