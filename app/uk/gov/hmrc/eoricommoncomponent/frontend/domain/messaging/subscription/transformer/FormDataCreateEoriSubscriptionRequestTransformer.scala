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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.transformer

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{CharityPublicBodyNotForProfit, Company, Embassy, Individual, LimitedLiabilityPartnership, Partnership, SoleTrader}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionRequest.principalEconomicActivityLength
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionRequest.{CdsEstablishmentAddress, ContactInformation, Id, Organisation, VatIdentification}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, RegistrationDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.EtmpLegalStatus.{CorporateBody, Llp, UnincorporatedBody}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.{CdsToEtmpOrganisationType, EtmpLegalStatus}

import java.time.format.DateTimeFormatter
import javax.inject.Singleton

@Singleton
class FormDataCreateEoriSubscriptionRequestTransformer() {

  def transform(
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    userLocation: UserLocation,
    service: Service
  ): CreateEoriSubscriptionRequest = {

    if (subDetails.formData.organisationType.contains(Embassy)) {
      transformEmbassy(
        regDetails: RegistrationDetails,
        subDetails: SubscriptionDetails,
        userLocation: UserLocation,
        service: Service
      )
    } else if (userLocation == UserLocation.Iom) {
      subDetails.formData.organisationType.head match {
        case Company | LimitedLiabilityPartnership | Partnership =>
          transformCompanyPartnershipLLP(regDetails, subDetails, userLocation, service)
        case SoleTrader | Individual => transformSoleTraderIndividual(regDetails, subDetails, userLocation, service)
        case CharityPublicBodyNotForProfit =>
          transformCharityPublicBodyNotForProfit(regDetails, subDetails, userLocation, service)
      }
    } else if (userLocation == UserLocation.Uk && subDetails.formData.organisationType.contains(CharityPublicBodyNotForProfit)) {
      transformCharityPublicBodyNotForProfit(regDetails, subDetails, userLocation, service)
    } else {
      throw new UnsupportedOperationException(
        s"Unable to create EORI for organisation type: ${subDetails.formData.organisationType}"
      )
    }
  }

  private def transformEmbassy(
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    userLocation: UserLocation,
    service: Service
  ): CreateEoriSubscriptionRequest = {
    CreateEoriSubscriptionRequest(
      edgeCaseType(subDetails.formData.organisationType.head, userLocation),
      regDetails.name,
      Some(Organisation(None, subDetails.embassyName.head)),
      None,
      CdsEstablishmentAddress(
        regDetails.address.addressLine3,
        regDetails.address.countryCode,
        regDetails.address.postalCode,
        s"${regDetails.address.addressLine1},${regDetails.address.addressLine2.map(l2 => s" $l2").getOrElse("")}"
      ),
      EtmpLegalStatus.Embassy,
      !subDetails.contactDetails.map(_.useAddressFromRegistrationDetails).head,
      subDetails.personalDataDisclosureConsent,
      contactInformation(subDetails.contactDetails),
      None,
      None,
      None,
      None,
      Some(service.enrolmentKey),
      None,
      None,
      None,
      None
    )
  }

  private def transformCompanyPartnershipLLP(
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    userLocation: UserLocation,
    service: Service
  ): CreateEoriSubscriptionRequest = {
    val cdsOrgType = subDetails.formData.organisationType.head
    val org = CdsToEtmpOrganisationType(Some(cdsOrgType)).orElse(CdsToEtmpOrganisationType(regDetails))

    CreateEoriSubscriptionRequest(
      edgeCaseType(cdsOrgType, userLocation),
      subDetails.nameOrganisationDetails.map(_.name).getOrElse(regDetails.name),
      Some(
        Organisation(
          subDetails.dateEstablished.map(_.format(DateTimeFormatter.ISO_DATE)),
          subDetails.nameOrganisationDetails.head.name
        )
      ),
      None,
      CdsEstablishmentAddress(
        regDetails.address.addressLine3,
        regDetails.address.countryCode,
        regDetails.address.postalCode,
        s"${regDetails.address.addressLine1},${regDetails.address.addressLine2.map(l2 => s" $l2").getOrElse("")}"
      ),
      legalStatusFromOrgType(cdsOrgType),
      !subDetails.contactDetails.map(_.useAddressFromRegistrationDetails).head,
      subDetails.personalDataDisclosureConsent,
      contactInformation(subDetails.contactDetails),
      None,
      None,
      None,
      subDetails.sicCode.map(_.take(principalEconomicActivityLength)),
      Some(service.enrolmentKey),
      None,
      None,
      org.map(_.typeOfPerson),
      subDetails.ukVatDetails.map(vd => List(VatIdentification("IM", vd.number)))
    )
  }

