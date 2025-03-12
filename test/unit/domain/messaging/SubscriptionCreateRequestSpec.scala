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

package unit.domain.messaging

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  ContactInformation,
  SubscriptionCreateRequest,
  VatId
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{AddressViewModel, ContactDetailsModel, VatDetails}

import java.time.{LocalDate, LocalDateTime}

class SubscriptionCreateRequestSpec extends UnitSpec {
  private val email = "john.doe@example.com"

  private val taxPayerId                 = TaxPayerId("taxPayerId")
  private val safeId                     = SafeId("safeId")
  private val fullName                   = "Full name"
  private val address                    = Address("addressLine1", None, Some("city"), None, Some("postcode"), "GB")
  private val establishmentAddress       = EstablishmentAddress("addressLine1", "city", Some("postcode"), "GB")
  private val addressViewModel           = AddressViewModel(address)
  private val dateOfBirthOrEstablishment = LocalDate.now()

  private val emailAddress = Some("john.doe@example.com")

  private val contactDetails = ContactDetailsModel(
    fullName = fullName,
    emailAddress = emailAddress.get,
    telephone = "01234123123",
    fax = None,
    street = None,
    city = None,
    postcode = None,
    countryCode = None
  )

  private def registrationExpectedContactInformation(timeStamp: LocalDateTime) = ContactInformation(
    Some(fullName),
    Some(true),
    Some(""),
    Some("-"),
    None,
    Some(""),
    Some("01234123123"),
    None,
    Some(email),
    Some(timeStamp)
  )

  "Subscription Create Request" should {

    "correctly build Organisation request during registration journey" in {
      val registrationDetails = RegistrationDetailsOrganisation(
        customsId = None,
        sapNumber = taxPayerId,
        safeId = safeId,
        name = fullName,
        address = address,
        dateOfEstablishment = None,
        etmpOrganisationType = Some(CorporateBody)
      )
      val subscriptionDetails = SubscriptionDetails(
        ukVatDetails = Some(VatDetails("AA11 1AA", "123456")),
        addressDetails = Some(addressViewModel),
        contactDetails = Some(contactDetails),
        personalDataDisclosureConsent = Some(true),
        businessShortName = Some(BusinessShortName("short name")),
        sicCode = Some("12345"),
        dateEstablished = Some(dateOfBirthOrEstablishment)
      )
      val cdsOrgType = CdsOrganisationType.Company

      val request = SubscriptionCreateRequest.fromOrganisation(
        registrationDetails,
        subscriptionDetails,
        Some(cdsOrgType),
        Some(atarService)
      )

      val requestCommon  = request.subscriptionCreateRequest.requestCommon
      val requestDetails = request.subscriptionCreateRequest.requestDetail

      requestCommon.regime shouldBe "CDS"
      requestDetails.SAFE shouldBe safeId.id
      requestDetails.EORINo shouldBe None
      requestDetails.CDSFullName shouldBe fullName
      requestDetails.CDSEstablishmentAddress shouldBe establishmentAddress
      requestDetails.establishmentInTheCustomsTerritoryOfTheUnion shouldBe None
      requestDetails.typeOfLegalEntity shouldBe Some("Corporate Body")
      requestDetails.contactInformation shouldBe Some(
        registrationExpectedContactInformation(requestDetails.contactInformation.get.emailVerificationTimestamp.get)
      )
      requestDetails.vatIDs shouldBe Some(Seq(VatId(Some("GB"), Some("123456"))))
      requestDetails.consentToDisclosureOfPersonalData shouldBe Some("1")
      requestDetails.shortName shouldBe None
      requestDetails.dateOfEstablishment shouldBe Some(dateOfBirthOrEstablishment)
      requestDetails.typeOfPerson shouldBe Some("2")
      requestDetails.principalEconomicActivity shouldBe Some("1234")
      requestDetails.serviceName shouldBe Some(atarService.enrolmentKey)
    }

    "correctly build Individual request during registration journey" in {
      val registrationDetails = RegistrationDetailsIndividual(
        customsId = None,
        sapNumber = taxPayerId,
        safeId = safeId,
        name = fullName,
        address = address,
        dateOfBirth = dateOfBirthOrEstablishment
      )
      val subscriptionDetails = SubscriptionDetails(
        ukVatDetails = Some(VatDetails("AA11 1AA", "123456")),
        addressDetails = Some(addressViewModel),
        contactDetails = Some(contactDetails),
        personalDataDisclosureConsent = Some(true),
        businessShortName = Some(BusinessShortName("short name")),
        sicCode = Some("12345")
      )
      val cdsOrgType = CdsOrganisationType.Company

      val request = SubscriptionCreateRequest.fromIndividual(
        registrationDetails,
        subscriptionDetails,
        Some(cdsOrgType),
        Some(atarService)
      )

      val requestCommon  = request.subscriptionCreateRequest.requestCommon
      val requestDetails = request.subscriptionCreateRequest.requestDetail

      requestCommon.regime shouldBe "CDS"
      requestDetails.SAFE shouldBe safeId.id
      requestDetails.EORINo shouldBe None
      requestDetails.CDSFullName shouldBe fullName
      requestDetails.CDSEstablishmentAddress shouldBe establishmentAddress
      requestDetails.establishmentInTheCustomsTerritoryOfTheUnion shouldBe None
      requestDetails.typeOfLegalEntity shouldBe Some("Corporate Body")
      requestDetails.contactInformation shouldBe Some(
        registrationExpectedContactInformation(requestDetails.contactInformation.get.emailVerificationTimestamp.get)
      )
      requestDetails.vatIDs shouldBe Some(Seq(VatId(Some("GB"), Some("123456"))))
      requestDetails.consentToDisclosureOfPersonalData shouldBe Some("1")
      requestDetails.shortName shouldBe None
      requestDetails.dateOfEstablishment shouldBe Some(dateOfBirthOrEstablishment)
      requestDetails.typeOfPerson shouldBe Some("2")
      requestDetails.principalEconomicActivity shouldBe Some("1234")
      requestDetails.serviceName shouldBe Some(atarService.enrolmentKey)
    }
  }
}
