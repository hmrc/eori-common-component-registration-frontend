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

package util.builders

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{NonUKIdentification, _}
import org.joda.time.DateTime

//TODO THIS MUST DIE !!!

object RegistrationInfoResponseHolderBuilder {
  val ProcessingDate    = new DateTime().withDate(2016, 3, 17).withTime(9, 31, 5, 0)
  val defaultTaxPayerId = "0100086619"

  val address = RegistrationInfoAddress(
    addressLine1 = "Line 1",
    addressLine2 = Some("line 2"),
    addressLine3 = Some("line 3"),
    addressLine4 = Some("line 4"),
    postalCode = Some("SE28 1AA"),
    countryCode = "ZZ"
  )

  val contactDetails = RegistrationInfoContactDetails(
    phoneNumber = Some("01632961234"),
    mobileNumber = None,
    faxNumber = None,
    emailAddress = Some("john.doe@example.com")
  )

  val AnIndividual = Some(
    RegistrationInfoIndividual(
      firstName = "John",
      middleName = None,
      lastName = "Doe",
      dateOfBirth = Some("1989-09-21")
    )
  )

  val AnOrganisation = Some(
    RegistrationInfoOrganisation(
      organisationName = "organisationName",
      isAGroup = false,
      organisationType = Some("Partnership"),
      None
    )
  )

  def registrationInfoResponseHolder(
    optionalIndividual: Option[RegistrationInfoIndividual],
    optionalOrganisation: Option[RegistrationInfoOrganisation],
    address: RegistrationInfoAddress = address,
    contact: RegistrationInfoContactDetails = contactDetails,
    taxPayerId: String = defaultTaxPayerId
  ): RegistrationInfoResponseHolder = RegistrationInfoResponseHolder(
    RegistrationInfoResponse(
      RegistrationInfoResponseCommon("OK", ProcessingDate, taxPayerId),
      RegistrationInfoResponseDetail(
        "XY0000100086619",
        Some("123"),
        Some(NonUKIdentification(IDNumber = "123", issuingInstitution = "AAA", issuingCountryCode = "GB")),
        isEditable = true,
        isAnAgent = false,
        isAnIndividual = optionalIndividual.isDefined,
        optionalIndividual,
        optionalOrganisation,
        address,
        contact
      )
    )
  )

}