  private def transformSoleTraderIndividual(
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    userLocation: UserLocation,
    service: Service
  ): CreateEoriSubscriptionRequest = {
    val cdsOrgType = subDetails.formData.organisationType.head
    val org = CdsToEtmpOrganisationType(Some(cdsOrgType)).orElse(CdsToEtmpOrganisationType(regDetails))

    CreateEoriSubscriptionRequest(
      edgeCaseType(cdsOrgType, userLocation),
      subDetails.nameDobDetails.map(details => s"${details.firstName} ${details.lastName}").getOrElse(regDetails.name),
      None,
      Some(
        CreateEoriSubscriptionRequest.Individual(
          subDetails.nameDobDetails.head.dateOfBirth.format(DateTimeFormatter.ISO_LOCAL_DATE),
          subDetails.nameDobDetails.head.firstName,
          subDetails.nameDobDetails.head.lastName
        )
      ),
      CdsEstablishmentAddress(
        regDetails.address.addressLine3,
        regDetails.address.countryCode,
        regDetails.address.postalCode,
        s"${regDetails.address.addressLine1},${regDetails.address.addressLine2.map(l2 => s" $l2").getOrElse("")}"
      ),
      legalStatusFromOrgType(cdsOrgType),
      !subDetails.contactDetails.map(_.useAddressFromRegistrationDetails).head,
      subDetails.personalDataDisclosureConsent,
      contactInformation(subDetails.contactDetails),
      None,
      None,
      None,
      subDetails.sicCode.map(_.take(principalEconomicActivityLength)),
      Some(service.enrolmentKey),
      None,
      None,
      org.map(_.typeOfPerson),
      subDetails.ukVatDetails.map(vd => List(VatIdentification("IM", vd.number)))
    )
  }

  private def transformCharityPublicBodyNotForProfit(
    regDetails: RegistrationDetails,
    subDetails: SubscriptionDetails,
    userLocation: UserLocation,
    service: Service
  ): CreateEoriSubscriptionRequest = {
    val cdsOrgType = subDetails.formData.organisationType.head
    val org = CdsToEtmpOrganisationType(Some(cdsOrgType)).orElse(CdsToEtmpOrganisationType(regDetails))
    val countryCode = userLocation match {
      case UserLocation.Uk => "GB"
      case UserLocation.Iom => "IM"
    }
    CreateEoriSubscriptionRequest(
      edgeCaseType(cdsOrgType, userLocation),
      subDetails.nameOrganisationDetails.map(_.name).getOrElse(regDetails.name),
      Some(Organisation(None, subDetails.nameOrganisationDetails.head.name)),
      None,
      CdsEstablishmentAddress(
        regDetails.address.addressLine3,
        regDetails.address.countryCode,
        regDetails.address.postalCode,
        s"${regDetails.address.addressLine1},${regDetails.address.addressLine2.map(l2 => s" $l2").getOrElse("")}"
      ),
      legalStatusFromOrgType(cdsOrgType),
      !subDetails.contactDetails.map(_.useAddressFromRegistrationDetails).head,
      subDetails.personalDataDisclosureConsent,
      contactInformation(subDetails.contactDetails),
      None,
      regDetails.customsId.map(cid => Id("UTR", cid.id)),
      None,
      subDetails.sicCode.map(_.take(principalEconomicActivityLength)),
      Some(service.enrolmentKey),
      None,
      None,
      org.map(_.typeOfPerson),
      subDetails.ukVatDetails.map(vd => List(VatIdentification(countryCode, vd.number)))
    )
  }

  private def legalStatusFromOrgType(cdsOrganisationType: CdsOrganisationType): String = {
    cdsOrganisationType match {
      case Company => CorporateBody
      case LimitedLiabilityPartnership => Llp
      case SoleTrader | Individual => UnincorporatedBody
      case Partnership => EtmpLegalStatus.Partnership
      case CharityPublicBodyNotForProfit => UnincorporatedBody
    }
  }

  private def edgeCaseType(cdsOrgType: CdsOrganisationType, userLocation: UserLocation): String = {
    if (userLocation == UserLocation.Iom) {
      "02"
    } else if (cdsOrgType == Embassy) {
      "01"
    } else if (cdsOrgType == CharityPublicBodyNotForProfit) {
      "03"
    } else {
      throw new IllegalArgumentException(s"Unknown CDS Organisation Type provided: $cdsOrgType")
    }
  }

  private def contactInformation(subDetailsContactDetails: Option[ContactDetailsModel]): Option[ContactInformation] = {
    if (subDetailsContactDetails.map(_.useAddressFromRegistrationDetails).head) {
      None
    } else {
      Some(
        ContactInformation(
          subDetailsContactDetails.map(_.fullName).head,
          subDetailsContactDetails.flatMap(_.street).getOrElse(""),
          subDetailsContactDetails.flatMap(_.city).getOrElse(""),
          subDetailsContactDetails.flatMap(_.countryCode).getOrElse(""),
          isAgent = true,
          isGroup = false,
          subDetailsContactDetails.map(_.emailAddress),
          None,
          None,
          subDetailsContactDetails.flatMap(_.postcode),
          subDetailsContactDetails.map(_.telephone)
        )
      )
    }

  }

}
