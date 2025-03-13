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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Json, OFormat, Writes}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.CustomsDataStoreRequest
import uk.gov.hmrc.http.HttpResponse

case class UpdateRequest(eori: String, address: String, timestamp: String)

object UpdateRequest {
  implicit val format: OFormat[UpdateRequest] = Json.format[UpdateRequest]

  def apply(request: CustomsDataStoreRequest): UpdateRequest =
    UpdateRequest(eori = request.eori, address = request.address, timestamp = request.timestamp)

}

case class UpdateResponse(status: String)

object UpdateResponse {
  implicit val format: OFormat[UpdateResponse] = Json.format[UpdateResponse]

  def apply(response: HttpResponse): UpdateResponse =
    UpdateResponse(status = response.status.toString)

}

case class CustomsDataStoreUpdate(request: UpdateRequest, response: UpdateResponse)

object CustomsDataStoreUpdate {

  // writes for flattened Audit event
  implicit val writes: Writes[CustomsDataStoreUpdate] =
    JsPath
      .write[UpdateRequest]
      .and(JsPath.write[UpdateResponse])(unlift(CustomsDataStoreUpdate.unapply))

}
