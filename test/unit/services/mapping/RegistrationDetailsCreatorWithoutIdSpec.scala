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

package unit.services.mapping

import org.scalacheck.Gen
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import util.scalacheck.TestDataGenerators.Implicits._

class RegistrationDetailsCreatorWithoutIdSpec extends RegistrationDetailsCreatorTestBase with TestMatchingModels {

  val registrationDetailsCreator = new RegistrationDetailsCreator()

  private val registerWithoutIdResponseDetailGen = for {
    arn <- Gen.alphaStr.asOption
  } yield RegisterWithoutIdResponseDetail("safe-id", arn)

  private val registerWithoutIDResponseGen = for {
    responseDetail <- registerWithoutIdResponseDetailGen
  } yield RegisterWithoutIDResponse(responseCommon, Some(responseDetail))

  private val organisationWithoutIdTestCases
    : Gen[((RegisterWithoutIDResponse, String, SixLineAddressMatchModel), RegistrationDetailsOrganisation)] = {
    val organisationAddressGen = for {
      addressLine1 <- Gen.alphaStr
      addressLine2 <- Gen.alphaStr.asOption
      addressLine3 <- Gen.alphaStr
      addressLine4 <- Gen.alphaStr.asOption
      postcode     <- Gen.alphaStr.asOption
      country      <- Gen.alphaStr
    } yield SixLineAddressMatchModel(
      lineOne = addressLine1,
      lineTwo = addressLine2,
      lineThree = addressLine3,
      lineFour = addressLine4,
      postcode = postcode,
      country = country
    )

    val organisationNameGen = for {
      name <- Gen.alphaStr
    } yield name

    for {
      response            <- registerWithoutIDResponseGen
      organisationName    <- organisationNameGen
      organisationAddress <- organisationAddressGen
    } yield (response, organisationName, organisationAddress) ->
      RegistrationDetailsOrganisation(
        customsId = None,
        sapNumber = TaxPayerId(sapNumber),
        safeId = SafeId("safe-id"),
        name = organisationName,
        address = Address(
          addressLine1 = organisationAddress.lineOne,
          addressLine2 = organisationAddress.lineTwo,
          addressLine3 = Some(organisationAddress.lineThree),
          addressLine4 = organisationAddress.lineFour,
          postalCode = organisationAddress.postcode,
          countryCode = organisationAddress.country
        ),
        dateOfEstablishment = None,
        etmpOrganisationType = None
      )
  }

  private val addressFromOrganisationAddressTestCases: Gen[(SixLineAddressMatchModel, Address)] = {
    val organisationAddressGen = for {
      addressLine1 <- Gen.alphaStr
      addressLine2 <- Gen.alphaStr.asOption
      addressLine3 <- Gen.alphaStr
      addressLine4 <- Gen.alphaStr.asOption
      postcode     <- Gen.alphaStr.asOption
      country      <- Gen.alphaStr
    } yield SixLineAddressMatchModel(
      lineOne = addressLine1,
      lineTwo = addressLine2,
      lineThree = addressLine3,
      lineFour = addressLine4,
      postcode = postcode,
      country = country
    )

    for {
      organisationAddress <- organisationAddressGen
    } yield organisationAddress ->
      Address(
        addressLine1 = organisationAddress.lineOne,
        addressLine2 = organisationAddress.lineTwo,
        addressLine3 = Some(organisationAddress.lineThree),
        addressLine4 = organisationAddress.lineFour,
        postalCode = organisationAddress.postcode,
        countryCode = organisationAddress.country
      )
  }

  private val addressFromAddressViewModelTestCases: Gen[(AddressViewModel, Address)] = {
    val addressViewModelGen = for {
      addressLine1 <- Gen.alphaStr
      addressLine2 <- Gen.alphaStr.asOption
      addressLine3 <- Gen.alphaStr
      postcode     <- Gen.alphaStr.asOption
      country      <- Gen.alphaStr
    } yield AddressViewModel(
      street = addressLine1 + " " + addressLine2,
      city = addressLine3,
      postcode = postcode,
      countryCode = country
    )

    for {
      addressViewModel <- addressViewModelGen
    } yield addressViewModel ->
      Address(
        addressLine1 = addressViewModel.street,
        addressLine2 = None,
        addressLine3 = Some(addressViewModel.city),
        addressLine4 = None,
        postalCode = addressViewModel.postcode,
        countryCode = addressViewModel.countryCode
      )
  }

