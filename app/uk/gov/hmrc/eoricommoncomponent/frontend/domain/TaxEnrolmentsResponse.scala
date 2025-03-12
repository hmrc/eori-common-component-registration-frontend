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

import java.time.format.DateTimeFormatter

case class TaxEnrolmentsResponse(serviceName: String)

object TaxEnrolmentsResponse {
  implicit val jsonFormat: OFormat[TaxEnrolmentsResponse] = Json.format[TaxEnrolmentsResponse]
}

case class TaxEnrolmentsRequest(
  serviceName: String,
  identifiers: List[KeyValue],
  verifiers: Option[List[KeyValue]] = None,
  subscriptionState: String
)

object TaxEnrolmentsRequest {
  val pattern: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  implicit val jsonKeyValueFormat: OFormat[KeyValue] = Json.format[KeyValue]

  implicit val jsonFormat: OFormat[TaxEnrolmentsRequest] = Json.format[TaxEnrolmentsRequest]
}
