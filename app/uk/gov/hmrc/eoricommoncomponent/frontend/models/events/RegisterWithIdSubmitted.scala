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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{MatchingRequestHolder, Organisation}

case class RegisterWithIdSubmitted(
  eori: Option[String],
  utr: Option[String],
  nino: Option[String],
  isAnAgent: Boolean,
  requiresNameMatch: Boolean,
  individual: Option[Individual],
  organisation: Option[Organisation] = None,
  acknowledgementReference: String,
  receiptDate: String,
  regime: String
)

object RegisterWithIdSubmitted {
  implicit val format = Json.format[RegisterWithIdSubmitted]

  def apply(request: MatchingRequestHolder): RegisterWithIdSubmitted = {

    val requestCommon = request.registerWithIDRequest.requestCommon
    val requestDetail = request.registerWithIDRequest.requestDetail

    RegisterWithIdSubmitted(
      eori = if (requestDetail.IDType == "EORI") Some(requestDetail.IDNumber) else None,
      utr = if (requestDetail.IDType == "UTR") Some(requestDetail.IDNumber) else None,
      nino = if (requestDetail.IDType == "NINO") Some(requestDetail.IDNumber) else None,
      isAnAgent = requestDetail.isAnAgent,
      requiresNameMatch = requestDetail.requiresNameMatch,
      individual = requestDetail.individual,
      organisation = requestDetail.organisation,
      acknowledgementReference = requestCommon.acknowledgementReference,
      receiptDate = requestCommon.receiptDate.toString,
      regime = requestCommon.regime
    )
  }

}
