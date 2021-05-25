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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.ContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel.trim
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionTimeOutException

case class ContactDetailsModel(
  fullName: String,
  emailAddress: String,
  telephone: String,
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

  def toContactDetailsViewModel: ContactDetailsViewModel = ContactDetailsViewModel(
    fullName,
    Some(emailAddress),
    telephone,
    fax,
    useAddressFromRegistrationDetails,
    trim(street),
    trim(city),
    trim(postcode),
    countryCode
  )

  def toRowContactInformation(): ContactInformation = ContactInformation(
    personOfContact = Some(fullName),
    sepCorrAddrIndicator = Some(false),
    streetAndNumber = None,
    city = None,
    postalCode = None,
    countryCode = None,
    telephoneNumber = Some(telephone),
    faxNumber = None,
    emailAddress = Some(emailAddress)
  )

}

object ContactDetailsModel {
  implicit val jsonFormat: OFormat[ContactDetailsModel] = Json.format[ContactDetailsModel]

  def trim(value: Option[String]): Option[String] = value.map(_.trim)
}

//TODO remove email address read from cache and populate the contact details
case class ContactDetailsViewModel(
  fullName: String,
  emailAddress: Option[String],
  telephone: String,
  fax: Option[String],
  useAddressFromRegistrationDetails: Boolean = true,
  street: Option[String],
  city: Option[String],
  postcode: Option[String],
  countryCode: Option[String]
) {

  def toContactDetailsModel: ContactDetailsModel = ContactDetailsModel(
    fullName,
    emailAddress.getOrElse(throw SessionTimeOutException("Email is required")),
    telephone,
    fax,
    useAddressFromRegistrationDetails,
    trim(street),
    trim(city),
    trim(postcode),
    countryCode
  )

}

object ContactDetailsViewModel {
  implicit val jsonFormat: OFormat[ContactDetailsViewModel] = Json.format[ContactDetailsViewModel]
}