  private val individualWithoutIdTestCases: Gen[
    ((RegisterWithoutIDResponse, IndividualNameAndDateOfBirth, SixLineAddressMatchModel), RegistrationDetailsIndividual)
  ] = {
    val individualNameAndDateOfBirthGen = for {
      firstName   <- Gen.alphaStr
      middleName  <- Gen.alphaStr.asOption
      lastName    <- Gen.alphaStr
      dateOfBirth <- dateOfBirthGenerator
    } yield IndividualNameAndDateOfBirth(
      firstName = firstName,
      middleName = middleName,
      lastName = lastName,
      dateOfBirth = dateOfBirth
    )

    val individualSoleTraderAddressGen = for {
      addressLine1 <- Gen.alphaStr
      addressLine2 <- Gen.alphaStr.asOption
      addressLine3 <- Gen.alphaStr
      addressLine4 <- Gen.alphaStr.asOption
      postcode     <- Gen.alphaStr.asOption
      country      <- Gen.alphaStr
    } yield SixLineAddressMatchModel(
      lineOne = addressLine1,
      lineTwo = addressLine2,
      lineThree = addressLine3,
      lineFour = addressLine4,
      postcode = postcode,
      country = country
    )

    for {
      response                        <- registerWithoutIDResponseGen
      individualData                  <- individualNameAndDateOfBirthGen
      individualSoleTraderAddressData <- individualSoleTraderAddressGen
    } yield (response, individualData, individualSoleTraderAddressData) ->
      RegistrationDetailsIndividual(
        customsId = None,
        sapNumber = TaxPayerId(sapNumber),
        safeId = SafeId("safe-id"),
        name = individualData.fullName,
        address = Address(
          addressLine1 = individualSoleTraderAddressData.lineOne,
          addressLine2 = individualSoleTraderAddressData.lineTwo,
          addressLine3 = Some(individualSoleTraderAddressData.lineThree),
          addressLine4 = individualSoleTraderAddressData.lineFour,
          postalCode = individualSoleTraderAddressData.postcode,
          countryCode = individualSoleTraderAddressData.country
        ),
        dateOfBirth = individualData.dateOfBirth
      )
  }

  "RegistrationDetailsCreator from RegisterWithoutIDResponse" should {

    "create organisation registration details" in testWithGen(organisationWithoutIdTestCases) {
      case ((registerWithoutIDResponse, organisationName, organisationAddress), expectedOrganisationDetails) =>
        registrationDetailsCreator.registrationDetails(
          registerWithoutIDResponse,
          organisationName,
          organisationAddress
        ) shouldBe expectedOrganisationDetails
    }

    "create Address from OrganisationAddress" in testWithGen(addressFromOrganisationAddressTestCases) {
      case (organisationAddress, expectedAddress) =>
        registrationDetailsCreator.registrationAddress(organisationAddress) shouldBe expectedAddress
    }

    "create Address from AddressViewModel" in testWithGen(addressFromAddressViewModelTestCases) {
      case (addressViewModel, expectedAddress) =>
        registrationDetailsCreator.registrationAddressFromAddressViewModel(addressViewModel) shouldBe expectedAddress
    }

    "create individual registration details" in testWithGen(individualWithoutIdTestCases) {
      case (
            (registerWithoutIDResponse, individualNameAndDateOfBirth, individualSoleTraderAddressData),
            expectedIndividualDetails
          ) =>
        registrationDetailsCreator.registrationDetails(
          registerWithoutIDResponse,
          individualNameAndDateOfBirth,
          individualSoleTraderAddressData
        ) shouldBe expectedIndividualDetails
    }

    "throw if organisation response does not provide SAP number" in testWithGen(organisationWithoutIdTestCases) {
      case ((validResponse, organisationName, organisationAddress), _) =>
        val withoutSap         = validResponse.responseCommon.copy(returnParameters = None)
        val responseWithoutSap = validResponse.copy(responseCommon = withoutSap)

        val caught = intercept[IllegalArgumentException] {
          registrationDetailsCreator.registrationDetails(responseWithoutSap, organisationName, organisationAddress)
        }

        caught.getMessage shouldBe "Invalid Response. SAP Number not returned by Messaging."
    }

    "throw if individual response does not provide SAP number" in testWithGen(individualWithoutIdTestCases) {
      case ((validResponse, individualNameAndDateOfBirth, individualSoleTraderAddressData), _) =>
        val withoutSap         = validResponse.responseCommon.copy(returnParameters = None)
        val responseWithoutSap = validResponse.copy(responseCommon = withoutSap)

        val caught = intercept[IllegalArgumentException] {
          registrationDetailsCreator.registrationDetails(
            responseWithoutSap,
            individualNameAndDateOfBirth,
            individualSoleTraderAddressData
          )
        }

        caught.getMessage shouldBe "Invalid Response. SAP Number not returned by Messaging."
    }
  }
}
