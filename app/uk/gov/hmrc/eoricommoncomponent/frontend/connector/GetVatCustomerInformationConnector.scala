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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import cats.data.EitherT
import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.GetVatInformationResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetVatCustomerInformationConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  def getVatCustomerInformation(
    vrn: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Int, GetVatInformationResponse] = EitherT {
    val url = url"${appConfig.handleSubscriptionBaseUrl}/vat-customer-information/$vrn"

    httpClient
      .get(url)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute map { response =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"vat-customer-information success. response status: ${response.status}")
      // $COVERAGE-ON
      response.status match {
        case OK =>
          Right(response.json.as[GetVatInformationResponse])
        case NOT_FOUND =>
          logger.warn(s"getVatCustomerInformation not found. Body: ${response.body}")
          Left(response.status)
        case BAD_REQUEST =>
          logger.warn(s"getVatCustomerInformation bad request. Body: ${response.body}")
          Left(response.status)
        case FORBIDDEN =>
          logger.warn(s"getVatCustomerInformation forbidden. Body: ${response.body}")
          Left(response.status)
        case INTERNAL_SERVER_ERROR =>
          logger.warn(s"getVatCustomerInformation internal server error. Body: ${response.body}")
          Left(response.status)
        case BAD_GATEWAY =>
          logger.warn(s"getVatCustomerInformation bad gateway. Body: ${response.body}")
          Left(response.status)
        case SERVICE_UNAVAILABLE =>
          logger.warn(s"getVatCustomerInformation service unavailable. Body: ${response.body}")
          Left(response.status)
        case _ =>
          logger.warn(s"getVatCustomerInformation unexpected status. Status: ${response.status} Body: ${response.body}")
          Left(response.status)
      }
    }
  }

}
