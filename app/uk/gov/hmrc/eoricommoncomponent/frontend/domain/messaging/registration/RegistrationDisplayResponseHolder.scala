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

import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{ContactResponse, IndividualResponse, OrganisationResponse}
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
  implicit val jsonFormat: OFormat[ResponseDetail] = Json.format[ResponseDetail]
}

case class ResponseCommon(
  status: String,
  statusText: Option[String],
  processingDate: String,
  returnParameters: Option[List[MessagingServiceParam]] = None,
  taxPayerID: Option[String]
)

object ResponseCommon {
  implicit val format: OFormat[ResponseCommon] = Json.format[ResponseCommon]
}

case class RegistrationDisplayResponse(responseCommon: ResponseCommon, responseDetail: Option[ResponseDetail]) extends Logging {

  def getResponseDetail: ResponseDetail = responseDetail match {
    case Some(detail) => detail
    case None =>
      val error = "RegistrationDisplayResponse did not include expected ResponseDetail object"
      // $COVERAGE-OFF$Loggers
      logger.warn(error)
      // $COVERAGE-ON
      throw new IllegalArgumentException(error)
  }

}

object RegistrationDisplayResponse {
  implicit val format: OFormat[RegistrationDisplayResponse] = Json.format[RegistrationDisplayResponse]
}

case class RegistrationDisplayResponseHolder(registrationDisplayResponse: RegistrationDisplayResponse)

object RegistrationDisplayResponseHolder {
  implicit val format: OFormat[RegistrationDisplayResponseHolder] = Json.format[RegistrationDisplayResponseHolder]
}
