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

import play.api.Logger
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatControlListRequest, VatControlListResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.{URL, URLEncoder}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatControlListConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  private val logger  = Logger(this.getClass)
  private val baseUrl = appConfig.getServiceUrl("vat-known-facts-control-list")

  def vatControlList(
    request: VatControlListRequest
  )(implicit hc: HeaderCarrier): Future[Either[EoriHttpResponse, VatControlListResponse]] = {

    val url = new URL(s"$baseUrl?${makeQueryString(request.queryParams)}")

    httpClient
      .get(url)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute map { response =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"vat-known-facts-control-list successful. url: $url")
      // $COVERAGE-ON
      response.status match {
        case OK        => Right(response.json.as[VatControlListResponse])
        case NOT_FOUND =>
          // $COVERAGE-OFF$Loggers
          logger.warn(
            s"VatControlList failed. url: $url. Reason: The back end has indicated that vat known facts cannot be returned."
          )
          // $COVERAGE-ON
          Left(NotFoundResponse)
        case BAD_REQUEST =>
          // $COVERAGE-OFF$Loggers
          logger.warn(s"VatControlList failed. url: $url. Reason: Request has not passed validation. Invalid vrn.")
          // $COVERAGE-ON
          Left(InvalidResponse)
        case SERVICE_UNAVAILABLE =>
          // $COVERAGE-OFF$Loggers
          logger.warn(s"VatControlList failed. url: $url. Reason: Dependent systems are currently not responding")
          // $COVERAGE-ON
          Left(ServiceUnavailableResponse)
        case _ =>
          val error = "Incorrect VAT Known facts response"
          // $COVERAGE-OFF$Loggers
          logger.warn(error)
          // $COVERAGE-ON
          throw new Exception(error)
      }
    }
  }

  private def makeQueryString(queryParams: Seq[(String, String)]) = {
    val paramPairs = queryParams.map { case (k, v) => s"$k=${URLEncoder.encode(v, "utf-8")}" }
    if (paramPairs.isEmpty) "" else paramPairs.mkString("&")
  }

}
