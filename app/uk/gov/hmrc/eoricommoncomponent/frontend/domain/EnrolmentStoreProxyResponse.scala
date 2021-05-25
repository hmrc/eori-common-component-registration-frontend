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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.{Json, OFormat}

case class EnrolmentStoreProxyResponse(enrolments: List[EnrolmentResponse])

object EnrolmentStoreProxyResponse {
  implicit val jsonFormat = Json.format[EnrolmentStoreProxyResponse]
}

case class EnrolmentResponse(service: String, state: String, identifiers: List[KeyValue]) {
  def eori: Option[String] = identifiers.find(_.key == "EORINumber").map(_.value)
}

object EnrolmentResponse {
  implicit val jsonKeyValueFormat: OFormat[KeyValue] = Json.format[KeyValue]

  implicit val jsonFormat: OFormat[EnrolmentResponse] = Json.format[EnrolmentResponse]
}
