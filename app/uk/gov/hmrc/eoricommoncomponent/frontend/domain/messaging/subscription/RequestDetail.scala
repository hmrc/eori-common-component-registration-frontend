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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EstablishmentAddress

import java.time.LocalDate

case class RequestDetail(
  SAFE: String,
  EORINo: Option[String],
  CDSFullName: String,
  CDSEstablishmentAddress: EstablishmentAddress,
  establishmentInTheCustomsTerritoryOfTheUnion: Option[String],
  typeOfLegalEntity: Option[String],
  contactInformation: Option[ContactInformation],
  vatIDs: Option[List[VatId]],
  consentToDisclosureOfPersonalData: Option[String],
  shortName: Option[String],
  dateOfEstablishment: Option[LocalDate],
  typeOfPerson: Option[String],
  principalEconomicActivity: Option[String],
  serviceName: Option[String]
)

object RequestDetail {

  implicit val jsonFormat: OFormat[RequestDetail] = Json.format[RequestDetail]
}
