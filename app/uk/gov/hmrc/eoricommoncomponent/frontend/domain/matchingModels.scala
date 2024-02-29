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

import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{IndividualName, RegistrationInfoRequest}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.formatInput
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

import java.time.LocalDate

sealed trait CustomsId {
  def id: String
}

case class Utr(override val id: String) extends CustomsId

case class Eori(override val id: String) extends CustomsId

case class Nino(override val id: String) extends CustomsId

case class SafeId(override val id: String) extends CustomsId

case class TaxPayerId(override val id: String) extends CustomsId {
  private val MDGTaxPayerIdLength = 42
  val mdgTaxPayerId: String       = id + "0" * (MDGTaxPayerIdLength - id.length)
}

object TaxPayerId {
  implicit val format: OFormat[TaxPayerId] = Json.format[TaxPayerId]
}

object SafeId {
  implicit val format: OFormat[SafeId] = Json.format[SafeId]
  implicit def toJsonFormat(safeId: SafeId): JsValue = Json.toJson(safeId)
}

case class InternalId(id: String)

object InternalId extends Logging {

  def apply(id: Option[String]): InternalId =
    new InternalId(id.getOrElse {
      val error = "InternalId is missing"
      // $COVERAGE-OFF$Loggers
      logger.warn(error)
      // $COVERAGE-ON
      throw new IllegalArgumentException(error)
    })

  implicit val format: OFormat[InternalId] = Json.format[InternalId]
}

case class GroupId(id: String)

object GroupId extends Logging {

  def apply(id: Option[String]): GroupId =
    new GroupId(id.getOrElse {
      val error = "GroupId is missing"
      // $COVERAGE-OFF$Loggers
      logger.warn(error)
      // $COVERAGE-ON
      throw new IllegalArgumentException(error)
    })

  implicit val format: OFormat[GroupId] = Json.format[GroupId]
}

case class CacheIds(internalId: InternalId, safeId: SafeId, serviceCode: Option[String])

object CacheIds extends Logging {

  def apply(mayBeInternalId: Option[String], mayBeSafeId: Option[String], mayBeService: Option[Service]): CacheIds = {
    val internalId = InternalId(mayBeInternalId.getOrElse {
      val error = "InternalId is missing"
      // $COVERAGE-OFF$Loggers
      logger.warn(error)
      // $COVERAGE-ON
      throw new IllegalArgumentException(error)
    })
    val safeId = SafeId(mayBeSafeId.getOrElse {
      val error = "SafeId is missing"
      // $COVERAGE-OFF$Loggers
      logger.warn(error)
      // $COVERAGE-ON
      throw new IllegalArgumentException(error)
    })
    val service = mayBeService.map(_.code)

    new CacheIds(internalId, safeId, service)
  }

  implicit val jsonFormat: OFormat[CacheIds] = Json.format[CacheIds]
  implicit def toJsonFormat(cacheIds: CacheIds): JsValue = Json.toJson(cacheIds)
}

object CustomsId extends Logging {
  val utr        = "utr"
  val eori       = "eori"
  val nino       = "nino"
  val safeId     = "safeId"
  val taxPayerId = "taxPayerId"
  val taxPayerID = "taxPayerID"

  private val idTypeMapping = Map[String, String => CustomsId](
    utr        -> Utr,
    eori       -> Eori,
    nino       -> Nino,
    safeId     -> (s => SafeId(s)),
    taxPayerId -> (s => TaxPayerId(s))
  )

  implicit val formats: Format[CustomsId] = Format[CustomsId](
    fjs = Reads { js =>
      idTypeMapping.view.flatMap {
        case (jsFieldName, idConstruct) =>
          for (id <- (js \ jsFieldName).asOpt[String]) yield idConstruct(id)
      }.headOption
        .fold[JsResult[CustomsId]](JsError("No matching id type and value found"))(customsId => JsSuccess(customsId))
    },
    tjs = Writes {
      case Utr(id)        => Json.obj(utr -> id)
      case Eori(id)       => Json.obj(eori -> id)
      case Nino(id)       => Json.obj(nino -> id)
      case SafeId(id)     => Json.obj(safeId -> id)
      case TaxPayerId(id) => Json.obj(taxPayerId -> id)
    }
  )

  def apply(idType: String, idNumber: String): CustomsId =
    idType match {
      case RegistrationInfoRequest.NINO   => Nino(idNumber)
      case RegistrationInfoRequest.UTR    => Utr(idNumber)
      case RegistrationInfoRequest.EORI   => Eori(idNumber)
      case RegistrationInfoRequest.SAFEID => SafeId(idNumber)
      case _ =>
        val error = s"Unknown Identifier: $idType. Expected Nino, UTR or EORI number"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalArgumentException(error)
    }

}

trait NameIdOrganisationMatch {
  def name: String
  def id: String
}

trait NameOrganisationMatch {
  def name: String
}

