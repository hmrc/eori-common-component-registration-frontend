/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{CommonHeader, RequestParameter}

import java.time.LocalDateTime

case class RequestCommon(receiptDate: LocalDateTime, requestParameters: Seq[RequestParameter])

object RequestCommon extends CommonHeader {
  implicit val format: OFormat[RequestCommon] = Json.format[RequestCommon]
}

case class RegistrationDisplayRequest(requestCommon: RequestCommon)

object RegistrationDisplayRequest {
  implicit val format: OFormat[RegistrationDisplayRequest] = Json.format[RegistrationDisplayRequest]
}

case class RegistrationDisplayRequestHolder(registrationDisplayRequest: RegistrationDisplayRequest)

object RegistrationDisplayRequestHolder {
  implicit val format: OFormat[RegistrationDisplayRequestHolder] = Json.format[RegistrationDisplayRequestHolder]
}
