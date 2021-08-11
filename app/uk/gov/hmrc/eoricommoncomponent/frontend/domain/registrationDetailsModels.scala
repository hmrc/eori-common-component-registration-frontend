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

import java.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address

case class BusinessAddress(
  line_1: String,
  line_2: String,
  line_3: Option[String],
  line_4: Option[String],
  postcode: Option[String] = None,
  country: String
)

object BusinessAddress {
  implicit val formats = Json.format[BusinessAddress]
}

//TODO Should we merge RegistrationDetails and RegistrationInfo? Both are our internal representation of RegistrationDetails
sealed trait RegistrationDetails {
  def customsId: Option[CustomsId]

  def sapNumber: TaxPayerId

  def safeId: SafeId

  def name: String

  def address: Address

  def dateOfEstablishmentOption: Option[LocalDate] = None
  def dateOfBirthOption: Option[LocalDate]         = None
}

case class RegistrationDetailsOrganisation(
  customsId: Option[CustomsId],
  sapNumber: TaxPayerId,
  safeId: SafeId,
  name: String,
  address: Address,
  dateOfEstablishment: Option[LocalDate],
  etmpOrganisationType: Option[EtmpOrganisationType]
) extends RegistrationDetails {
  override def dateOfEstablishmentOption: Option[LocalDate] = dateOfEstablishment
}

case class RegistrationDetailsIndividual(
  customsId: Option[CustomsId],
  sapNumber: TaxPayerId,
  safeId: SafeId,
  name: String,
  address: Address,
  dateOfBirth: LocalDate
) extends RegistrationDetails {
  override def dateOfBirthOption: Option[LocalDate] = Some(dateOfBirth)
}

case class RegistrationDetailsSafeId(
  safeId: SafeId,
  address: Address,
  sapNumber: TaxPayerId,
  customsId: Option[CustomsId] = None,
  name: String
) extends RegistrationDetails

object RegistrationDetails {

  def individual(
    sapNumber: String,
    safeId: SafeId,
    name: String,
    address: Address,
    dateOfBirth: LocalDate,
    customsId: Option[CustomsId]
  ): RegistrationDetailsIndividual =
    RegistrationDetailsIndividual(customsId, TaxPayerId(sapNumber), safeId, name, address, dateOfBirth)

  def organisation(
    sapNumber: String,
    safeId: SafeId,
    name: String,
    address: Address,
    customsId: Option[CustomsId],
    dateEstablished: Option[LocalDate] = None,
    etmpOrganisationType: Option[EtmpOrganisationType] = None
  ): RegistrationDetailsOrganisation =
    RegistrationDetailsOrganisation(
      customsId,
      TaxPayerId(sapNumber),
      safeId,
      name,
      address,
      dateEstablished,
      etmpOrganisationType
    )

  def rdSafeId(safeId: SafeId): RegistrationDetailsSafeId =
    RegistrationDetailsSafeId(safeId, Address("", Some(""), Some(""), Some(""), Some(""), ""), TaxPayerId(""), None, "")

  private val orgFormat                = Json.format[RegistrationDetailsOrganisation]
  private val individualFormat         = Json.format[RegistrationDetailsIndividual]
  private val registrationSafeIdFormat = Json.format[RegistrationDetailsSafeId]

  implicit val formats = Format[RegistrationDetails](
    Reads { js =>
      individualFormat.reads(js) match {
        case ok: JsSuccess[RegistrationDetailsIndividual] => ok
        case _ =>
          orgFormat.reads(js) match {
            case ok: JsSuccess[RegistrationDetailsOrganisation] => ok
            case _                                              => registrationSafeIdFormat.reads(js)
          }
      }
    },
    Writes {
      case individual: RegistrationDetailsIndividual     => individualFormat.writes(individual)
      case organisation: RegistrationDetailsOrganisation => orgFormat.writes(organisation)
      case regSafeId: RegistrationDetailsSafeId          => registrationSafeIdFormat.writes(regSafeId)
    }
  )

}

object RegistrationDetailsIndividual {

  def apply(fullName: String, dateOfBirth: LocalDate): RegistrationDetailsIndividual =
    new RegistrationDetailsIndividual(
      None,
      TaxPayerId(""),
      SafeId(""),
      fullName,
      Address("", None, None, None, None, ""),
      dateOfBirth
    )

  def apply(): RegistrationDetailsIndividual =
    new RegistrationDetailsIndividual(
      None,
      TaxPayerId(""),
      SafeId(""),
      "",
      Address("", None, None, None, None, ""),
      LocalDate.now
    )

}

object RegistrationDetailsOrganisation {

  def apply(): RegistrationDetailsOrganisation =
    new RegistrationDetailsOrganisation(
      None,
      TaxPayerId(""),
      SafeId(""),
      "",
      Address("", None, None, None, None, ""),
      None,
      None
    )

}
