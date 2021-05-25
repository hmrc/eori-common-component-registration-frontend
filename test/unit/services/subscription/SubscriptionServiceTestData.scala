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

package unit.services.subscription

import common.support.testdata.TestData
import org.joda.time.{DateTime, LocalDate}
import org.scalacheck.Gen
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.{AddressViewModel, VatDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{
  EtmpLegalStatus,
  EtmpTypeOfPerson,
  OrganisationTypeConfiguration
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionSuccessful
import util.TestData

trait SubscriptionServiceTestData extends TestData {

  val sapNumber                          = "0123456789"
  val expectedTaxPayerId                 = "012345678900000000000000000000000000000000"
  val businessName                       = "Test Business Name not really a Ltd"
  val shortName                          = "tbnnraltd"
  val individualName                     = "John Doe"
  val dateOfBirthString                  = "1970-12-31"
  val dateOfBirth: LocalDate             = LocalDate.parse(dateOfBirthString)
  val dateEstablishedString              = "1963-05-01"
  val dateOfEstablishment: LocalDate     = LocalDate.parse(dateEstablishedString)
  val dateEstablishedStringForPublicBody = "1900-01-01"
  val principalEconomicActivity          = "A123"
  val ukVatDetails                       = Some(VatDetails("SE28 1AA", "123456789", LocalDate.parse("2017-01-01")))

  val contactName        = "John Doe"
  val contactStreet      = "Line 1"
  val contactCity        = "city name"
  val contactPostalCode  = "SE28 1AA"
  val contactCountryCode = "GB"
  val contactFax         = "01632961235"
  val contactTelephone   = "01632961234"
  val contactEmail       = "john.doe@example.com"
  val capturedEmail      = "captured@email.com"

  val EmptyVatIds: List[VatIdentification] = Nil

  val subscriptionContactDetailsModel = ContactDetailsModel(
    contactName,
    contactEmail,
    contactTelephone,
    Some(contactFax),
    useAddressFromRegistrationDetails = false,
    Some(contactStreet),
    Some(contactCity),
    Some(contactPostalCode),
    Some(contactCountryCode)
  )

  val subscriptionContactDetailsWithPlusSignInTelAndFaxModel = ContactDetailsModel(
    contactName,
    contactEmail,
    "+01632961234",
    Some("+01632961235"),
    useAddressFromRegistrationDetails = false,
    Some(contactStreet),
    Some(contactCity),
    Some(contactPostalCode),
    Some(contactCountryCode)
  )

  val responseEoriNumber                   = "ZZZ1ZZZZ23ZZZZZZZ"
  val responseFormBundleId: String         = "Form-Bundle-Id"
  val processingDateResponse: String       = "18 Aug 2016"
  val emailVerificationTimestamp: DateTime = TestData.emailVerificationTimestamp
  val eori                                 = Eori(responseEoriNumber)

  val subscriptionSuccessResult =
    SubscriptionSuccessful(eori, responseFormBundleId, processingDateResponse, Some(emailVerificationTimestamp))

  val cdsOrganisationTypeToTypeOfPersonMap: Map[CdsOrganisationType, OrganisationTypeConfiguration] = Map(
    CdsOrganisationType("company")                       -> OrganisationTypeConfiguration.Company,
    CdsOrganisationType("partnership")                   -> OrganisationTypeConfiguration.Partnership,
    CdsOrganisationType("limited-liability-partnership") -> OrganisationTypeConfiguration.LimitedLiabilityPartnership,
    CdsOrganisationType(
      "charity-public-body-not-for-profit"
    )                                                 -> OrganisationTypeConfiguration.CharityPublicBodyNotForProfit,
    CdsOrganisationType("eu-organisation")            -> OrganisationTypeConfiguration.EUOrganisation,
    CdsOrganisationType("third-country-organisation") -> OrganisationTypeConfiguration.ThirdCountryOrganisation
  )

  val etmpOrganisationTypeToTypeOfPersonMap: Map[EtmpOrganisationType, OrganisationTypeConfiguration] = Map(
    EtmpOrganisationType("Partnership")         -> OrganisationTypeConfiguration.EtmpPartnership,
    EtmpOrganisationType("LLP")                 -> OrganisationTypeConfiguration.EtmpLlp,
    EtmpOrganisationType("Corporate Body")      -> OrganisationTypeConfiguration.EtmpCorporateBody,
    EtmpOrganisationType("Unincorporated Body") -> OrganisationTypeConfiguration.EtmpUnincorporatedBody
  )

  val address: Address = Address("Line 1", Some("line 2"), Some("city name"), Some(""), Some("SE28 1AA"), "GB")

  val organisationRegistrationDetails: RegistrationDetailsOrganisation = RegistrationDetails.organisation(
    customsId = None,
    sapNumber = sapNumber,
    safeId = SafeId("safe-id"),
    name = businessName,
    address = address
  )

  val individualRegistrationDetails: RegistrationDetailsIndividual = RegistrationDetails.individual(
    customsId = None,
    sapNumber = sapNumber,
    safeId = SafeId("safe-id"),
    name = individualName,
    address = address,
    dateOfBirth = dateOfBirth
  )

  val fullyPopulatedSubscriptionDetails = SubscriptionDetails(
    ukVatDetails = Some(VatDetails("SE28 1AA", "123456789", LocalDate.parse("2017-01-01"))),
    personalDataDisclosureConsent = Some(true),
    contactDetails = Some(subscriptionContactDetailsModel),
    dateEstablished = Some(dateOfEstablishment),
    sicCode = Some(principalEconomicActivity),
    email = Some(capturedEmail),
    eoriNumber = Some("GB123456789000"),
    addressDetails = Some(AddressViewModel("Line 1 line 2", "city name", Some("SE28 1AA"), "GB"))
  )

  val fullyPopulatedSubscriptionDetailsWithPlusSignInTelephone: SubscriptionDetails = SubscriptionDetails(
    ukVatDetails = Some(VatDetails("SE28 1AA", "123456789", LocalDate.parse("2017-01-01"))),
    personalDataDisclosureConsent = Some(true),
    contactDetails = Some(subscriptionContactDetailsModel.copy(telephone = "+01632961234")),
    dateEstablished = Some(dateOfEstablishment),
    sicCode = Some(principalEconomicActivity),
    email = Some(capturedEmail)
  )

  val fullyPopulatedSubscriptionDetailsWithPlusSignInFaxNumber: SubscriptionDetails = SubscriptionDetails(
    ukVatDetails = Some(VatDetails("SE28 1AA", "123456789", LocalDate.parse("2017-01-01"))),
    personalDataDisclosureConsent = Some(true),
    contactDetails = Some(subscriptionContactDetailsModel.copy(fax = Some("+01632961234"))),
    dateEstablished = Some(dateOfEstablishment),
    sicCode = Some(principalEconomicActivity),
    email = Some(capturedEmail)
  )

  val fullyPopulatedSubscriptionDetailsWithPlusSignInTelAndFaxNumber: SubscriptionDetails = SubscriptionDetails(
    ukVatDetails = Some(VatDetails("SE28 1AA", "123456789", LocalDate.parse("2017-01-01"))),
    personalDataDisclosureConsent = Some(true),
    contactDetails = Some(subscriptionContactDetailsWithPlusSignInTelAndFaxModel),
    dateEstablished = Some(dateOfEstablishment),
    sicCode = Some(principalEconomicActivity),
    email = Some(capturedEmail)
  )

  def createVatIdentificationsGenerator: Gen[List[VatIdentification]] = {
    val CountryCodeLength    = 2
    val VatNumberMaxLength   = 15
    val vatNumberGenerator   = Gen.numStr retryUntil (_.length <= VatNumberMaxLength)
    val countryCodeGenerator = Gen.listOfN(CountryCodeLength, Gen.alphaChar) map (_.mkString)
    val vatIdentificationGenerator = for {
      countryCode <- Gen.option(countryCodeGenerator)
      vatNumber   <- Gen.option(vatNumberGenerator)
    } yield VatIdentification(countryCode, vatNumber)

    val minVatIdsCount = 0
    val maxVatIdsCount = 99
    for {
      numOfVats <- Gen.chooseNum(minVatIdsCount, maxVatIdsCount)
      vatIds    <- Gen.listOfN(numOfVats, vatIdentificationGenerator)
    } yield vatIds
  }

  def stubRegisterWithPartialResponse(outcomeType: String = "PASS"): RegisterWithEoriAndIdResponse = {
    val establishmentAddress =
      EstablishmentAddress(
        streetAndNumber = "Line 1 line 2",
        city = "city name",
        postalCode = Some("SE28 1AA"),
        countryCode = "GB"
      )
    val responseData = ResponseData(
      SAFEID = "SafeID123",
      trader = Trader(fullName = "Name", shortName = "nt"),
      establishmentAddress = establishmentAddress,
      hasInternetPublication = true,
      startDate = "2018-01-01",
      dateOfEstablishmentBirth = Some(dateEstablishedString)
    )
    val responseDetail = RegisterWithEoriAndIdResponseDetail(
      outcome = Some(outcomeType),
      caseNumber = Some("case no 1"),
      responseData = Some(responseData)
    )
    RegisterWithEoriAndIdResponse(
      ResponseCommon(status = "OK", processingDate = DateTime.now.withTimeAtStartOfDay()),
      Some(responseDetail)
    )
  }

  def stubRegisterWithPartialResponseWithNoDoe(outcomeType: String = "PASS"): RegisterWithEoriAndIdResponse = {
    val establishmentAddress =
      EstablishmentAddress(streetAndNumber = "Street", city = "city", postalCode = Some("NE1 1BG"), countryCode = "GB")
    val responseData = ResponseData(
      SAFEID = "SafeID123",
      trader = Trader(fullName = "Name", shortName = "nt"),
      establishmentAddress = establishmentAddress,
      hasInternetPublication = true,
      startDate = "2018-01-01",
      dateOfEstablishmentBirth = None
    )
    val responseDetail = RegisterWithEoriAndIdResponseDetail(
      outcome = Some(outcomeType),
      caseNumber = Some("case no 1"),
      responseData = Some(responseData)
    )
    RegisterWithEoriAndIdResponse(
      ResponseCommon(status = "OK", processingDate = DateTime.now.withTimeAtStartOfDay()),
      Some(responseDetail)
    )
  }

  def stubRegisterWithCompleteResponse(outcomeType: String = "PASS"): RegisterWithEoriAndIdResponse = {
    val processingDate = DateTime.now.withTimeAtStartOfDay()
    val contactDetailAddress =
      EstablishmentAddress(streetAndNumber = "Street", city = "city", postalCode = Some("NE1 1BG"), countryCode = "GB")
    val responseData = ResponseData(
      SAFEID = "SafeID123",
      trader = Trader(fullName = "Name", shortName = "nt"),
      establishmentAddress = EstablishmentAddress(
        streetAndNumber = "Line 1 line 2",
        city = "city name",
        postalCode = Some("SE28 1AA"),
        countryCode = "GB"
      ),
      hasInternetPublication = true,
      dateOfEstablishmentBirth = Some(dateEstablishedString),
      contactDetail = Some(
        ContactDetail(
          address = contactDetailAddress,
          contactName = "Name",
          phone = Some("00088000000"),
          fax = Some("00088000000"),
          email = Some("test@test.com")
        )
      ),
      VATIDs = Some(Seq(VatIds(countryCode = "AD", vatNumber = "4567"))),
      principalEconomicActivity = Some("p001"),
      hasEstablishmentInCustomsTerritory = Some(true),
      legalStatus = Some("Official"),
      thirdCountryIDNumber = Some(Seq("1234", "1222")),
      personType = Some(1),
      startDate = "2018-01-01",
      expiryDate = Some("2018-05-16")
    )

    val responseCommon = ResponseCommon(
      status = "OK",
      processingDate = processingDate,
      statusText = Some("Status Text OK"),
      returnParameters = Some(
        List(
          MessagingServiceParam(MessagingServiceParam.positionParamName, "POSITION"),
          MessagingServiceParam(MessagingServiceParam.Pending, "Pending")
        )
      )
    )
    val responseDetail = RegisterWithEoriAndIdResponseDetail(
      outcome = Some(outcomeType),
      caseNumber = Some("case no 1"),
      responseData = Some(responseData)
    )
    RegisterWithEoriAndIdResponse(responseCommon, Some(responseDetail))
  }

  private def vatIdsJson(vatIds: List[VatIdentification]): String = {
    def vatIdJson(v: VatIdentification): Option[String] = v match {
      case VatIdentification(None, None) => None
      case VatIdentification(Some(countryCode), Some(vatNumber)) =>
        Some(s"""{"countryCode": "$countryCode", "vatID": "$vatNumber"}""")
      case VatIdentification(Some(countryCode), None) => Some(s"""{"countryCode": "$countryCode"}""")
      case VatIdentification(None, Some(vatNumber))   => Some(s"""{"vatID": "$vatNumber"}""")
    }

    vatIds.filter(vatIdent => vatIdent.number.isDefined || vatIdent.countryCode.isDefined) match {
      case Nil => ""
      case vs  => s""""vatIDs": [${(vs flatMap vatIdJson).mkString(", ")}],"""
    }
  }

  def individualAutomaticSubscriptionRequestJson: JsValue =
    Json.parse(s"""
         |{
         |   "subscriptionCreateRequest":{
         |      "requestCommon":{
         |         "regime":"CDS",
         |         "receiptDate":"2016-08-18T14:00:05Z",
         |         "acknowledgementReference":"4482baa81c844d23a8db3fc180325e7a",
         |         "originatingSystem":"MDTP"
         |      },
         |      "requestDetail":{
         |         "SAFE":"safe-id",
         |         "EORINo":"$responseEoriNumber",
         |         "CDSFullName":"$individualName",
         |         "CDSEstablishmentAddress":{
         |            "streetAndNumber":"Line 1 line 2",
         |            "city":"city name",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB"
         |         },
         |         "typeOfLegalEntity": "Unincorporated Body",
         |         "contactInformation":{
         |            "personOfContact":"John Doe",
         |            "sepCorrAddrIndicator":true,
         |            "streetAndNumber":"Line 1",
         |            "city":"city name",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB",
         |            "telephoneNumber":"01632961234",
         |            "faxNumber":"01632961235",
         |            "emailAddress":"$contactEmail",
         |            "emailVerificationTimestamp": "$emailVerificationTimestamp"
         |         },
         |         "dateOfEstablishment":"$dateOfBirthString",
         |         "typeOfPerson": "1",
         |         "serviceName": "${atarService.enrolmentKey}"
         |      }
         |   }
         |}
      """.stripMargin)

  def individualAutomaticSubscriptionRequestWithCapturedEmailJson: JsValue =
    Json.parse(s"""
         |{
         |   "subscriptionCreateRequest":{
         |      "requestCommon":{
         |         "regime":"CDS",
         |         "receiptDate":"2016-08-18T14:00:05Z",
         |         "acknowledgementReference":"4482baa81c844d23a8db3fc180325e7a",
         |         "originatingSystem":"MDTP"
         |      },
         |      "requestDetail":{
         |         "SAFE":"safe-id",
         |         "EORINo":"$responseEoriNumber",
         |         "CDSFullName":"$individualName",
         |         "CDSEstablishmentAddress":{
         |            "streetAndNumber":"Line 1 line 2",
         |            "city":"city name",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB"
         |         },
         |         "typeOfLegalEntity": "Unincorporated Body",
         |         "contactInformation":{
         |            "personOfContact":"John Doe",
         |            "sepCorrAddrIndicator":true,
         |            "streetAndNumber":"Line 1",
         |            "city":"city name",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB",
         |            "telephoneNumber":"01632961234",
         |            "faxNumber":"01632961235",
         |            "emailAddress":"$capturedEmail",
         |            "emailVerificationTimestamp": "$emailVerificationTimestamp"
         |         },
         |         "dateOfEstablishment":"$dateOfBirthString",
         |         "typeOfPerson": "1",
         |         "serviceName": "${atarService.enrolmentKey}"
         |      }
         |   }
         |}
       """.stripMargin)

  def organisationAutomaticExistingRegistrationRequestJson(contactEmail: String): JsValue =
    Json.parse(
      s"""{"subscriptionCreateRequest":{"requestCommon":{"regime":"CDS","receiptDate":"2016-08-18T14:00:05Z","acknowledgementReference":"4482baa81c844d23a8db3fc180325e7a","originatingSystem":"MDTP"},"requestDetail":{"SAFE":"SafeID123","EORINo":"GB123456789000","CDSFullName":"Name","CDSEstablishmentAddress":{"streetAndNumber":"Line 1 line 2","city":"city name","postalCode":"SE28 1AA","countryCode":"GB"},"contactInformation":{"city":"-","emailAddress":"$contactEmail", "emailVerificationTimestamp": "$emailVerificationTimestamp"},"shortName":"nt","dateOfEstablishment":"1963-05-01","serviceName":"${atarService.enrolmentKey}"}}}""".stripMargin
    )

  def existingRegistrationSubcriptionRequestJson(contactEmail: String): JsValue =
    Json.parse(s"""{
         |  "subscriptionCreateRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-08-18T14:00:05Z",
         |      "acknowledgementReference": "4482baa81c844d23a8db3fc180325e7a",
         |      "originatingSystem": "MDTP"
         |    },
         |    "requestDetail": {
         |      "SAFE": "SafeID123",
         |      "EORINo": "GB123456789000",
         |      "CDSFullName": "Name",
         |      "CDSEstablishmentAddress": {
         |        "streetAndNumber": "Line 1 line 2",
         |        "city": "city name",
         |        "postalCode": "SE28 1AA",
         |        "countryCode": "GB"
         |      },
         |      "establishmentInTheCustomsTerritoryOfTheUnion": "1",
         |      "typeOfLegalEntity": "Official",
         |      "contactInformation":{"personOfContact":"Name","sepCorrAddrIndicator":true,"streetAndNumber":"Street","city":"city","postalCode":"NE1 1BG","countryCode":"GB","telephoneNumber":"00088000000","faxNumber":"00088000000","emailAddress":"$contactEmail", "emailVerificationTimestamp": "$emailVerificationTimestamp"},
         |      "vatIDs": [
         |        {
         |          "countryCode": "AD",
         |          "vatID": "4567"
         |        }
         |      ],
         |      "shortName": "nt",
         |      "dateOfEstablishment": "1963-05-01",
         |      "typeOfPerson": "1",
         |      "principalEconomicActivity": "p001",
         |      "serviceName": "${atarService.enrolmentKey}"
         |    }
         |  }
         |}
      """.stripMargin)

  def organisationAutomaticSubscriptionRequestJson: JsValue =
    Json.parse(s"""
         |{
         |   "subscriptionCreateRequest":{
         |      "requestCommon":{
         |         "regime":"CDS",
         |         "receiptDate":"2016-08-18T14:00:05Z",
         |         "acknowledgementReference":"4482baa81c844d23a8db3fc180325e7a",
         |         "originatingSystem":"MDTP"
         |      },
         |      "requestDetail":{
         |         "SAFE":"safe-id",
         |         "EORINo":"$responseEoriNumber",
         |         "CDSFullName":"$businessName",
         |         "CDSEstablishmentAddress":{
         |            "streetAndNumber":"Line 1 line 2",
         |            "city":"city name",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB"
         |         },
         |         "typeOfLegalEntity":"Corporate Body",
         |         "contactInformation":{
         |            "personOfContact":"John Doe",
         |            "sepCorrAddrIndicator":true,
         |            "streetAndNumber":"Line 1",
         |            "city":"city name",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB",
         |            "telephoneNumber":"01632961234",
         |            "faxNumber":"01632961235",
         |            "emailAddress":"$contactEmail",
         |            "emailVerificationTimestamp": "$emailVerificationTimestamp"
         |         },
         |         "dateOfEstablishment":"$dateEstablishedString",
         |         "typeOfPerson": "2",
         |         "serviceName": "${atarService.enrolmentKey}"
         |      }
         |   }
         |}
      """.stripMargin)

  def organisationAutomaticSubscriptionRequestWithoutServiceNameJson: JsValue =
    Json.parse(s"""
                  |{
                  |   "subscriptionCreateRequest":{
                  |      "requestCommon":{
                  |         "regime":"CDS",
                  |         "receiptDate":"2016-08-18T14:00:05Z",
                  |         "acknowledgementReference":"4482baa81c844d23a8db3fc180325e7a",
                  |         "originatingSystem":"MDTP"
                  |      },
                  |      "requestDetail":{
                  |         "SAFE":"safe-id",
                  |         "EORINo":"$responseEoriNumber",
                  |         "CDSFullName":"$businessName",
                  |         "CDSEstablishmentAddress":{
                  |            "streetAndNumber":"Line 1 line 2",
                  |            "city":"city name",
                  |            "postalCode":"SE28 1AA",
                  |            "countryCode":"GB"
                  |         },
                  |         "typeOfLegalEntity":"Corporate Body",
                  |         "contactInformation":{
                  |            "personOfContact":"John Doe",
                  |            "sepCorrAddrIndicator":true,
                  |            "streetAndNumber":"Line 1",
                  |            "city":"city name",
                  |            "postalCode":"SE28 1AA",
                  |            "countryCode":"GB",
                  |            "telephoneNumber":"01632961234",
                  |            "faxNumber":"01632961235",
                  |            "emailAddress":"$contactEmail",
                  |            "emailVerificationTimestamp": "$emailVerificationTimestamp"
                  |         },
                  |         "dateOfEstablishment":"$dateEstablishedString",
                  |         "typeOfPerson": "2"
                  |      }
                  |   }
                  |}
      """.stripMargin)

  def requestJsonIndividual(
    name: String,
    vatIds: List[VatIdentification],
    organisationType: Option[CdsOrganisationType],
    expectedDateOfBirthString: String = dateOfBirthString
  ): JsValue = {

    val typeOfPersonJson: String =
      determineTypeOfPersonJson(organisationType.map(x => EtmpOrganisationType.apply(x)), false)
    val typeOfLegalStatusJson: String =
      determineLegalStatus(organisationType.map(x => EtmpOrganisationType.apply(x)), false)

    Json.parse(s"""
         | {
         | "subscriptionCreateRequest": {
         | "requestCommon": {
         | "regime": "CDS",
         | "receiptDate": "2016-08-18T14:00:05Z",
         | "acknowledgementReference": "4482baa81c844d23a8db3fc180325e7a",
         | "originatingSystem":"MDTP"
         |},
         | "requestDetail": {
         | "SAFE": "safe-id",
         | "CDSFullName": "$name",
         | "CDSEstablishmentAddress": {
         | "streetAndNumber": "Line 1 line 2",
         | "city": "city name",
         | "postalCode": "SE28 1AA",
         | "countryCode": "GB"
         |},
         | $typeOfLegalStatusJson
         |         "contactInformation": {
         |           "personOfContact": "$contactName",
         |           "sepCorrAddrIndicator": true,
         |           "streetAndNumber": "$contactStreet",
         |           "city": "$contactCity",
         |           "postalCode": "$contactPostalCode",
         |           "countryCode": "$contactCountryCode",
         |           "telephoneNumber": "$contactTelephone",
         |           "faxNumber": "$contactFax",
         |           "emailAddress": "$contactEmail",
         |           "emailVerificationTimestamp": "$emailVerificationTimestamp"
         |         },
         | ${vatIdsJson(vatIds)}
         | "consentToDisclosureOfPersonalData": "0",
         | $typeOfPersonJson
         | "dateOfEstablishment": "$expectedDateOfBirthString",
         | "serviceName": "${atarService.enrolmentKey}"
         |
         |}
         |}
        }
        """.stripMargin)
  }

  def requestJson(
    name: String,
    vatIds: List[VatIdentification],
    organisationType: Option[EtmpOrganisationType],
    isOrganisationEvenIfOrganisationTypeIsNone: Boolean = false,
    expectedDateEstablishedString: String = dateEstablishedString
  ): JsValue = {
    val typeOfPersonJson: String =
      determineTypeOfPersonJson(organisationType, isOrganisationEvenIfOrganisationTypeIsNone)
    val typeOfLegalStatusJson: String =
      determineLegalStatus(organisationType, isOrganisationEvenIfOrganisationTypeIsNone)

    Json.parse(s"""
         | {
         | "subscriptionCreateRequest": {
         | "requestCommon": {
         | "regime": "CDS",
         | "receiptDate": "2016-08-18T14:00:05Z",
         | "acknowledgementReference": "4482baa81c844d23a8db3fc180325e7a",
         | "originatingSystem":"MDTP"
         |},
         | "requestDetail": {
         | "SAFE": "safe-id",
         | "CDSFullName": "$name",
         | "CDSEstablishmentAddress": {
         | "streetAndNumber": "Line 1 line 2",
         | "city": "city name",
         | "postalCode": "SE28 1AA",
         | "countryCode": "GB"
         |},
         | $typeOfLegalStatusJson
         |         "contactInformation": {
         |           "personOfContact": "$contactName",
         |           "sepCorrAddrIndicator": true,
         |           "streetAndNumber": "$contactStreet",
         |           "city": "$contactCity",
         |           "postalCode": "$contactPostalCode",
         |           "countryCode": "$contactCountryCode",
         |           "telephoneNumber": "$contactTelephone",
         |           "faxNumber": "$contactFax",
         |           "emailAddress": "$contactEmail",
         |           "emailVerificationTimestamp": "$emailVerificationTimestamp"
         |         },
         | ${vatIdsJson(vatIds)}
         | "consentToDisclosureOfPersonalData": "0",
         | "shortName": "$shortName",
         | "dateOfEstablishment": "$expectedDateEstablishedString",
         | $typeOfPersonJson
         | "principalEconomicActivity": "$principalEconomicActivity",
         | "serviceName": "${atarService.enrolmentKey}"
         |}
         |}
         |
        }
        """.stripMargin)
  }

  def determineTypeOfPersonJson(
    organisationType: Option[EtmpOrganisationType],
    isOrganisationEvenIfOrganisationTypeIsNone: Boolean
  ): String = {
    val typeOfPerson = organisationType match {
      case Some(o: EtmpOrganisationType)                      => Some(etmpOrganisationTypeToTypeOfPersonMap(o).typeOfPerson)
      case None if isOrganisationEvenIfOrganisationTypeIsNone => None
      case _                                                  => Some(EtmpTypeOfPerson.NaturalPerson)
    }

    typeOfPerson match {
      case None             => ""
      case Some(personType) => s""""typeOfPerson": "$personType", """
    }
  }

  def determineLegalStatus(
    organisationType: Option[EtmpOrganisationType],
    isOrganisationEvenIfOrganisationTypeIsNone: Boolean
  ): String = {
    val legalStatus = organisationType match {
      case Some(o: CdsOrganisationType)  => Some(cdsOrganisationTypeToTypeOfPersonMap(o).legalStatus)
      case Some(o: EtmpOrganisationType) => Some(etmpOrganisationTypeToTypeOfPersonMap(o).legalStatus)
      case None                          => if (isOrganisationEvenIfOrganisationTypeIsNone) None else Some(EtmpLegalStatus.UnincorporatedBody)
    }

    legalStatus match {
      case None         => ""
      case Some(status) => s""""typeOfLegalEntity": "$status","""
    }
  }

  val subscriptionGenerateResponseJson: JsValue =
    Json.parse(s"""
         | {
         | "subscriptionCreateResponse": {
         | "responseCommon": {
         | "status": "OK",
         | "processingDate": "2016-08-18T14:01:05Z",
         | "returnParameters": [
         | {
         | "paramName": "ETMPFORMBUNDLENUMBER",
         | "paramValue": "$responseFormBundleId"
         |},
         | {
         | "paramName": "POSITION",
         | "paramValue": "GENERATE"
         |}
         |]
         |},
         | "responseDetail": {
         | "EORINo": "$responseEoriNumber"
         |}
         |}
         |
        }
        """.stripMargin)

  val subscriptionLinkedResponseJson: JsValue =
    Json.parse(s"""
         | {
         | "subscriptionCreateResponse": {
         | "responseCommon": {
         | "status": "OK",
         | "processingDate": "2016-08-18T14:01:05Z",
         | "returnParameters": [
         | {
         | "paramName": "ETMPFORMBUNDLENUMBER",
         | "paramValue": "$responseFormBundleId"
         |},
         | {
         | "paramName": "POSITION",
         | "paramValue": "LINK"
         |}
         |]
         |},
         | "responseDetail": {
         | "EORINo": "$responseEoriNumber"
         |}
         |}
         |
        }
        """.stripMargin)

  val subscriptionPendingResponseJson: JsValue =
    Json.parse(s"""
         | {
         | "subscriptionCreateResponse": {
         | "responseCommon": {
         | "status": "OK",
         | "processingDate": "2016-08-18T14:01:05Z",
         | "returnParameters": [
         | {
         | "paramName": "ETMPFORMBUNDLENUMBER",
         | "paramValue": "$responseFormBundleId"
         |},
         | {
         | "paramName": "POSITION",
         | "paramValue": "WORKLIST"
         |}
         |]
         |}
         |}
        }
        """.stripMargin)

  val subscriptionResponseWithoutPositionJson: JsValue =
    Json.parse(s"""
         | {
         | "subscriptionCreateResponse": {
         | "responseCommon": {
         | "status": "OK",
         | "processingDate": "2016-08-18T14:01:05Z",
         | "returnParameters": [
         | {
         | "paramName": "ETMPFORMBUNDLENUMBER",
         | "paramValue": "$responseFormBundleId"
         |}
         |]
         |}
         |}
      }
      """.stripMargin)

  def subscriptionFailedResponseJson(statusText: String = ""): JsValue =
    Json.parse(s"""
         | {
         | "subscriptionCreateResponse": {
         | "responseCommon": {
         | "status": "OK",
         | "statusText": "$statusText",
         | "processingDate": "2016-08-18T14:01:05Z",
         | "returnParameters": [
         | {
         | "paramName": "POSITION",
         | "paramValue": "FAIL"
         |}
         |]
         |}
         |}
        }
        """.stripMargin)

  def subscriptionResponseWithoutFormBundleIdJson(position: String): JsValue =
    Json.parse(s"""
         |{
         |  "subscriptionCreateResponse": {
         |    "responseCommon": {
         |      "status": "OK",
         |      "processingDate": "2016-08-18T14:01:05Z",
         |      "returnParameters": [
         |        {
         |          "paramName": "POSITION",
         |          "paramValue": "$position"
         |        }
         |      ]
         |    },
         |    "responseDetail": {
         |      "EORINo": "$responseEoriNumber"
         |    }
         |  }
         |}
      """.stripMargin)

  val subscriptionGenerateResponse: SubscriptionResponse = subscriptionGenerateResponseJson.as[SubscriptionResponse]

  val subscriptionResponseWithoutPosition: SubscriptionResponse =
    subscriptionResponseWithoutPositionJson.as[SubscriptionResponse]

  val successfulPositionValues = Table("positionValue", "GENERATE", "LINK", "WORKLIST")
}
