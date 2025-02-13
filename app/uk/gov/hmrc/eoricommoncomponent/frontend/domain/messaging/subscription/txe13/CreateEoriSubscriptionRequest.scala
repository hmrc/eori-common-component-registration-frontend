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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13

import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionRequest.{
  CdsEstablishmentAddress,
  ContactInformation,
  Id,
  Individual,
  Organisation,
  ThirdCountryUniqueIdentificationNumber,
  VatIdentification
}

case class CreateEoriSubscriptionRequest(
  edgeCaseType: String,
  cdsFullName: String,
  organisation: Option[Organisation],
  individual: Option[Individual],
  cdsEstablishmentAddress: CdsEstablishmentAddress,
  legalStatus: String,
  separateCorrespondenceAddressIndicator: Boolean,
  consentToDisclosureOfPersonalData: Option[Boolean],
  contactInformation: Option[ContactInformation],
  establishmentCustomsInTheTerritory: Option[Boolean],
  id: Option[Id],
  preBrexitIndicator: Option[Boolean],
  principalEconomicActivity: Option[String],
  serviceName: Option[String],
  shortName: Option[String],
  thirdCountryUniqueIdentificationNumber: Option[ThirdCountryUniqueIdentificationNumber],
  typeOfPerson: Option[String],
  vatIdentificationNumbers: Option[List[VatIdentification]]
)

object CreateEoriSubscriptionRequest {
  case class Organisation(dateOfEstablishment: Option[String], organisationName: String)

  case class Individual(dateOfBirth: String, firstName: String, lastName: String)

  case class CdsEstablishmentAddress(
    city: Option[String],
    countryCode: String,
    postcode: Option[String],
    streetAndNumber: String
  )

  case class ContactInformation(
    personOfContact: String,
    streetAndNumber: String,
    city: String,
    countryCode: String,
    isAgent: Boolean,
    isGroup: Boolean,
    email: Option[String],
    emailVerificationTimestamp: Option[String],
    faxNumber: Option[String],
    postcode: Option[String],
    telephoneNumber: Option[String]
  )

  case class Id(idType: String, idNumber: String)

  case class ThirdCountryUniqueIdentificationNumber(
    identificationReferenceNumber: String,
    issuingCountry: Option[String],
    issuingInstitution: Option[String]
  )

  case class VatIdentification(countryCode: String, vatIdentificationNumber: String)

  implicit val individualReads: Reads[Individual]    = Json.reads[Individual]
  implicit val individualWrites: OWrites[Individual] = Json.writes[Individual]

  implicit val organisationReads: Reads[Organisation]    = Json.reads[Organisation]
  implicit val organisationWrites: OWrites[Organisation] = Json.writes[Organisation]

  implicit val cdsEstablishmentAddressReads: Reads[CdsEstablishmentAddress]    = Json.reads[CdsEstablishmentAddress]
  implicit val cdsEstablishmentAddressWrites: OWrites[CdsEstablishmentAddress] = Json.writes[CdsEstablishmentAddress]

  implicit val contactInformationReads: Reads[ContactInformation]    = Json.reads[ContactInformation]
  implicit val contactInformationWrites: OWrites[ContactInformation] = Json.writes[ContactInformation]

  implicit val idReads: Reads[Id]    = Json.reads[Id]
  implicit val idWrites: OWrites[Id] = Json.writes[Id]

  implicit val thirdCountryUniqueIdentificationNumberReads: Reads[ThirdCountryUniqueIdentificationNumber] =
    Json.reads[ThirdCountryUniqueIdentificationNumber]

  implicit val thirdCountryUniqueIdentificationNumberWrites: OWrites[ThirdCountryUniqueIdentificationNumber] =
    Json.writes[ThirdCountryUniqueIdentificationNumber]

  implicit val vatIdentificationReads: Reads[VatIdentification]    = Json.reads[VatIdentification]
  implicit val vatIdentificationWrites: OWrites[VatIdentification] = Json.writes[VatIdentification]

  implicit val createEoriSubscriptionRequestReads: Reads[CreateEoriSubscriptionRequest] =
    Json.reads[CreateEoriSubscriptionRequest]

  implicit val createEoriSubscriptionRequestWrites: OWrites[CreateEoriSubscriptionRequest] =
    Json.writes[CreateEoriSubscriptionRequest]

}
