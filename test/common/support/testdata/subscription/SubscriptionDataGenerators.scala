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

package common.support.testdata.subscription

import org.scalacheck.Gen
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.VatIdentification
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.ContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel

trait SubscriptionDataGenerators {

  private val stringWithoutEmptyString: Gen[String] = Gen.alphaStr.filter(_ != "")

  val trueFalseGenerator: Gen[Boolean] = Gen.oneOf(true, false)

  val addressGenerator: Gen[Address] = for {
    addressLine1 <- stringWithoutEmptyString
    addressLine2 <- Gen.option(stringWithoutEmptyString)
    addressLine3 <- Gen.option(stringWithoutEmptyString)
    addressLine4 <- Gen.option(stringWithoutEmptyString)
    postcode     <- Gen.option(stringWithoutEmptyString)
    country      <- stringWithoutEmptyString
  } yield Address(addressLine1, addressLine2, addressLine3, addressLine4, postcode, country)

  val addressGeneratorWithoutPostCode: Gen[Address] = for {
    addressLine1 <- stringWithoutEmptyString
    addressLine2 <- Gen.option(stringWithoutEmptyString)
    addressLine3 <- Gen.option(stringWithoutEmptyString)
    addressLine4 <- Gen.option(stringWithoutEmptyString)
    country      <- stringWithoutEmptyString
  } yield Address(addressLine1, addressLine2, addressLine3, addressLine4, Some(""), country)

  val subscriptionContactDetailsGenerator: Gen[ContactDetails] = for {
    faxOption        <- Gen.option("01632961234")
    postalCodeOption <- Gen.option("SE28 1AA")
  } yield ContactDetails(
    "John Doe",
    "john.doe@example.com",
    "01632961234",
    faxOption,
    "Line 1",
    "city name",
    postalCodeOption,
    "ZZ"
  )

  val subscriptionContactDetailsWithoutPostcodeGenerator: Gen[ContactDetails] = for {
    faxOption <- Gen.option("07893345672")
  } yield ContactDetails(
    "John Doe",
    "john.doe@example.com",
    "01632961234",
    faxOption,
    "Line 1",
    "city name",
    Some(""),
    "ZZ"
  )

  val subscriptionInfoServiceContactInformationGenerator: Gen[ContactInformation] = for {
    personOfContactGen      <- Gen.option(stringWithoutEmptyString)
    sepCorrAddrIndicatorGen <- Gen.option(trueFalseGenerator)
    streetAndNumberGen      <- Gen.option(stringWithoutEmptyString)
    cityGen                 <- Gen.option(stringWithoutEmptyString)
    postalCodeGen           <- Gen.option(stringWithoutEmptyString)
    countryCodeGen          <- Gen.option(stringWithoutEmptyString)
    telephoneNumberGen      <- Gen.option(stringWithoutEmptyString)
    faxNumberGen            <- Gen.option(stringWithoutEmptyString)
    emailAddressGen         <- Gen.option(stringWithoutEmptyString)
  } yield ContactInformation(
    personOfContact = personOfContactGen,
    sepCorrAddrIndicator = sepCorrAddrIndicatorGen,
    streetAndNumber = streetAndNumberGen,
    city = cityGen,
    postalCode = postalCodeGen,
    countryCode = countryCodeGen,
    telephoneNumber = telephoneNumberGen,
    faxNumber = faxNumberGen,
    emailAddress = emailAddressGen
  )

  val subscriptionInfoServiceContactInformationWithEmptyPostcodeGenerator: Gen[ContactInformation] = for {
    personOfContactGen      <- Gen.option(stringWithoutEmptyString)
    sepCorrAddrIndicatorGen <- Gen.option(trueFalseGenerator)
    streetAndNumberGen      <- Gen.option(stringWithoutEmptyString)
    cityGen                 <- Gen.option(stringWithoutEmptyString)
    countryCodeGen          <- Gen.option(stringWithoutEmptyString)
    telephoneNumberGen      <- Gen.option(stringWithoutEmptyString)
    faxNumberGen            <- Gen.option(stringWithoutEmptyString)
    emailAddressGen         <- Gen.option(stringWithoutEmptyString)
  } yield ContactInformation(
    personOfContact = personOfContactGen,
    sepCorrAddrIndicator = sepCorrAddrIndicatorGen,
    streetAndNumber = streetAndNumberGen,
    city = cityGen,
    postalCode = Some(""),
    countryCode = countryCodeGen,
    telephoneNumber = telephoneNumberGen,
    faxNumber = faxNumberGen,
    emailAddress = emailAddressGen
  )

