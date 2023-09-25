/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.GetVatCustomerInformationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GetVatInformationResponse, VatControlListResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GetVatCustomerInformationService @Inject() (
  getVatCustomerInformationConnector: GetVatCustomerInformationConnector,
  sessionCache: SessionCache
)(implicit ec: ExecutionContext)
    extends Logging {

  def checkResponseMatchesNewVATAPI(
    vatControlListResponse: VatControlListResponse
  )(implicit request: Request[_], hc: HeaderCarrier): Unit =
    for {
      vatDetails <- sessionCache.subscriptionDetails.map(_.ukVatDetails)
      vrn = vatDetails.map(_.number)
    } yield vrn.map { vatNumber =>
      getVatCustomerInformationConnector.getVatCustomerInformation(vatNumber)
        .fold(
          errorResponse =>
            logger.warn(s"getVatCustomerInformation returned response: $errorResponse. Cannot compare values"),
          vatCustomerInformation => compareApiResponses(vatControlListResponse, vatCustomerInformation)
        )
    }

  def compareApiResponses(oldResponse: VatControlListResponse, newResponse: GetVatInformationResponse): Boolean = {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")

    val postCodeMatches = (for {
      oldPostcode <- oldResponse.postcode
      newPostcode <- newResponse.postCode
    } yield oldPostcode == newPostcode).getOrElse(false)

    val dateMatches = (for {
      oldDateString <- oldResponse.dateOfReg
      oldDate = format.parse(oldDateString)
      newDate <- newResponse.effectiveRegistrationDate
    } yield oldDate == newDate).getOrElse(false)

    if (postCodeMatches && dateMatches) {
      logger.info("compareApiResponses matches postcode and date")
      true
    } else {
      logger.warn(s"compareApiResponses does not match. Postcode: $postCodeMatches, Date: $dateMatches")
      false
    }
  }

}
