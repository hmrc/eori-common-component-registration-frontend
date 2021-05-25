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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  EstablishmentAddress,
  RegisterWithEoriAndIdOrganisation,
  RegisterWithEoriAndIdRequest
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Individual, RegistrationInfoRequest}

case class RegistrationSubmitted(
  eori: String,
  utr: Option[String],
  nino: Option[String],
  acknowledgementReference: String,
  address: EstablishmentAddress,
  fullName: String,
  isNameMatched: Boolean,
  individual: Option[Individual],
  organisation: Option[RegisterWithEoriAndIdOrganisation],
  receiptDate: String,
  regime: String
)

object RegistrationSubmitted {
  implicit val format = Json.format[RegistrationSubmitted]

  def apply(request: RegisterWithEoriAndIdRequest): RegistrationSubmitted = {

    val idType = request.requestDetail.registerModeID.IDType

    RegistrationSubmitted(
      eori = request.requestDetail.registerModeEORI.EORI,
      utr = if (idType == RegistrationInfoRequest.UTR) Some(request.requestDetail.registerModeID.IDNumber) else None,
      nino = if (idType == RegistrationInfoRequest.NINO) Some(request.requestDetail.registerModeID.IDNumber) else None,
      acknowledgementReference = request.requestCommon.acknowledgementReference,
      address = request.requestDetail.registerModeEORI.address,
      fullName = request.requestDetail.registerModeEORI.fullName,
      isNameMatched = request.requestDetail.registerModeID.isNameMatched,
      individual = request.requestDetail.registerModeID.individual,
      organisation = request.requestDetail.registerModeID.organisation,
      receiptDate = request.requestCommon.receiptDate.toString(),
      regime = request.requestCommon.regime
    )
  }

}
