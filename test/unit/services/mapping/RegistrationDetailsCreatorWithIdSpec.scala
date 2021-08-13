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

import java.time.LocalDate
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import util.scalacheck.TestDataGenerators.Implicits._

class RegistrationDetailsCreatorWithIdSpec extends RegistrationDetailsCreatorTestBase with TestMatchingModels {

  val registrationDetailsCreator = new RegistrationDetailsCreator()

  private val addressGen = for {
    addressLine1 <- Gen.alphaStr
    addressLine2 <- Gen.alphaStr.asOption
    addressLine3 <- Gen.alphaStr.asOption
    addressLine4 <- Gen.alphaStr.asOption
    postcode     <- Gen.alphaStr.asOption
    country      <- Gen.alphaStr
  } yield Address(addressLine1, addressLine2, addressLine3, addressLine4, postcode, country)

  private val contactResponseGen = for {
    phoneNumber  <- Gen.alphaStr.asOption
    mobileNumber <- Gen.alphaStr.asOption
    faxNumber    <- Gen.alphaStr.asOption
    emailAddress <- Gen.alphaStr.asOption
  } yield ContactResponse(phoneNumber, mobileNumber, faxNumber, emailAddress)

  private val customsIdGen = Gen.oneOf(Utr(uuid()), Eori(uuid()), Nino(uuid()))

  type FromMatchingWithIdArguments = (RegisterWithIDResponse, CustomsId, Option[LocalDate])

  private val organisationWithIdTestCases: Gen[(FromMatchingWithIdArguments, RegistrationDetailsOrganisation)] = {
    val organisationResponseGen = for {
      organisationName <- Gen.alphaStr
      code             <- Gen.alphaStr.asOption
      isAGroup         <- Arbitrary.arbitrary[Boolean].asOption
    } yield OrganisationResponse(organisationName, code, isAGroup, Some("Partnership"))

    def organisationResponseDetailGen(organisationResponse: OrganisationResponse, address: Address) =
      for {
        arn             <- Gen.alphaStr.asOption
        isEditable      <- Arbitrary.arbitrary[Boolean]
        isAnAgent       <- Arbitrary.arbitrary[Boolean]
        contactResponse <- contactResponseGen
      } yield ResponseDetail(
        SAFEID = "safe-id",
        ARN = arn,
        isEditable = isEditable,
        isAnAgent = isAnAgent,
        isAnIndividual = false,
        individual = None,
        organisation = Some(organisationResponse),
        address = address,
        contactDetails = contactResponse
      )

    for {
      organisationResponse       <- organisationResponseGen
      address                    <- addressGen
      organisationResponseDetail <- organisationResponseDetailGen(organisationResponse, address)
      customsId                  <- customsIdGen
      capturedDate               <- dateOfBirthGenerator.asOption
    } yield (RegisterWithIDResponse(responseCommon, Some(organisationResponseDetail)), customsId, capturedDate) ->
      RegistrationDetailsOrganisation(
        customsId = Some(customsId),
        sapNumber = TaxPayerId(sapNumber),
        safeId = SafeId("safe-id"),
        name = organisationResponse.organisationName,
        address = address,
        dateOfEstablishment = capturedDate,
        etmpOrganisationType = organisationResponse.organisationType.map(EtmpOrganisationType(_))
      )
  }

