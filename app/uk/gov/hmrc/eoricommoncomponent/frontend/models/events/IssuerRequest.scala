/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{KeyValue, TaxEnrolmentsRequest}

case class IssuerRequest(
  serviceName: String,
  identifiers: List[KeyValue],
  verifiers: Option[List[KeyValue]],
  supscriptionState: String
)

object IssuerRequest {
  implicit val format: OFormat[IssuerRequest] = Json.format[IssuerRequest]

  def apply(request: TaxEnrolmentsRequest): IssuerRequest =
    IssuerRequest(
      serviceName = request.serviceName,
      identifiers = request.identifiers,
      verifiers = request.verifiers,
      supscriptionState = request.subscriptionState
    )

}
