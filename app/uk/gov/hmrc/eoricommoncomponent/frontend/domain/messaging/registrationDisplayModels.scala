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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._

case class RegistrationInfoRequest(regime: String = "CDS", idType: String, idValue: String)

object RegistrationInfoRequest {
  implicit val jsonFormat = Json.format[RegistrationInfoRequest]

  val UTR  = "UTR"
  val EORI = "EORI"
  val NINO = "NINO"
  val SAFE = "SAFE"

  def forCustomsId(customsId: CustomsId): RegistrationInfoRequest = {
    val idType = customsId match {
      case _: Eori   => EORI
      case _: Utr    => UTR
      case _: Nino   => NINO
      case _: SafeId => SAFE
      case _: TaxPayerId =>
        throw new IllegalArgumentException("TaxPayerId is not supported by RegistrationInfo service")
    }
    RegistrationInfoRequest(idType = idType, idValue = customsId.id)
  }

}

case class RegistrationInfoResponseCommon(status: String, processingDate: DateTime, taxPayerID: String)

object RegistrationInfoResponseCommon extends CommonHeader {
  implicit val jsonFormat = Json.format[RegistrationInfoResponseCommon]
}

case class RegistrationInfoIndividual(
  firstName: String,
  middleName: Option[String],
  lastName: String,
  dateOfBirth: Option[String]
) extends IndividualName

object RegistrationInfoIndividual {
  implicit val jsonFormat = Json.format[RegistrationInfoIndividual]
}

case class NonUKIdentification(IDNumber: String, issuingInstitution: String, issuingCountryCode: String)

object NonUKIdentification {
  implicit val jsonFormat = Json.format[NonUKIdentification]
}

case class RegistrationInfoOrganisation(
  organisationName: String,
  isAGroup: Boolean,
  organisationType: Option[String],
  code: Option[String]
)

object RegistrationInfoOrganisation {
  implicit val jsonFormat = Json.format[RegistrationInfoOrganisation]
}

case class RegistrationInfoAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postalCode: Option[String],
  countryCode: String
)

object RegistrationInfoAddress {
  implicit val jsonFormat = Json.format[RegistrationInfoAddress]
}

case class RegistrationInfoContactDetails(
  phoneNumber: Option[String],
  mobileNumber: Option[String],
  faxNumber: Option[String],
  emailAddress: Option[String]
)

object RegistrationInfoContactDetails {
  implicit val jsonFormat = Json.format[RegistrationInfoContactDetails]
}

case class RegistrationInfoResponseDetail(
  SAFEID: String,
  ARN: Option[String],
  nonUKIdentification: Option[NonUKIdentification],
  isEditable: Boolean,
  isAnAgent: Boolean,
  isAnIndividual: Boolean,
  individual: Option[RegistrationInfoIndividual],
  organisation: Option[RegistrationInfoOrganisation],
  address: RegistrationInfoAddress,
  contactDetails: RegistrationInfoContactDetails
) {
  require(
    isAnIndividual && individual.isDefined && organisation.isEmpty || !isAnIndividual && individual.isEmpty && organisation.isDefined
  )
}

object RegistrationInfoResponseDetail {
  implicit val jsonFormat = Json.format[RegistrationInfoResponseDetail]
}

case class RegistrationInfoResponse(
  responseCommon: RegistrationInfoResponseCommon,
  responseDetail: RegistrationInfoResponseDetail
)

object RegistrationInfoResponse {
  implicit val jsonFormat = Json.format[RegistrationInfoResponse]
}

case class RegistrationInfoResponseHolder(registrationDisplayResponse: RegistrationInfoResponse)

object RegistrationInfoResponseHolder {
  implicit val jsonFormat = Json.format[RegistrationInfoResponseHolder]
}
