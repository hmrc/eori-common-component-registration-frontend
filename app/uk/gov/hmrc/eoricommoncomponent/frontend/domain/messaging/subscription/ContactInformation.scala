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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription

import java.time.{Clock, LocalDateTime, ZoneId}

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.CommonHeader
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.ContactDetail

case class ContactInformation(
  personOfContact: Option[String] = None,
  sepCorrAddrIndicator: Option[Boolean] = None,
  streetAndNumber: Option[String] = None,
  city: Option[String] = None,
  postalCode: Option[String] = None,
  countryCode: Option[String] = None,
  telephoneNumber: Option[String] = None,
  faxNumber: Option[String] = None,
  emailAddress: Option[String] = None,
  emailVerificationTimestamp: Option[LocalDateTime] = Some(
    LocalDateTime.ofInstant(Clock.systemUTC().instant, ZoneId.of("Europe/London"))
  )
) {

  def withEmail(email: String): ContactInformation =
    this.copy(emailAddress = Some(email))

}

object ContactInformation extends CommonHeader {
  implicit val jsonFormat = Json.format[ContactInformation]

  private def dashForEmpty(s: String): String =
    if (s.isEmpty) "-" else s

  def createContactInformation(cd: ContactDetail): ContactInformation =
    ContactInformation(
      personOfContact = Some(cd.contactName),
      sepCorrAddrIndicator = Some(true), //ASSUMPTION
      streetAndNumber = Some(cd.address.streetAndNumber),
      city = Some(dashForEmpty(cd.address.city)),
      postalCode = cd.address.postalCode,
      countryCode = Some(cd.address.countryCode),
      telephoneNumber = cd.phone,
      emailAddress = cd.email,
      faxNumber = cd.fax
    )

  def createContactInformation(contactDetails: ContactDetails): ContactInformation =
    ContactInformation(
      personOfContact = Some(contactDetails.fullName),
      sepCorrAddrIndicator = Some(true),
      streetAndNumber = Some(contactDetails.street),
      city = Some(dashForEmpty(contactDetails.city)),
      postalCode = contactDetails.postcode.filter(_.nonEmpty),
      countryCode = Some(contactDetails.countryCode),
      telephoneNumber = Some(contactDetails.telephone),
      faxNumber = contactDetails.fax,
      emailAddress = Some(contactDetails.emailAddress)
    )

}
