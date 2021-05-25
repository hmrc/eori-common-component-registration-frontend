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

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.{postCodeMandatoryCountryCodes, postcodeRegex}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries

case class Header(originatingSystem: String, requestTimeStamp: String, correlationId: String)

object Header {
  implicit val jsonFormat = Json.format[Header]
}

case class Address(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  postalCode: Option[String],
  countryCode: String
) {

  private def isValidCountry: Boolean     = Countries.all.exists(_.countryCode == countryCode)
  private def hasValidPostcode: Boolean   = this.postalCode.exists(_.matches(postcodeRegex.regex))
  private def isPostcodeRequired: Boolean = postCodeMandatoryCountryCodes.contains(countryCode)

  def isValidAddress(): Boolean =
    if (isValidCountry)
      if (isPostcodeRequired) hasValidPostcode else true
    else false

}

object Address {
  implicit val jsonFormat = Json.format[Address]

  def apply(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    postalCode: Option[String],
    countryCode: String
  ): Address =
    new Address(
      addressLine1,
      addressLine2,
      addressLine3,
      addressLine4,
      postalCode.filter(_.nonEmpty),
      countryCode.toUpperCase()
    ) {}

  def apply(address: AddressViewModel): Address =
    new Address(address.street, None, Some(address.city), None, address.postcode, address.countryCode) {}

}

trait IndividualName {
  def firstName: String

  def middleName: Option[String]

  def lastName: String

  final def fullName: String = s"$firstName ${middleName.getOrElse("")} $lastName"
}

case class Individual(firstName: String, middleName: Option[String], lastName: String, dateOfBirth: String)
    extends IndividualName

object Individual {

  def noMiddle(firstName: String, lastName: String, dateOfBirth: String): Individual =
    Individual(firstName, middleName = None, lastName, dateOfBirth)

  def withLocalDate(firstName: String, lastName: String, dateOfBirth: LocalDate): Individual =
    withLocalDate(firstName, middleName = None, lastName, dateOfBirth)

  def withLocalDate(
    firstName: String,
    middleName: Option[String],
    lastName: String,
    dateOfBirth: LocalDate
  ): Individual =
    Individual(firstName, middleName, lastName, dateOfBirth.toString)

  implicit val formats = Json.format[Individual]
}

trait CommonHeader {

  private def dateTimeWritesIsoUtc: Writes[DateTime] = new Writes[DateTime] {

    def writes(d: org.joda.time.DateTime): JsValue =
      JsString(d.toString(ISODateTimeFormat.dateTimeNoMillis().withZoneUTC()))

  }

  private def dateTimeReadsIso: Reads[DateTime] = new Reads[DateTime] {

    def reads(value: JsValue): JsResult[DateTime] =
      try JsSuccess(ISODateTimeFormat.dateTimeParser.parseDateTime(value.as[String]))
      catch {
        case e: Exception => JsError(s"Could not parse '${value.toString()}' as an ISO date. Reason: $e")
      }

  }

  implicit val dateTimeReads  = dateTimeReadsIso
  implicit val dateTimeWrites = dateTimeWritesIsoUtc
}

case class MessagingServiceParam(paramName: String, paramValue: String)

object MessagingServiceParam {
  implicit val formats = Json.format[MessagingServiceParam]

  val positionParamName = "POSITION"
  val Generate          = "GENERATE"
  val Link              = "LINK"
  val Pending           = "WORKLIST"
  val Fail              = "FAIL"

  val formBundleIdParamName = "ETMPFORMBUNDLENUMBER"
}

case class RequestParameter(paramName: String, paramValue: String)

object RequestParameter {
  implicit val formats = Json.format[RequestParameter]
}

case class RequestCommon(
  regime: String,
  receiptDate: DateTime,
  acknowledgementReference: String,
  originatingSystem: Option[String] = None,
  requestParameters: Option[Seq[RequestParameter]] = None
)

object RequestCommon extends CommonHeader {
  implicit val requestParamFormat = Json.format[RequestParameter]
  implicit val formats            = Json.format[RequestCommon]
}

case class ResponseCommon(
  status: String,
  statusText: Option[String] = None,
  processingDate: DateTime,
  returnParameters: Option[List[MessagingServiceParam]] = None
)

object ResponseCommon extends CommonHeader {
  val StatusOK         = "OK"
  val StatusNotOK      = "NOT_OK"
  implicit val formats = Json.format[ResponseCommon]
}
