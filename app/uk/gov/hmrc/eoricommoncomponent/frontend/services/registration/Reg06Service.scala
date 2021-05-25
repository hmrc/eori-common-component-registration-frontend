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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.registration

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.RegisterWithEoriAndIdConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RegistrationInfoRequest.{NINO, UTR}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RequestCommonGenerator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{
  CdsToEtmpOrganisationType,
  OrganisationTypeConfiguration
}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Reg06Service @Inject() (
  connector: RegisterWithEoriAndIdConnector,
  reqCommonGenerator: RequestCommonGenerator,
  dataCache: SessionCache,
  requestSessionData: RequestSessionData
)(implicit ec: ExecutionContext) {

  def sendIndividualRequest(implicit request: Request[AnyContent], headerCarrier: HeaderCarrier): Future[Boolean] = {
    def ninoOrUtr(id: CustomsId): String = id match {
      case _: Nino => NINO
      case _: Utr  => UTR
      case unexpected =>
        throw new IllegalArgumentException("Expected only nino or utr to be populated but got: " + unexpected)
    }

    def createRegDetail(address: AddressViewModel, nameDob: NameDobMatchModel, eori: String, id: CustomsId) = {
      val addressStreet = address.street.take(70)
      val registerModeEori = RegisterModeEori(
        eori,
        nameDob.firstName + " " + nameDob.lastName,
        EstablishmentAddress(addressStreet, address.city, address.postcode, address.countryCode)
      )
      val registerModeId = RegisterModeId(
        ninoOrUtr(id),
        id.id,
        isNameMatched = true,
        Some(Individual(nameDob.firstName, None, nameDob.lastName, nameDob.dateOfBirth.toString)),
        None
      )
      RegisterWithEoriAndIdDetail(registerModeEori, registerModeId, None)
    }

    dataCache.subscriptionDetails.flatMap { subscription =>
      val organisationType = requestSessionData.userSelectedOrganisationType.getOrElse(
        throw new IllegalStateException("Org type missing from cache")
      )
      val maybeOrganisationTypeConfiguration: Option[OrganisationTypeConfiguration] =
        CdsToEtmpOrganisationType(Some(organisationType))
      val address =
        subscription.addressDetails.getOrElse(throw new IllegalStateException("Address missing from subscription"))
      val nameDob =
        subscription.nameDobDetails.getOrElse(throw new IllegalStateException("Name / DOB missing from subscription"))
      val eori =
        subscription.eoriNumber.getOrElse(throw new IllegalStateException("EORI number missing from subscription"))
      val id = subscription.customsId.getOrElse(throw new IllegalStateException("Customs ID missing from subscription"))
      registerWithEoriAndId(
        createRegDetail(address, nameDob, eori, id),
        subscription,
        maybeOrganisationTypeConfiguration
      )
    }
  }

  def sendOrganisationRequest(implicit request: Request[AnyContent], headerCarrier: HeaderCarrier): Future[Boolean] = {
    def regModeId(idType: String, id: String, organisationType: CdsOrganisationType, orgName: String) =
      RegisterModeId(
        idType,
        id,
        isNameMatched = true,
        None,
        Some(RegisterWithEoriAndIdOrganisation(orgName, EtmpOrganisationType(organisationType).etmpOrgTypeCode))
      )

    def regModeEORI(address: AddressViewModel, eori: String, orgName: String) = {
      val addressStreet = address.street.take(70)

      RegisterModeEori(
        eori,
        orgName,
        EstablishmentAddress(addressStreet, address.city, address.postcode.filter(_.nonEmpty), address.countryCode)
      )
    }

    for {
      subscriptionDetails <- dataCache.subscriptionDetails
      organisationType = requestSessionData.userSelectedOrganisationType.getOrElse(
        throw new IllegalStateException("Org type missing from cache")
      )
      maybeOrganisationTypeConfiguration: Option[OrganisationTypeConfiguration] = CdsToEtmpOrganisationType(
        Some(organisationType)
      )
      address = subscriptionDetails.addressDetails.getOrElse(
        throw new IllegalStateException("Address missing from subscription")
      )
      eori = subscriptionDetails.eoriNumber.getOrElse(
        throw new IllegalStateException("EORI number missing from subscription")
      )
      orgDetails = subscriptionDetails.nameIdOrganisationDetails.getOrElse(
        throw new IllegalStateException("Organisation details missing from subscription")
      )
      regEoriAndId = RegisterWithEoriAndIdDetail(
        regModeEORI(address, eori, orgDetails.name),
        regModeId(UTR, orgDetails.id, organisationType, orgDetails.name),
        None
      )
      result <- registerWithEoriAndId(regEoriAndId, subscriptionDetails, maybeOrganisationTypeConfiguration)
    } yield result
  }

  def registerWithEoriAndId(
    value: RegisterWithEoriAndIdDetail,
    subscriptionDetails: SubscriptionDetails,
    maybeOrganisationTypeConfiguration: Option[OrganisationTypeConfiguration]
  )(implicit hc: HeaderCarrier): Future[Boolean] = {

    def stripKFromUtr: RegisterWithEoriAndIdDetail => RegisterWithEoriAndIdDetail = {
      case r @ RegisterWithEoriAndIdDetail(_, id, _) if id.IDType == UTR =>
        r.copy(registerModeID = id.copy(IDNumber = id.IDNumber.stripSuffix("K").stripSuffix("k")))
      case nonUtr => nonUtr
    }

    def save(details: RegisterWithEoriAndIdResponse, subscriptionDetails: SubscriptionDetails)(implicit
      hc: HeaderCarrier
    ): Future[Boolean] =
      if (details.isResponseData)
        (details.isDoE, details.isPersonType) match {
          case (true, true) => dataCache.saveRegisterWithEoriAndIdResponse(details)
          case (false, true) =>
            val date = subscriptionDetails.dateEstablished.map(_.toString()) orElse subscriptionDetails.nameDobDetails
              .map(_.dateOfBirth.toString())
            dataCache.saveRegisterWithEoriAndIdResponse(
              details
                .withDateOfEstablishment(date)
                .getOrElse(throw new IllegalStateException("DOE is missing from REG06 response"))
            )
          case (true, false) =>
            dataCache.saveRegisterWithEoriAndIdResponse(
              details
                .withPersonType(maybeOrganisationTypeConfiguration.map(_.typeOfPerson))
                .getOrElse(throw new IllegalStateException("TypeOfPerson is missing from REG06 response"))
            )
          case (false, false) =>
            val date = subscriptionDetails.dateEstablished.map(_.toString()) orElse subscriptionDetails.nameDobDetails
              .map(_.dateOfBirth.toString())
            val detailsWithDOE: RegisterWithEoriAndIdResponse = details
              .withDateOfEstablishment(date)
              .getOrElse(throw new IllegalStateException("DOE is missing from REG06 response"))
            val detailsWithTypeOfPerson = detailsWithDOE
              .withPersonType(maybeOrganisationTypeConfiguration.map(_.typeOfPerson))
              .getOrElse(throw new IllegalStateException("TypeOfPerson is missing from REG06 response"))
            dataCache.saveRegisterWithEoriAndIdResponse(detailsWithTypeOfPerson)
        }
      else
        dataCache.saveRegisterWithEoriAndIdResponse(details)

    for {
      response <- connector.register(RegisterWithEoriAndIdRequest(reqCommonGenerator.generate(), stripKFromUtr(value)))
      saved    <- save(response, subscriptionDetails)
    } yield saved
  }

}
