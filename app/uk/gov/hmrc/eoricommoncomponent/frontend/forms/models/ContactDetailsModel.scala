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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models

import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.ContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel.trim
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionTimeOutException

case class ContactDetailsModel(
  fullName: String,
  emailAddress: String,
  telephone: Option[String],
  fax: Option[String],
  useAddressFromRegistrationDetails: Boolean = true,
  street: Option[String],
  city: Option[String],
  postcode: Option[String],
  countryCode: Option[String]
) {

  def contactDetails: ContactDetails = ContactDetails(
    fullName,
    emailAddress,
    telephone,
    fax,
    trim(street).getOrElse(""),
    trim(city).getOrElse(""),
    trim(postcode),
    countryCode.getOrElse("")
  )

  def toContactInfoViewModel: ContactDetailsViewModel =
    ContactDetailsViewModel(fullName, Some(emailAddress), telephone)

  def toAddressViewModel: Option[AddressViewModel] =
    Some(
      AddressViewModel(trim(street).getOrElse(""), trim(city).getOrElse(""), trim(postcode), countryCode.getOrElse(""))
    )

  def toRowContactInformation(): ContactInformation = ContactInformation(
    personOfContact = Some(fullName),
    sepCorrAddrIndicator = Some(false),
    streetAndNumber = None,
    city = None,
    postalCode = None,
    countryCode = None,
    telephoneNumber = telephone,
    faxNumber = None,
    emailAddress = Some(emailAddress)
  )

}

object ContactDetailsModel {
  implicit val jsonFormat: OFormat[ContactDetailsModel] = Json.format[ContactDetailsModel]

  def trim(value: Option[String]): Option[String] = value.map(_.trim)
}

case class ContactDetailsViewModel(fullName: String, emailAddress: Option[String], telephone: Option[String])
    extends Logging {

  def toContactInfoDetailsModel(contactDetails: Option[ContactDetailsModel]): ContactDetailsModel =
    contactDetails match {
      case Some(cd) =>
        ContactDetailsModel(
          fullName,
          emailAddress.getOrElse {
            // $COVERAGE-OFF$Loggers
            logger.warn("SessionTimeOutException. Email is required")
            // $COVERAGE-ON
            throw SessionTimeOutException("Email is required")
          },
          telephone,
          fax = cd.fax,
          useAddressFromRegistrationDetails = false,
          street = cd.street,
          city = cd.city,
          postcode = cd.postcode,
          countryCode = cd.countryCode
        )
      case None =>
        ContactDetailsModel(
          fullName,
          emailAddress.getOrElse {
            // $COVERAGE-OFF$Loggers
            logger.warn("SessionTimeOutException. Email is required")
            // $COVERAGE-ON
            throw SessionTimeOutException("Email is required")
          },
          telephone,
          fax = None,
          useAddressFromRegistrationDetails = false,
          street = None,
          city = None,
          postcode = None,
          countryCode = None
        )
    }

}

object ContactDetailsViewModel {
  implicit val jsonFormat: OFormat[ContactDetailsViewModel] = Json.format[ContactDetailsViewModel]
}
