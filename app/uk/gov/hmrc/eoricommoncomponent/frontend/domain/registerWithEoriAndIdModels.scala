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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel

import java.lang.reflect.Field
import java.time.LocalDate

case class GovGatewayCredentials(email: String)

object GovGatewayCredentials {
  implicit val format: OFormat[GovGatewayCredentials] = Json.format[GovGatewayCredentials]
}

case class EstablishmentAddress(
  streetAndNumber: String,
  city: String,
  postalCode: Option[String] = None,
  countryCode: String
)

object EstablishmentAddress {
  implicit val jsonFormat: OFormat[EstablishmentAddress] = Json.format[EstablishmentAddress]

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
  implicit val format: OFormat[VatIds] = Json.format[VatIds]
}

case class ContactDetail(
  address: EstablishmentAddress,
  contactName: String,
  phone: Option[String],
  fax: Option[String],
  email: Option[String]
)

object ContactDetail {
  implicit val format: OFormat[ContactDetail] = Json.format[ContactDetail]
}

case class Trader(fullName: String, shortName: String)

object Trader {
  implicit val format: OFormat[Trader] = Json.format[Trader]
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
  implicit val format: OFormat[ResponseData] = Json.format[ResponseData]
}

case class RegisterWithEoriAndIdResponseDetail(
  outcome: Option[String],
  caseNumber: Option[String],
  responseData: Option[ResponseData] = None
)

object RegisterWithEoriAndIdResponseDetail {
  implicit val format: OFormat[RegisterWithEoriAndIdResponseDetail] = Json.format[RegisterWithEoriAndIdResponseDetail]
}

case class AdditionalInformation(id: CustomsId, isIndividual: Boolean)

object AdditionalInformation {
  implicit val format: OFormat[AdditionalInformation] = Json.format[AdditionalInformation]
}

trait CaseClassAuditHelper {

  def toMap(caseClassObject: AnyRef = this, ignoredFields: List[String] = List.empty): Map[String, String] =
    caseClassObject.getClass.getDeclaredFields
      .filterNot(field => ignoredFields.contains(field.getName))
      .foldLeft(Map[String, String]()) {
        (acc, f) =>
          f.setAccessible(true)
          val value = f.get(caseClassObject)
          if (value != null)
            if (isScalaOption(value)) {
              val option = value.asInstanceOf[Option[Any]]
              if (option.isDefined)
                fetchValue(acc, f, option.get)
              else
                acc
            } else
              fetchValue(acc, f, value)
          else
            acc

      }

  private def getKeyValue(acc: Map[String, String], value: Any) =
    value match {
      case v: CaseClassAuditHelper => v.toMap()
      case _                       => acc
    }

  private def fetchValue(acc: Map[String, String], f: Field, value: Any) =
    if (isLeafNode(value))
      acc + (f.getName -> value.toString)
    else
      getKeyValue(acc, value)

  private def isLeafNode(value: Any) =
    value match {
      case _: String     => true
      case _: Int        => true
      case _: Long       => true
      case _: Boolean    => true
      case _: Double     => true
      case _: BigDecimal => true
      case _: Float      => true
      case _: LocalDate  => true
      case _             => false
    }

  private def isScalaOption(value: Object): Boolean = value.getClass.getSuperclass.equals(Class.forName("scala.Option"))
}

case class RegisterWithEoriAndIdResponse(
  responseCommon: ResponseCommon,
  responseDetail: Option[RegisterWithEoriAndIdResponseDetail],
  additionalInformation: Option[AdditionalInformation] = None
)

object RegisterWithEoriAndIdResponse {
  implicit val format: OFormat[RegisterWithEoriAndIdResponse] = Json.format[RegisterWithEoriAndIdResponse]
}