  val contactDetailsCreateViewModelGenerator: Gen[ContactDetailsModel] = for {
    fullName             <- stringWithoutEmptyString
    emailAddress         <- stringWithoutEmptyString
    telephone            <- stringWithoutEmptyString
    fax                  <- Gen.option(stringWithoutEmptyString)
    useRegisteredAddress <- trueFalseGenerator
    street               <- stringWithoutEmptyString.map(Some(_))
    city                 <- stringWithoutEmptyString.map(Some(_))
    postcode             <- Gen.option(stringWithoutEmptyString)
    countryCode          <- stringWithoutEmptyString.map(Some(_))
  } yield ContactDetailsModel(
    fullName = fullName,
    emailAddress = emailAddress,
    telephone = telephone,
    fax = fax,
    useAddressFromRegistrationDetails = useRegisteredAddress,
    street = street,
    city = city,
    postcode = postcode,
    countryCode = countryCode
  )

  val contactDetailsCreateViewModelWithEmptyPostcodeGenerator: Gen[ContactDetailsModel] = for {
    fullName             <- stringWithoutEmptyString
    emailAddress         <- stringWithoutEmptyString
    telephone            <- stringWithoutEmptyString
    fax                  <- Gen.option(stringWithoutEmptyString)
    useRegisteredAddress <- trueFalseGenerator
    street               <- stringWithoutEmptyString.map(Some(_))
    city                 <- stringWithoutEmptyString.map(Some(_))
    countryCode          <- stringWithoutEmptyString.map(Some(_))
  } yield ContactDetailsModel(
    fullName = fullName,
    emailAddress = emailAddress,
    telephone = telephone,
    fax = fax,
    useAddressFromRegistrationDetails = useRegisteredAddress,
    street = street,
    city = city,
    postcode = Some(""),
    countryCode = countryCode
  )

  val contactDetailsCreateViewModelMissingAddressFieldsGenerator: Gen[ContactDetailsModel] = for {
    fullName             <- stringWithoutEmptyString
    emailAddress         <- stringWithoutEmptyString
    telephone            <- stringWithoutEmptyString
    fax                  <- Gen.option(stringWithoutEmptyString)
    useRegisteredAddress <- trueFalseGenerator
    street               <- Gen.option(stringWithoutEmptyString)
    city                 <- Gen.option(stringWithoutEmptyString)
    postcode             <- Gen.option(stringWithoutEmptyString)
    countryCode          <- if (street.isDefined && city.isDefined) Gen.const(None) else Gen.option(stringWithoutEmptyString)
  } yield ContactDetailsModel(
    fullName = fullName,
    emailAddress = emailAddress,
    telephone = telephone,
    fax = fax,
    useAddressFromRegistrationDetails = useRegisteredAddress,
    street = street,
    city = city,
    postcode = postcode,
    countryCode = countryCode
  )

  val contactDetailsCreateViewModelAndAddressGenerator: Gen[(ContactDetailsModel, Address)] = for {
    model   <- contactDetailsCreateViewModelGenerator
    address <- addressGenerator
  } yield model -> address

  val contactDetailsCreateViewModelAndAddressWithEmptyPostcodeGenerator: Gen[(ContactDetailsModel, Address)] = for {
    model   <- contactDetailsCreateViewModelGenerator
    address <- addressGeneratorWithoutPostCode
  } yield model -> address

  val vatIdentificationGenerator: Gen[VatIdentification] = for {
    vatNumberGen   <- Gen.option(stringWithoutEmptyString)
    countryCodeGen <- Gen.option("ZZ")
  } yield VatIdentification(countryCodeGen, vatNumberGen)

  def vatIdentificationsGenerator(
    vatIdGen: Gen[VatIdentification] = vatIdentificationGenerator
  ): Gen[List[VatIdentification]] = {
    val minVatIdsCount = 0
    val maxVatIdsCount = 99
    for {
      numOfVats <- Gen.chooseNum(minVatIdsCount, maxVatIdsCount)
      vatIds    <- Gen.listOfN(numOfVats, vatIdGen)
    } yield vatIds
  }

  val yesNoOrOtherGenerator   = Gen.oneOf("yEs", "Yes", "YES", "yes", "No", "no", "NO", "nO", "other", None)
  val oneZeroOrOtherGenerator = Gen.option(Gen.oneOf("1", "0", "other"))

  val tenCharStringGen: Gen[String] = for {
    first   <- Gen.alphaChar
    second  <- Gen.alphaChar
    third   <- Gen.alphaChar
    fourth  <- Gen.alphaChar
    fifth   <- Gen.alphaChar
    sixth   <- Gen.alphaChar
    seventh <- Gen.alphaChar
    eighth  <- Gen.alphaChar
    ninth   <- Gen.alphaChar
    tenth   <- Gen.alphaChar
  } yield "" + first + second + third + fourth + fifth + sixth + seventh + eighth + ninth + tenth

  val sapNumberGenerator = tenCharStringGen
}
