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

import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EstablishmentAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails

object SubscriptionInfoBuilder {

  val eori           = Some("12345")
  val CDSOrgName     = "orgName"
  val orgStreetName  = "Line 1"
  val orgCity        = "line 2"
  val orgPostalCode  = Some("SE28 1AA")
  val orgCountryCode = "ZZ"

  val contactName                  = "John Doe"
  val contactStreet                = "Line 1"
  val contactCity                  = "city name"
  val contactPostalCode            = "SE28 1AA"
  val contactCountry               = "ZZ"
  val telephoneNumber              = "01632961234"
  val faxNumber                    = "01632961235"
  val emailAddress                 = "john.doe@example.com"
  val dateOfEstablishmentFormatted = "31 December 2015"
  val dateOfEstablishment          = LocalDate.parse(dateOfEstablishmentFormatted, DateTimeFormat.forPattern("d MMM YYYY"))

  val VATIdNoList      = List("VAT-1", "VAT-2", "VAT-3", "VAT-4", "VAT-5")
  val VATIdCountryList = List("GB", "FR", "ES", "PT", "IN")

  val vatIDList = for {
    index <- VATIdNoList.indices
    vatCountry = VATIdCountryList lift index
    vatID      = VATIdNoList lift index
    vatList    = SubscriptionInfoVatId(vatCountry, vatID)
  } yield vatList

  val shortName                                = "ltd"
  val legalEntityValue                         = "0001"
  val typeOfPerson                             = "1"
  val principalEconomicActivity                = "100"
  val consentToDisclosureOfPersonalDataGranted = "1"
  val consentToDisclosureOfPersonalDataDenied  = "0"

  private def thirdCountryUniqueIdentificationNumber(index: Int) = s"000$index"

  val establishmentInTheCustomsTerritoryOfTheUnion = "1"

  val fullyPopulatedEstablishmentAddress = EstablishmentAddress(orgStreetName, orgCity, orgPostalCode, orgCountryCode)

  val fullyPopulatedContactInformation = ContactInformation(
    personOfContact = Some(contactName),
    sepCorrAddrIndicator = Some(true),
    streetAndNumber = Some(contactStreet),
    city = Some(contactCity),
    postalCode = Some(contactPostalCode),
    countryCode = Some(contactCountry),
    telephoneNumber = Some(telephoneNumber),
    faxNumber = Some(faxNumber),
    emailAddress = Some(emailAddress)
  )

  val unpopulatedContactInformation = ContactInformation(
    personOfContact = None,
    sepCorrAddrIndicator = None,
    streetAndNumber = None,
    city = None,
    postalCode = None,
    countryCode = None,
    telephoneNumber = None,
    faxNumber = None,
    emailAddress = None
  )

  val partiallyPopulatedContactInformation = ContactInformation(
    personOfContact = None,
    sepCorrAddrIndicator = None,
    streetAndNumber = Some(contactStreet),
    city = None,
    postalCode = Some(contactPostalCode),
    countryCode = Some(contactCountry),
    telephoneNumber = None,
    faxNumber = None,
    emailAddress = None
  )

  val onlyMandatoryPopulatedResponseDetail = SubscriptionDisplayResponseDetail(
    EORINo = eori,
    CDSFullName = CDSOrgName,
    CDSEstablishmentAddress = fullyPopulatedEstablishmentAddress,
    establishmentInTheCustomsTerritoryOfTheUnion = None,
    typeOfLegalEntity = None,
    contactInformation = None,
    VATIDs = None,
    thirdCountryUniqueIdentificationNumber = None,
    consentToDisclosureOfPersonalData = None,
    shortName = None,
    dateOfEstablishment = Some(dateOfEstablishment),
    typeOfPerson = None,
    principalEconomicActivity = None
  )

  val fullyPopulatedResponseDetail = SubscriptionDisplayResponseDetail(
    EORINo = eori,
    CDSFullName = CDSOrgName,
    CDSEstablishmentAddress = fullyPopulatedEstablishmentAddress,
    establishmentInTheCustomsTerritoryOfTheUnion = Some(establishmentInTheCustomsTerritoryOfTheUnion),
    typeOfLegalEntity = Some(legalEntityValue),
    contactInformation = Some(fullyPopulatedContactInformation),
    VATIDs = Some(vatIDList.toList),
    thirdCountryUniqueIdentificationNumber = Some(
      List(
        thirdCountryUniqueIdentificationNumber(1),
        thirdCountryUniqueIdentificationNumber(2),
        thirdCountryUniqueIdentificationNumber(3),
        thirdCountryUniqueIdentificationNumber(4),
        thirdCountryUniqueIdentificationNumber(5)
      )
    ),
    consentToDisclosureOfPersonalData = Some(consentToDisclosureOfPersonalDataGranted),
    shortName = Some(shortName),
    dateOfEstablishment = Some(dateOfEstablishment),
    typeOfPerson = Some(typeOfPerson),
    principalEconomicActivity = Some(principalEconomicActivity)
  )

