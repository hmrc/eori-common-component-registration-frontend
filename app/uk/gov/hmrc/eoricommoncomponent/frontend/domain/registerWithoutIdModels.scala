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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel

case class OrganisationName(organisationName: String)

object OrganisationName {
  implicit val format: OFormat[OrganisationName] = Json.format[OrganisationName]
}

case class RegisterWithoutIdContactDetails(
  phoneNumber: Option[String] = None,
  mobileNumber: Option[String] = None,
  faxNumber: Option[String] = None,
  emailAddress: Option[String] = None
)

object RegisterWithoutIdContactDetails {
  implicit val jsonFormat: OFormat[RegisterWithoutIdContactDetails] = Json.format[RegisterWithoutIdContactDetails]
}

case class RegisterWithoutIdReqDetails(
  contactDetails: RegisterWithoutIdContactDetails = RegisterWithoutIdContactDetails(),
  address: Address,
  organisation: Option[OrganisationName] = None,
  individual: Option[Individual] = None
) {
  require(organisation.isDefined ^ individual.isDefined)
}

object RegisterWithoutIdReqDetails {
  implicit val format: OFormat[RegisterWithoutIdReqDetails] = Json.format[RegisterWithoutIdReqDetails]

  def organisation(
    organisation: OrganisationName,
    address: Address,
    contactDetail: ContactDetailsModel
  ): RegisterWithoutIdReqDetails = {
    val cd = RegisterWithoutIdContactDetails(
      Some(contactDetail.telephone),
      mobileNumber = None,
      faxNumber = contactDetail.fax,
      emailAddress = Some(contactDetail.emailAddress)
    )
    RegisterWithoutIdReqDetails(cd, address, Some(organisation))
  }

  def individual(
    individual: Individual,
    address: Address,
    contactDetail: ContactDetailsModel
  ): RegisterWithoutIdReqDetails = {
    val cd = RegisterWithoutIdContactDetails(
      Some(contactDetail.telephone),
      mobileNumber = None,
      faxNumber = contactDetail.fax,
      emailAddress = Some(contactDetail.emailAddress)
    )
    RegisterWithoutIdReqDetails(cd, address = address, individual = Some(individual))
  }

}

case class RegisterWithoutIDRequest(requestCommon: RequestCommon, requestDetail: RegisterWithoutIdReqDetails)

object RegisterWithoutIDRequest {
  implicit val format: OFormat[RegisterWithoutIDRequest] = Json.format[RegisterWithoutIDRequest]
}

case class RegisterWithoutIdRequestHolder(registerWithoutIDRequest: RegisterWithoutIDRequest)

object RegisterWithoutIdRequestHolder {
  implicit val format: OFormat[RegisterWithoutIdRequestHolder] = Json.format[RegisterWithoutIdRequestHolder]
}

case class RegisterWithoutIdResponseDetail(SAFEID: String, ARN: Option[String])

object RegisterWithoutIdResponseDetail {
  implicit val formats: OFormat[RegisterWithoutIdResponseDetail] = Json.format[RegisterWithoutIdResponseDetail]
}

case class RegisterWithoutIDResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[RegisterWithoutIdResponseDetail]
)

object RegisterWithoutIDResponse {
  implicit val format: OFormat[RegisterWithoutIDResponse] = Json.format[RegisterWithoutIDResponse]
}

case class RegisterWithoutIdResponseHolder(registerWithoutIDResponse: RegisterWithoutIDResponse)

object RegisterWithoutIdResponseHolder {
  implicit val format: OFormat[RegisterWithoutIdResponseHolder] = Json.format[RegisterWithoutIdResponseHolder]
}
