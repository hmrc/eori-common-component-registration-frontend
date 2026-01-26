/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EstablishmentAddress.createEstablishmentAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.ContactInformation.createContactInformation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionRequest.principalEconomicActivityLength
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{RequestCommon, RequestParameter}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.CdsToEtmpOrganisationType

import java.time.{Clock, LocalDate, LocalDateTime, ZoneId}
import java.util.UUID

case class SubscriptionCreateRequest(requestCommon: RequestCommon, requestDetail: RequestDetail)

object SubscriptionCreateRequest {

  implicit val jsonFormat: OFormat[SubscriptionCreateRequest] = Json.format[SubscriptionCreateRequest]

  // Registration journey
  def fromOrganisation(
    reg: RegistrationDetailsOrganisation,
    sub: SubscriptionDetails,
    cdsOrgType: Option[CdsOrganisationType],
    service: Option[Service]
  ): SubscriptionRequest = {
    toSubscriptionRequest(reg, sub, cdsOrgType, sub.dateEstablished, service)
  }

  def fromIndividual(
    reg: RegistrationDetailsIndividual,
    sub: SubscriptionDetails,
    cdsOrgType: Option[CdsOrganisationType],
    service: Option[Service]
  ): SubscriptionRequest = toSubscriptionRequest(reg, sub, cdsOrgType, Some(reg.dateOfBirth), service)

  private def toSubscriptionRequest(
    reg: RegistrationDetails,
    sub: SubscriptionDetails,
    cdsOrgType: Option[CdsOrganisationType],
    dateEstablished: Option[LocalDate],
    service: Option[Service]
  ): SubscriptionRequest = {
    val org = CdsToEtmpOrganisationType(cdsOrgType) orElse CdsToEtmpOrganisationType(reg)
    val ukVatId: Option[List[VatIdentification]] =
      sub.ukVatDetails.map(vd => List(VatIdentification(Some("GB"), Some(vd.number))))

    SubscriptionRequest(
      SubscriptionCreateRequest(
        generateWithOriginatingSystem(),
        RequestDetail(
          SAFE = reg.safeId.id,
          EORINo = None,
          CDSFullName = reg.name,
          CDSEstablishmentAddress = fourFieldAddress(sub, reg),
          establishmentInTheCustomsTerritoryOfTheUnion = None,
          typeOfLegalEntity = org.map(_.legalStatus),
          contactInformation = sub.contactDetails.map(c => createContactInformation(c.contactDetails)),
          vatIDs = createVatIds(ukVatId),
          consentToDisclosureOfPersonalData = sub.personalDataDisclosureConsent.map(bool => if (bool) "1" else "0"),
          shortName = None, // sending and capturing businessShortName is removed: https://jira.tools.tax.service.gov.uk/browse/ECC-1367
          dateOfEstablishment = dateEstablished,
          typeOfPerson = org.map(_.typeOfPerson),
          principalEconomicActivity = sub.sicCode.map(_.take(principalEconomicActivityLength)),
          serviceName = service.map(_.enrolmentKey)
        )
      )
    )
  }

  private def dashForEmpty(s: String): String =
    if (s.isEmpty) "-" else s

  private def fourFieldAddress(subscription: SubscriptionDetails, registration: RegistrationDetails) = {

    val address = subscription.addressDetails match {
      case Some(a) =>
        EstablishmentAddress(streetAndNumber = a.street, city = a.city, a.postcode.filter(_.nonEmpty), a.countryCode)
      case _ => createEstablishmentAddress(registration.address)
    }

    address.copy(city = dashForEmpty(address.city))
  }

  private def generateWithOriginatingSystem(requestParameters: Option[Seq[RequestParameter]] = None): RequestCommon =
    RequestCommon(
      regime = Service.regimeCDS,
      receiptDate = LocalDateTime.ofInstant(Clock.systemUTC().instant, ZoneId.of("Europe/London")),
      acknowledgementReference = UUID.randomUUID().toString.replace("-", ""),
      originatingSystem = Some("MDTP"),
      requestParameters = requestParameters
    )

  private def createVatIds(vis: Option[List[VatIdentification]]): Option[List[VatId]] = {
    def removeEmpty: List[VatIdentification] => List[VatId] = _.flatMap {
      case VatIdentification(None, None) => None
      case VatIdentification(cc, n) => Some(VatId(cc, n))
    }

    def removeEmptyList(): List[VatId] => Option[List[VatId]] = {
      case Nil => None
      case vs => Some(vs)
    }

    vis map removeEmpty flatMap removeEmptyList()
  }

}