case class NameIdOrganisationMatchModel(name: String, id: String) extends NameIdOrganisationMatch

object NameIdOrganisationMatchModel {
  implicit val jsonFormat: OFormat[NameIdOrganisationMatchModel] = Json.format[NameIdOrganisationMatchModel]

  def apply(name: String, id: String): NameIdOrganisationMatchModel =
    new NameIdOrganisationMatchModel(name, formatInput(id))

}

case class NameOrganisationMatchModel(name: String) extends NameOrganisationMatch

object NameOrganisationMatchModel {
  implicit val jsonFormat: OFormat[NameOrganisationMatchModel] = Json.format[NameOrganisationMatchModel]
}

case class YesNo(isYes: Boolean) {
  def isNo: Boolean = !isYes
}

object YesNo {
  val yesAndNoAnswer = "yes-no-answer"
  val answerTrue     = "yes-no-answer-true"
}

case class VatVerificationOption(isDateOption: Boolean) {
  def isAmountOption: Boolean = !isDateOption
}

case class NinoMatch(firstName: String, lastName: String, dateOfBirth: LocalDate, nino: String)

object NinoMatch {

  def apply(firstName: String, lastName: String, dateOfBirth: LocalDate, nino: String): NinoMatch =
    new NinoMatch(firstName, lastName, dateOfBirth, formatInput(nino))

}

trait NameDobMatch {
  def firstName: String

  def lastName: String

  def dateOfBirth: LocalDate
}

case class NameDobMatchModel(firstName: String, lastName: String, dateOfBirth: LocalDate) extends NameDobMatch {
  def name: String = s"$firstName $lastName"
}

object NameDobMatchModel {
  implicit val jsonFormat: OFormat[NameDobMatchModel] = Json.format[NameDobMatchModel]
}

case class NinoOrUtr(nino: Option[String], utr: Option[String], ninoOrUtrRadio: Option[String])

object NinoOrUtr {

  def apply(nino: Option[String], utr: Option[String], ninoOrUtrRadio: Option[String]): NinoOrUtr =
    new NinoOrUtr(formatInput(nino), formatInput(utr), ninoOrUtrRadio)

}

case class NinoOrUtrChoice(ninoOrUtrRadio: Option[String])

case class SixLineAddressMatchModel(
  lineOne: String,
  lineTwo: Option[String],
  lineThree: String,
  lineFour: Option[String],
  postcode: Option[String],
  country: String
) {
  require(
    if (postCodeMandatoryForCountryCode) postcode.fold(false)(_.trim.nonEmpty) else true,
    s"Postcode required for country code: $country"
  )

  private def postCodeMandatoryForCountryCode = List("GG", "JE").contains(country)
}

object SixLineAddressMatchModel {

  def apply(
    lineOne: String,
    lineTwo: Option[String],
    lineThree: String,
    lineFour: Option[String],
    postcode: Option[String],
    country: String
  ): SixLineAddressMatchModel = new SixLineAddressMatchModel(
    lineOne.trim,
    lineTwo.map(_.trim),
    lineThree.trim,
    lineFour.map(_.trim),
    postcode.map(_.trim),
    country
  )

}

case class IndividualNameAndDateOfBirth(firstName: String, lastName: String, dateOfBirth: LocalDate)
    extends IndividualName

case class EoriAndIdNameAndAddress(fullName: String, address: EstablishmentAddress)

trait IdMatch {
  def id: String
}

case class IdMatchModel(id: String) extends IdMatch

object IdMatchModel {
  implicit val jsonFormat: OFormat[IdMatchModel] = Json.format[IdMatchModel]

  def apply(id: String): IdMatchModel = new IdMatchModel(formatInput(id))
}

case class UtrMatchModel(haveUtr: Option[Boolean], id: Option[String])

object UtrMatchModel {
  implicit val jsonFormat: OFormat[UtrMatchModel] = Json.format[UtrMatchModel]

  def apply(haveUtr: Option[Boolean]): UtrMatchModel = new UtrMatchModel(haveUtr, None)
}

trait NameMatch {
  def name: String
}

case class NameMatchModel(name: String) extends NameMatch

object NameMatchModel {
  implicit val jsonFormat: OFormat[NameMatchModel] = Json.format[NameMatchModel]
}

case class NinoMatchModel(haveNino: Option[Boolean], nino: Option[String])

object NinoMatchModel {
  implicit val jsonFormat: OFormat[NinoMatchModel] = Json.format[NinoMatchModel]

  def apply(haveNino: Option[Boolean]): NinoMatchModel =
    new NinoMatchModel(haveNino, None)

}

case class ExistingEori(id: String, enrolmentKey: String)

object ExistingEori {
  implicit val jsonFormat: OFormat[ExistingEori] = Json.format[ExistingEori]

  def apply(id: Option[String], enrolmentKey: String): ExistingEori =
    new ExistingEori(id.getOrElse(throw new IllegalArgumentException("EORI is missing")), enrolmentKey)

}
