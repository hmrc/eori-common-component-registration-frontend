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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  ContactResponse,
  IndividualResponse,
  OrganisationResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, MessagingServiceParam, NonUKIdentification}

case class ResponseDetail(
  SAFEID: String,
  ARN: Option[String] = None,
  nonUKIdentification: Option[NonUKIdentification] = None,
  isEditable: Boolean,
  isAnAgent: Boolean,
  isAnIndividual: Boolean,
  individual: Option[IndividualResponse] = None,
  organisation: Option[OrganisationResponse] = None,
  address: Address,
  contactDetails: ContactResponse
) {
  require(
    isAnIndividual && individual.isDefined && organisation.isEmpty || !isAnIndividual && individual.isEmpty && organisation.isDefined
  )
}

object ResponseDetail {
  implicit val jsonFormat = Json.format[ResponseDetail]
}

case class ResponseCommon(
  status: String,
  statusText: Option[String],
  processingDate: String,
  returnParameters: Option[List[MessagingServiceParam]] = None,
  taxPayerID: Option[String]
)

object ResponseCommon {
  implicit val format = Json.format[ResponseCommon]
}

case class RegistrationDisplayResponse(responseCommon: ResponseCommon, responseDetail: Option[ResponseDetail])

object RegistrationDisplayResponse {
  implicit val format = Json.format[RegistrationDisplayResponse]
}

case class RegistrationDisplayResponseHolder(registrationDisplayResponse: RegistrationDisplayResponse)

object RegistrationDisplayResponseHolder {
  implicit val format = Json.format[RegistrationDisplayResponseHolder]
}
