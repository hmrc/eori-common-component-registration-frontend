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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel

case class GovGatewayCredentials(email: String)

object GovGatewayCredentials {
  implicit val format = Json.format[GovGatewayCredentials]
}

case class EstablishmentAddress(
  streetAndNumber: String,
  city: String,
  postalCode: Option[String] = None,
  countryCode: String
) {

  def updateCountryFromAddress(address: AddressViewModel): EstablishmentAddress =
    this.copy(countryCode = address.countryCode)

}

object EstablishmentAddress {
  implicit val jsonFormat = Json.format[EstablishmentAddress]

  def createEstablishmentAddress(address: Address): EstablishmentAddress = {
    val fourLineAddress = AddressViewModel(address)
    new EstablishmentAddress(
      fourLineAddress.street,
      fourLineAddress.city,
      address.postalCode.filterNot(p => p.isEmpty),
      fourLineAddress.countryCode
    )
  }

}

case class VatIds(countryCode: String, vatNumber: String)

object VatIds {
  implicit val format = Json.format[VatIds]
}

case class ContactDetail(
  address: EstablishmentAddress,
  contactName: String,
  phone: Option[String],
  fax: Option[String],
  email: Option[String]
)

object ContactDetail {
  implicit val format = Json.format[ContactDetail]
}

case class Trader(fullName: String, shortName: String)

object Trader {
  implicit val format = Json.format[Trader]
}

case class ResponseData(
  SAFEID: String,
  trader: Trader,
  establishmentAddress: EstablishmentAddress,
  contactDetail: Option[ContactDetail] = None,
  VATIDs: Option[Seq[VatIds]] = None,
  hasInternetPublication: Boolean,
  principalEconomicActivity: Option[String] = None,
  hasEstablishmentInCustomsTerritory: Option[Boolean] = None,
  legalStatus: Option[String] = None,
  thirdCountryIDNumber: Option[Seq[String]] = None,
  dateOfEstablishmentBirth: Option[String] = None,
  personType: Option[Int] = None,
  startDate: String,
  expiryDate: Option[String] = None
)

object ResponseData {
  implicit val format = Json.format[ResponseData]
}

case class RegisterWithEoriAndIdResponseDetail(
  outcome: Option[String],
  caseNumber: Option[String],
  responseData: Option[ResponseData] = None
)

object RegisterWithEoriAndIdResponseDetail {
  implicit val format = Json.format[RegisterWithEoriAndIdResponseDetail]
}

case class AdditionalInformation(id: CustomsId, isIndividual: Boolean)

object AdditionalInformation {
  implicit val format = Json.format[AdditionalInformation]
}

case class RegisterWithEoriAndIdResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[RegisterWithEoriAndIdResponseDetail],
  additionalInformation: Option[AdditionalInformation] = None
)

object RegisterWithEoriAndIdResponse {
  implicit val format = Json.format[RegisterWithEoriAndIdResponse]
}
