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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping

import javax.inject.Singleton
import org.joda.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{
  IndividualResponse,
  OrganisationResponse,
  RegisterWithIDResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.RegistrationDisplayResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel

@Singleton
class RegistrationDetailsCreator {

  def registrationDetails(
    response: RegisterWithIDResponse,
    customsId: CustomsId,
    capturedDate: Option[LocalDate]
  ): RegistrationDetails = {
    val Some(responseDetail) = response.responseDetail
    val sapNumber            = extractSapNumber(response.responseCommon.returnParameters)
    if (responseDetail.isAnIndividual)
      convertIndividualMatchingResponse(
        responseDetail.individual.get,
        Some(customsId),
        sapNumber,
        SafeId(responseDetail.SAFEID),
        responseDetail.address,
        dateOfBirth = capturedDate
      )
    else
      convertOrganisationMatchingResponse(
        responseDetail.organisation.get,
        Some(customsId),
        sapNumber,
        SafeId(responseDetail.SAFEID),
        responseDetail.address,
        dateOfEstablishment = capturedDate
      )
  }

  def extractSapNumber(returnParameters: Option[List[MessagingServiceParam]]): String =
    returnParameters
      .getOrElse(List.empty)
      .find(_.paramName == "SAP_NUMBER")
      .fold(throw new IllegalArgumentException("Invalid Response. SAP Number not returned by Messaging."))(_.paramValue)

  private def convertIndividualMatchingResponse(
    individualResponse: IndividualResponse,
    customsId: Option[CustomsId],
    sapNumber: String,
    safeId: SafeId,
    address: Address,
    dateOfBirth: Option[LocalDate]
  ): RegistrationDetailsIndividual = {
    val name = individualResponse.fullName
    val dob =
      individualResponse.dateOfBirth.flatMap(toLocalDate).orElse(dateOfBirth)
    dob.fold(ifEmpty =
      throw new IllegalArgumentException(
        "Date of Birth is neither provided in registration response nor captured in the application page"
      )
    )(
      dateOfBirth =>
        RegistrationDetails
          .individual(sapNumber, safeId, name, address, dateOfBirth, customsId)
    )
  }

  private def convertOrganisationMatchingResponse(
    organisationResponse: OrganisationResponse,
    customsId: Option[CustomsId],
    sapNumber: String,
    safeId: SafeId,
    address: Address,
    dateOfEstablishment: Option[LocalDate]
  ): RegistrationDetailsOrganisation = {
    val name = organisationResponse.organisationName
    val etmpOrganisationType =
      organisationResponse.organisationType.map(EtmpOrganisationType.apply)
    RegistrationDetails.organisation(
      sapNumber,
      safeId,
      name,
      address,
      customsId,
      dateOfEstablishment,
      etmpOrganisationType
    )
  }

  def registrationDetails(
    response: RegisterWithoutIDResponse,
    orgName: String,
    orgAddress: SixLineAddressMatchModel
  ): RegistrationDetailsOrganisation = {
    val sapNumber = extractSapNumber(response.responseCommon.returnParameters)
    val address = Address(
      orgAddress.lineOne,
      orgAddress.lineTwo,
      Some(orgAddress.lineThree),
      orgAddress.lineFour,
      orgAddress.postcode,
      orgAddress.country
    )

    RegistrationDetails.organisation(
      sapNumber,
      SafeId(
        response.responseDetail
          .getOrElse(throw new IllegalStateException("No responseDetail"))
          .SAFEID
      ),
      orgName,
      address,
      customsId = None,
      dateEstablished = None,
      etmpOrganisationType = None
    )
  }

  def registrationAddress(orgAddress: SixLineAddressMatchModel): Address =
    Address(
      orgAddress.lineOne,
      orgAddress.lineTwo,
      Some(orgAddress.lineThree),
      orgAddress.lineFour,
      orgAddress.postcode,
      orgAddress.country
    )

  def registrationAddressFromAddressViewModel(addressViewModel: AddressViewModel): Address =
    Address(
      addressViewModel.street,
      None,
      Some(addressViewModel.city),
      None,
      addressViewModel.postcode,
      addressViewModel.countryCode
    )

  def registrationDetails(
    response: RegisterWithoutIDResponse,
    ind: IndividualNameAndDateOfBirth,
    add: SixLineAddressMatchModel
  ): RegistrationDetailsIndividual = {
    val sapNumber = extractSapNumber(response.responseCommon.returnParameters)
    val address   = Address(add.lineOne, add.lineTwo, Some(add.lineThree), add.lineFour, add.postcode, add.country)
    val name      = ind.fullName

    RegistrationDetails.individual(
      sapNumber,
      SafeId(
        response.responseDetail
          .getOrElse(throw new IllegalStateException("No responseDetail"))
          .SAFEID
      ),
      name,
      address,
      ind.dateOfBirth,
      customsId = None
    )
  }

  def registrationDetails(customsId: Option[CustomsId] = None)(response: RegistrationInfo): RegistrationDetails = {
    val address = Address(
      response.lineOne,
      response.lineTwo,
      response.lineThree,
      response.lineFour,
      response.postcode,
      response.country
    )

    response match {
      case i: IndividualRegistrationInfo =>
        createRegistrationDetailsIndividual(i, address, customsId)
      case o: OrgRegistrationInfo =>
        createRegistrationDetailsOrganisation(o, address, customsId)
    }
  }

  private def createRegistrationDetailsIndividual(
    individualResponse: IndividualRegistrationInfo,
    address: Address,
    customsId: Option[CustomsId]
  ): RegistrationDetailsIndividual = {
    val name = List(
      Some(individualResponse.firstName),
      individualResponse.middleName,
      Some(individualResponse.lastName)
    ).flatten mkString " "
    individualResponse.dateOfBirth.fold(ifEmpty =
      throw new IllegalArgumentException("Date of Birth is not provided in registration info response")
    )(
      dateOfBirth =>
        RegistrationDetails
          .individual(individualResponse.taxPayerId.id, SafeId(""), name, address, dateOfBirth, customsId = customsId)
    )
  }

  private def createRegistrationDetailsOrganisation(
    organisationResponse: OrgRegistrationInfo,
    address: Address,
    maybeCustomsId: Option[CustomsId]
  ): RegistrationDetailsOrganisation =
    RegistrationDetails.organisation(
      organisationResponse.taxPayerId.id,
      SafeId(""),
      organisationResponse.name,
      address,
      customsId = maybeCustomsId,
      dateEstablished = None,
      organisationResponse.organisationType.map(EtmpOrganisationType.apply)
    )

  def registrationDetails(response: RegistrationDisplayResponse): RegistrationDetails = {
    val RegistrationDisplayResponse(responseCommon, Some(responseDetail)) =
      response
    (responseDetail.individual, responseDetail.organisation, responseCommon.taxPayerID) match {
      case (Some(individual), None, Some(taxPayerId)) =>
        convertIndividualMatchingResponse(
          individual,
          None,
          taxPayerId,
          SafeId(responseDetail.SAFEID),
          responseDetail.address,
          dateOfBirth = None
        )
      case (None, Some(org), Some(taxPayerId)) =>
        convertOrganisationMatchingResponse(
          org,
          None,
          taxPayerId,
          SafeId(responseDetail.SAFEID),
          responseDetail.address,
          dateOfEstablishment = None
        )
      case _ =>
        throw new IllegalStateException("Unexpected Response or Missing Key Information")
    }
  }

}
