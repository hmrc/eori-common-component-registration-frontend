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

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  ContactResponse,
  IndividualResponse,
  OrganisationResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.RegistrationDisplayResponseHolder
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, NonUKIdentification}

case class RegistrationDisplayResult(
  safeId: Option[String],
  status: String,
  statusText: Option[String],
  processingDate: String,
  arn: Option[String],
  nonUKIdentification: Option[NonUKIdentification],
  isEditable: Option[Boolean],
  isAnAgent: Option[Boolean],
  isAnIndividual: Option[Boolean],
  individual: Option[IndividualResponse],
  organisation: Option[OrganisationResponse],
  address: Option[Address],
  contactDetails: Option[ContactResponse]
)

object RegistrationDisplayResult {
  implicit val format = Json.format[RegistrationDisplayResult]

  def apply(response: RegistrationDisplayResponseHolder): RegistrationDisplayResult =
    RegistrationDisplayResult(
      safeId = response.registrationDisplayResponse.responseDetail.map(_.SAFEID),
      status = response.registrationDisplayResponse.responseCommon.status,
      statusText = response.registrationDisplayResponse.responseCommon.statusText,
      processingDate = response.registrationDisplayResponse.responseCommon.processingDate,
      arn = response.registrationDisplayResponse.responseDetail.flatMap(_.ARN),
      nonUKIdentification = response.registrationDisplayResponse.responseDetail.flatMap(_.nonUKIdentification),
      isEditable = response.registrationDisplayResponse.responseDetail.map(_.isEditable),
      isAnAgent = response.registrationDisplayResponse.responseDetail.map(_.isAnAgent),
      isAnIndividual = response.registrationDisplayResponse.responseDetail.map(_.isAnIndividual),
      individual = response.registrationDisplayResponse.responseDetail.flatMap(_.individual),
      organisation = response.registrationDisplayResponse.responseDetail.flatMap(_.organisation),
      address = response.registrationDisplayResponse.responseDetail.map(_.address),
      contactDetails = response.registrationDisplayResponse.responseDetail.map(_.contactDetails)
    )

}
