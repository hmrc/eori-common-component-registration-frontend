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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel

case class GovGatewayCredentials(email: String)

object GovGatewayCredentials {
  implicit val format = Json.format[GovGatewayCredentials]
}

case class RegisterWithEoriAndIdRequestCommon(
  receiptDate: DateTime,
  acknowledgementReference: String,
  requestParameters: Option[Seq[RequestParameter]] = None
)

object RegisterWithEoriAndIdRequestCommon extends CommonHeader {
  implicit val format             = Json.format[RegisterWithEoriAndIdRequestCommon]
  implicit val requestParamFormat = Json.format[RequestParameter]
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

case class RegisterModeEori(EORI: String, fullName: String, address: EstablishmentAddress)

object RegisterModeEori {
  implicit val format = Json.format[RegisterModeEori]
}

case class RegisterWithEoriAndIdOrganisation(name: String, `type`: String)

object RegisterWithEoriAndIdOrganisation {
  implicit val formats = Json.format[RegisterWithEoriAndIdOrganisation]
}

case class RegisterModeId(
  IDType: String,
  IDNumber: String,
  isNameMatched: Boolean,
  individual: Option[Individual] = None,
  organisation: Option[RegisterWithEoriAndIdOrganisation] = None
) {
  require(individual.isDefined ^ organisation.isDefined)
}

object RegisterModeId {
  implicit val format = Json.format[RegisterModeId]
}

case class RegisterWithEoriAndIdDetail(
  registerModeEORI: RegisterModeEori,
  registerModeID: RegisterModeId,
  govGatewayCredentials: Option[GovGatewayCredentials]
)

object RegisterWithEoriAndIdDetail {
  implicit val format = Json.format[RegisterWithEoriAndIdDetail]
}

case class RegisterWithEoriAndIdRequest(requestCommon: RequestCommon, requestDetail: RegisterWithEoriAndIdDetail)

object RegisterWithEoriAndIdRequest {
  implicit val format = Json.format[RegisterWithEoriAndIdRequest]
}

case class RegisterWithEoriAndIdRequestHolder(registerWithEORIAndIDRequest: RegisterWithEoriAndIdRequest)

object RegisterWithEoriAndIdRequestHolder {
  implicit val format = Json.format[RegisterWithEoriAndIdRequestHolder]
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
) {

  def isDoE: Boolean = {
    val doe = for {
      res  <- responseDetail
      data <- res.responseData
      doe  <- data.dateOfEstablishmentBirth
    } yield doe
    doe.isDefined
  }

  def isPersonType: Boolean = {
    val pt = for {
      res  <- responseDetail
      data <- res.responseData
      pt   <- data.personType
    } yield pt
    pt.isDefined
  }

  def isResponseData: Boolean = {
    val data = for {
      res  <- responseDetail
      data <- res.responseData
    } yield data
    data.isDefined
  }

  def withPersonType(typeOfPerson: Option[String]): Option[RegisterWithEoriAndIdResponse] =
    for {
      res  <- responseDetail
      data <- res.responseData.map(_.copy(personType = typeOfPerson.map(_.toInt)))
    } yield this.copy(responseDetail = Some(res.copy(responseData = Some(data))))

  def withDateOfEstablishment(dob: Option[String]): Option[RegisterWithEoriAndIdResponse] =
    for {
      res  <- responseDetail
      data <- res.responseData.map(_.copy(dateOfEstablishmentBirth = dob))
    } yield this.copy(responseDetail = Some(res.copy(responseData = Some(data))))

  def withAdditionalInfo(request: RegisterModeId): RegisterWithEoriAndIdResponse = {
    val customsId             = CustomsId(request.IDType, request.IDNumber)
    val additionalInformation = AdditionalInformation(customsId, request.individual.isDefined)
    this.copy(additionalInformation = Some(additionalInformation))
  }

}

object RegisterWithEoriAndIdResponse {
  implicit val format            = Json.format[RegisterWithEoriAndIdResponse]
  val EoriAlreadyLinked          = "600 - EORI already linked to a different ID"
  val IDLinkedWithEori           = "602 - ID already linked to a different EORI"
  val RejectedPreviouslyAndRetry = "601 - Rejected previously and retry failed"
  val RequestCouldNotBeProcessed = "003 - Request could not be processed"
}

case class RegisterWithEoriAndIdResponseHolder(registerWithEORIAndIDResponse: RegisterWithEoriAndIdResponse)

object RegisterWithEoriAndIdResponseHolder {
  implicit val format = Json.format[RegisterWithEoriAndIdResponseHolder]
}
