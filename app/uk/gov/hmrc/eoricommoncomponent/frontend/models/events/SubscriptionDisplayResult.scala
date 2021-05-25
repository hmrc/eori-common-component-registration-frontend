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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.MessagingServiceParam
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  ContactInformation,
  SubscriptionDisplayResponseHolder
}

case class SubscriptionInfoVatId(countryCode: Option[String], vatId: Option[String])

object SubscriptionInfoVatId {
  implicit val format = Json.format[SubscriptionInfoVatId]

  def from(
    vatId: uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionInfoVatId
  ): SubscriptionInfoVatId =
    SubscriptionInfoVatId(vatId.countryCode, vatId.VATID)

}

case class SubscriptionDisplayResult(
  status: String,
  eori: Option[String],
  cdsFullName: String,
  cdsEstablishmentAddress: EstablishmentAddress,
  typeOfLegalEntity: Option[String],
  contactInformation: Option[ContactInformation],
  vatIds: Option[List[SubscriptionInfoVatId]],
  thirdCountryUniqueIdentificationNumber: Option[List[String]],
  consentToDisclosureOfPersonalData: Option[String],
  shortName: Option[String],
  dateOfEstablishment: Option[String],
  typeOfPerson: Option[String],
  principalEconomicActivity: Option[String],
  processingDate: String,
  returnParameters: Option[List[MessagingServiceParam]]
)

object SubscriptionDisplayResult {
  implicit val format = Json.format[SubscriptionDisplayResult]

  def apply(response: SubscriptionDisplayResponseHolder): SubscriptionDisplayResult = {
    val responseCommon = response.subscriptionDisplayResponse.responseCommon
    val responseDetail = response.subscriptionDisplayResponse.responseDetail

    val subscriptionInfoVatIds = responseDetail.VATIDs.map { vatIds =>
      vatIds.map { id =>
        SubscriptionInfoVatId.from(id)
      }
    }

    SubscriptionDisplayResult(
      status = responseCommon.status,
      eori = responseDetail.EORINo,
      cdsFullName = responseDetail.CDSFullName,
      cdsEstablishmentAddress = responseDetail.CDSEstablishmentAddress,
      typeOfLegalEntity = responseDetail.typeOfLegalEntity,
      contactInformation = responseDetail.contactInformation,
      vatIds = subscriptionInfoVatIds,
      thirdCountryUniqueIdentificationNumber = responseDetail.thirdCountryUniqueIdentificationNumber,
      consentToDisclosureOfPersonalData = responseDetail.consentToDisclosureOfPersonalData,
      shortName = responseDetail.shortName,
      dateOfEstablishment = responseDetail.dateOfEstablishment.map(_.toString),
      typeOfPerson = responseDetail.typeOfPerson,
      principalEconomicActivity = responseDetail.principalEconomicActivity,
      processingDate = responseCommon.processingDate.toString,
      returnParameters = responseCommon.returnParameters
    )
  }

}