  private val individualWithIdTestCases: Gen[(FromMatchingWithIdArguments, RegistrationDetailsIndividual)] = {
    def individualResponseGen(dateOfBirth: Option[String]) =
      for {
        firstName  <- Gen.alphaStr
        middleName <- Gen.alphaStr.asOption
        lastName   <- Gen.alphaStr
      } yield IndividualResponse(
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        dateOfBirth = dateOfBirth
      )

    def individualResponseDetailGen(individualResponse: IndividualResponse, address: Address) =
      for {
        arn             <- Gen.alphaStr.asOption
        isEditable      <- Arbitrary.arbitrary[Boolean]
        isAnAgent       <- Arbitrary.arbitrary[Boolean]
        contactResponse <- contactResponseGen
      } yield ResponseDetail(
        SAFEID = "safe-id",
        ARN = arn,
        isEditable = isEditable,
        isAnAgent = isAnAgent,
        isAnIndividual = true,
        individual = Some(individualResponse),
        organisation = None,
        address = address,
        contactDetails = contactResponse
      )

    for {
      dateOfBirthInResponse <- dateOfBirthGenerator.asOption
      individualResponse    <- individualResponseGen(dateOfBirthInResponse.map(_.toString))
      address               <- addressGen

      individualResponseDetail <- individualResponseDetailGen(individualResponse, address)
      customsId                <- customsIdGen
      capturedDate <- if (dateOfBirthInResponse.isEmpty) dateOfBirthGenerator.asMandatoryOption
      else dateOfBirthGenerator.asOption
    } yield (RegisterWithIDResponse(responseCommon, Some(individualResponseDetail)), customsId, capturedDate) ->
      RegistrationDetailsIndividual(
        customsId = Some(customsId),
        sapNumber = TaxPayerId(sapNumber),
        safeId = SafeId("safe-id"),
        name = individualResponse.fullName,
        address = address,
        dateOfBirth = (dateOfBirthInResponse orElse capturedDate).getOrElse(fail("Test data error"))
      )
  }

  "RegistrationDetailsCreator from RegisterWithIDResponse" should {

    "create organisation registration details" in testWithGen(organisationWithIdTestCases) {
      case ((response, customsId, capturedDate), expectedOrganisationDetails) =>
        registrationDetailsCreator.registrationDetails(
          response,
          customsId,
          capturedDate
        ) shouldBe expectedOrganisationDetails
    }

    "create individual registration details" in testWithGen(individualWithIdTestCases) {
      case ((response, customsId, capturedDate), expectedIndividualDetails) =>
        registrationDetailsCreator.registrationDetails(
          response,
          customsId,
          capturedDate
        ) shouldBe expectedIndividualDetails
    }

    "throw if organisation response does not provide SAP number" in testWithGen(organisationWithIdTestCases) {
      case ((validResponse, customsId, capturedDate), _) =>
        val withoutSap         = validResponse.responseCommon.copy(returnParameters = None)
        val responseWithoutSap = validResponse.copy(responseCommon = withoutSap)

        val caught = intercept[IllegalArgumentException] {
          registrationDetailsCreator.registrationDetails(responseWithoutSap, customsId, capturedDate)
        }

        caught.getMessage shouldBe "Invalid Response. SAP Number not returned by Messaging."
    }

    "throw if individual response does not provide SAP number" in testWithGen(individualWithIdTestCases) {
      case ((validResponse, customsId, capturedDate), _) =>
        val withoutSap         = validResponse.responseCommon.copy(returnParameters = None)
        val responseWithoutSap = validResponse.copy(responseCommon = withoutSap)

        val caught = intercept[IllegalArgumentException] {
          registrationDetailsCreator.registrationDetails(responseWithoutSap, customsId, capturedDate)
        }

        caught.getMessage shouldBe "Invalid Response. SAP Number not returned by Messaging."
    }

    "throw if individual date of birth neither found in RegisterWithIDResponse nor provided" in testWithGen(
      individualWithIdTestCases
    ) {
      case ((validResponse, customsId, _), _) =>
        val Some(responseDetail)      = validResponse.responseDetail
        val withoutDateOfBirth        = responseDetail.individual.map(_.copy(dateOfBirth = None))
        val responseDetailWithoutDate = responseDetail.copy(individual = withoutDateOfBirth)
        val response                  = validResponse.copy(responseDetail = Some(responseDetailWithoutDate))

        val caught = intercept[IllegalArgumentException] {
          registrationDetailsCreator.registrationDetails(response, customsId, capturedDate = None)
        }

        caught.getMessage shouldBe "Date of Birth is neither provided in registration response nor captured in the application page"
    }
  }
}
