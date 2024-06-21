/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  GetVatCustomerInformationConnector,
  ResponseError,
  VatControlListConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatControlListRequest, VatControlListResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class VatDetailsService @Inject() (
  appConfig: AppConfig,
  vatCustomerInfoConnector: GetVatCustomerInformationConnector,
  vatControlListConnector: VatControlListConnector
) {

  def getVatCustomerInformation(
    number: String
  )(implicit hc: HeaderCarrier): EitherT[Future, ResponseError, VatControlListResponse] =
    if (appConfig.vatDetailsFeatureFlag)
      vatCustomerInfoConnector.getVatCustomerInformation(number.filterNot(_.isWhitespace))
    else
      vatControlListConnector.vatControlList(VatControlListRequest(number))

}
