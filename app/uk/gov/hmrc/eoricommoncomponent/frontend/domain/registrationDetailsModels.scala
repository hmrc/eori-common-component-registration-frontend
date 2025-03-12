/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.EmbassyId
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.RegistrationDetailsEmbassy.{embassyReads, embassyWrites}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address

import java.time.LocalDate

case class BusinessAddress(
  line_1: String,
  line_2: String,
  line_3: Option[String],
  line_4: Option[String],
  postcode: Option[String] = None,
  country: String
)

object BusinessAddress {
  implicit val formats: OFormat[BusinessAddress] = Json.format[BusinessAddress]
}

sealed trait RegistrationDetails {
  def customsId: Option[CustomsId]

  def sapNumber: TaxPayerId

  def safeId: SafeId

  def name: String

  def address: Address

  def dateOfEstablishmentOption: Option[LocalDate] = None
  def dateOfBirthOption: Option[LocalDate]         = None
  def orgType: Option[String]                      = None
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

case class RegistrationDetailsEmbassy(
  customsId: Option[CustomsId] = None,
  sapNumber: TaxPayerId = TaxPayerId(""),
  safeId: SafeId = SafeId(""),
  name: String,
  address: Address = Address("", None, None, None, None, ""),
  override val orgType: Option[String] = Some(EmbassyId)
) extends RegistrationDetails

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

  private val orgFormat: OFormat[RegistrationDetailsOrganisation]          = Json.format[RegistrationDetailsOrganisation]
  private val individualFormat: OFormat[RegistrationDetailsIndividual]     = Json.format[RegistrationDetailsIndividual]
  private val registrationSafeIdFormat: OFormat[RegistrationDetailsSafeId] = Json.format[RegistrationDetailsSafeId]

  /** TODO in the future change this to always read the orgType & create the corresponding registration details object based on it.
    */
  implicit val formats: Format[RegistrationDetails] = Format[RegistrationDetails](
    Reads { js =>
      individualFormat.reads(js) match {
        case ok: JsSuccess[RegistrationDetailsIndividual] => ok
        case _ =>
          embassyReads.reads(js) match {
            case ok: JsSuccess[RegistrationDetailsEmbassy] => ok
            case _ =>
              orgFormat.reads(js) match {
                case ok: JsSuccess[RegistrationDetailsOrganisation] => ok
                case _ => registrationSafeIdFormat.reads(js)
              }
          }
      }
    },
    Writes {
      case individual: RegistrationDetailsIndividual => individualFormat.writes(individual)
      case organisation: RegistrationDetailsOrganisation => orgFormat.writes(organisation)
      case regSafeId: RegistrationDetailsSafeId => registrationSafeIdFormat.writes(regSafeId)
      case embassy: RegistrationDetailsEmbassy => embassyWrites.writes(embassy)
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

  def charityPublicBodyNotForProfit: RegistrationDetailsOrganisation =
    new RegistrationDetailsOrganisation(
      None,
      TaxPayerId(""),
      SafeId(""),
      "",
      Address("", None, None, None, None, ""),
      None,
      Some(UnincorporatedBody)
    )

}

object RegistrationDetailsEmbassy {

  def apply(embassyName: String): RegistrationDetailsEmbassy = {
    new RegistrationDetailsEmbassy(name = embassyName)
  }

  def apply(
    embassyName: String,
    embassyAddress: Address,
    embassyCustomsId: Option[CustomsId],
    embassySafeId: SafeId
  ): RegistrationDetailsEmbassy = {
    new RegistrationDetailsEmbassy(
      name = embassyName,
      address = embassyAddress,
      customsId = embassyCustomsId,
      safeId = embassySafeId
    )
  }

  def initEmpty(): RegistrationDetailsEmbassy = {
    new RegistrationDetailsEmbassy(name = "")
  }

  val embassyWrites: Writes[RegistrationDetailsEmbassy] = (o: RegistrationDetailsEmbassy) => {
    JsObject(
      Seq(
        "orgType"   -> JsString(o.orgType.head),
        "name"      -> JsString(o.name),
        "address"   -> Json.toJson(o.address),
        "customsId" -> Json.toJson(o.customsId),
        "safeId"    -> Json.toJson(o.safeId)
        // TODO date of establishment
      )
    )
  }

  private val blindReads: Reads[RegistrationDetailsEmbassy] = (
    (__ \ "orgType").read[String] and
      (__ \ "name").read[String] and
      (__ \ "address").read[Address] and
      (__ \ "customsId").readNullable[CustomsId] and
      (__ \ "safeId").read[SafeId]
  )((_, name, address, customsId, safeId) => RegistrationDetailsEmbassy(name, address, customsId, safeId))

  val embassyReads: Reads[RegistrationDetailsEmbassy] = (json: JsValue) => {
    val value = (json \ "orgType").validateOpt[String]

    if (value.get.isEmpty) {
      JsError("Unable to read json as RegistrationDetailsEmbassy")
    } else {
      value.map {
        case Some(orgType) if orgType == EmbassyId => blindReads.reads(json).get
      }
    }

  }

}