  val responseDetailWithoutEmail =
    fullyPopulatedResponseDetail.copy(contactInformation = Some(partiallyPopulatedContactInformation))

  val responseDetailWithUnverifiedEmail =
    fullyPopulatedResponseDetail.copy(contactInformation =
      Some(fullyPopulatedContactInformation.copy(emailVerificationTimestamp = None))
    )

  val responseDetailWithoutPersonOfContact = fullyPopulatedResponseDetail.copy(contactInformation =
    Some(partiallyPopulatedContactInformation.copy(emailAddress = Some(emailAddress)))
  )

  val sampleResponseCommon = ResponseCommon(
    "OK",
    Some("Status text"),
    DateTime.now,
    Some(
      List(
        MessagingServiceParam("POSITION", "GENERATE"),
        MessagingServiceParam("ETMPFORMBUNDLENUMBER", "0771123680108")
      )
    )
  )

  val sampleResponseCommonWithBlankReturnParameters = sampleResponseCommon.copy(returnParameters = None)

  val sampleResponseCommonWithNoETMPFORMBUNDLENUMBER =
    sampleResponseCommon.copy(returnParameters = Some(List(MessagingServiceParam("POSITION", "GENERATE"))))

  val fullyPopulatedResponse = SubscriptionDisplayResponse(sampleResponseCommon, fullyPopulatedResponseDetail)

  val onlyMandatoryPopulatedResponse =
    SubscriptionDisplayResponse(sampleResponseCommon, onlyMandatoryPopulatedResponseDetail)

  def mandatoryResponseWithConsentPopulated(consent: String): SubscriptionDisplayResponse = {
    val responseDetail = onlyMandatoryPopulatedResponseDetail.copy(consentToDisclosureOfPersonalData = Some(consent))

    onlyMandatoryPopulatedResponse.copy(responseDetail = responseDetail)
  }

  def fullyPopulatedResponseWithEmptyVATIDsList: SubscriptionDisplayResponse = {
    val responseDetail = fullyPopulatedResponseDetail.copy(VATIDs = Some(List.empty))

    fullyPopulatedResponse.copy(responseDetail = responseDetail)
  }

  def fullyPopulatedResponseWithDateOfEstablishment(dateOfEstablishment: LocalDate): SubscriptionDisplayResponse = {
    val responseDetail = fullyPopulatedResponseDetail.copy(dateOfEstablishment = Some(dateOfEstablishment))

    fullyPopulatedResponse.copy(responseDetail = responseDetail)
  }

  val fullyPopulatedResponseWithBlankReturnParameters =
    SubscriptionDisplayResponse(sampleResponseCommonWithBlankReturnParameters, fullyPopulatedResponseDetail)

  val fullyPopulatedResponseWithNoETMPFORMBUNDLENUMBER =
    SubscriptionDisplayResponse(sampleResponseCommonWithNoETMPFORMBUNDLENUMBER, fullyPopulatedResponseDetail)

  val responseWithoutContactDetails =
    SubscriptionDisplayResponse(sampleResponseCommon, onlyMandatoryPopulatedResponseDetail)

  val responseWithoutEmailAddress = SubscriptionDisplayResponse(sampleResponseCommon, responseDetailWithoutEmail)

  val responseWithUnverifiedEmailAddress =
    SubscriptionDisplayResponse(sampleResponseCommon, responseDetailWithUnverifiedEmail)

  val responseWithoutPersonOfContact =
    SubscriptionDisplayResponse(sampleResponseCommon, responseDetailWithoutPersonOfContact)

  def fullyPopulatedContactDetails: ContactDetails =
    ContactDetails(
      contactName,
      emailAddress,
      telephoneNumber,
      Some(faxNumber),
      contactStreet,
      contactCity,
      Some(contactPostalCode),
      orgCountryCode
    )

}
