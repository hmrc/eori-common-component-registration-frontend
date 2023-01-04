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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, MessagingServiceParam}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  IndividualResponse,
  MatchingResponse,
  OrganisationResponse
}

case class RegisterWithIdConfirmation(
  status: String,
  safeId: Option[String],
  arn: Option[String],
  isEditable: Option[Boolean],
  isAnAgent: Option[Boolean],
  isAnIndividual: Option[Boolean],
  organisationAsAGroup: Option[Boolean],
  individual: Option[IndividualResponse],
  organisation: Option[OrganisationResponse],
  address: Option[Address],
  processingDate: String,
  returnParameters: Option[List[MessagingServiceParam]]
)

object RegisterWithIdConfirmation {
  implicit val format = Json.format[RegisterWithIdConfirmation]

  def apply(response: MatchingResponse): RegisterWithIdConfirmation = {

    val responseCommon = response.registerWithIDResponse.responseCommon
    val responseDetail = response.registerWithIDResponse.responseDetail

    RegisterWithIdConfirmation(
      status = responseCommon.status,
      safeId = responseDetail.map(_.SAFEID),
      arn = responseDetail.flatMap(_.ARN),
      isEditable = responseDetail.map(_.isEditable),
      isAnAgent = responseDetail.map(_.isAnAgent),
      isAnIndividual = responseDetail.map(_.isAnIndividual),
      organisationAsAGroup = responseDetail.flatMap(_.organisation.flatMap(_.isAGroup)),
      individual = responseDetail.flatMap(_.individual),
      organisation = responseDetail.flatMap(_.organisation),
      address = responseDetail.map(_.address),
      processingDate = responseCommon.processingDate.toString,
      returnParameters = responseCommon.returnParameters
    )
  }

}
