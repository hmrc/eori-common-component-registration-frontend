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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.events

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EstablishmentAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  ContactInformation,
  SubscriptionRequest,
  VatId
}

case class SubscriptionSubmitted(
  eori: Option[String],
  safeId: String,
  cdsFullName: String,
  cdsEstablishmentAddress: EstablishmentAddress,
  establishmentInTheCustomsTerritoryOfTheUnion: Option[String],
  typeOfLegalEntity: Option[String],
  contactInformation: Option[ContactInformation],
  vatIDs: Option[List[VatId]],
  consentToDisclosureOfPersonalData: Option[String],
  shortName: Option[String],
  dateOfEstablishment: Option[String],
  typeOfPerson: Option[String],
  principalEconomicActivity: Option[String],
  serviceName: Option[String],
  acknowledgementReferece: String,
  receiptDate: String,
  regime: String
)

object SubscriptionSubmitted {
  implicit val format = Json.format[SubscriptionSubmitted]

  def apply(request: SubscriptionRequest): SubscriptionSubmitted = {

    val requestCommon = request.subscriptionCreateRequest.requestCommon
    val requestDetail = request.subscriptionCreateRequest.requestDetail

    SubscriptionSubmitted(
      eori = requestDetail.EORINo,
      safeId = requestDetail.SAFE,
      cdsFullName = requestDetail.CDSFullName,
      cdsEstablishmentAddress = requestDetail.CDSEstablishmentAddress,
      establishmentInTheCustomsTerritoryOfTheUnion = requestDetail.establishmentInTheCustomsTerritoryOfTheUnion,
      typeOfLegalEntity = requestDetail.typeOfLegalEntity,
      contactInformation = requestDetail.contactInformation,
      vatIDs = requestDetail.vatIDs,
      consentToDisclosureOfPersonalData = requestDetail.consentToDisclosureOfPersonalData,
      shortName = requestDetail.shortName,
      dateOfEstablishment = requestDetail.dateOfEstablishment.map(_.toString),
      typeOfPerson = requestDetail.typeOfPerson,
      principalEconomicActivity = requestDetail.principalEconomicActivity,
      serviceName = requestDetail.serviceName,
      acknowledgementReferece = requestCommon.acknowledgementReference,
      receiptDate = requestCommon.receiptDate.toString,
      regime = requestCommon.regime
    )
  }

}
