/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.RegisterWithoutId
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterWithoutIdConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditor)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url = url"${appConfig.getServiceUrl("register-without-id")}"

  def register(request: RegisterWithoutIDRequest)(implicit hc: HeaderCarrier): Future[RegisterWithoutIDResponse] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"Register: $url, body: $request and hc: $hc")
    // $COVERAGE-ON

    httpClient
      .post(url)
      .withBody(Json.toJson(RegisterWithoutIdRequestHolder(request)))
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[RegisterWithoutIdResponseHolder] map { response =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"Register: responseCommon: ${response.registerWithoutIDResponse.responseCommon}")
      // $COVERAGE-ON

      audit.sendRegistrationDataEvent(url.toString, Json.toJson(RegisterWithoutId(request, response)))
      response.registerWithoutIDResponse
    } recover { case e: Throwable =>
      // $COVERAGE-OFF$Loggers
      logger.warn(
        s"Failure. postUrl: $url, acknowledgement ref: ${request.requestCommon.acknowledgementReference}, error: $e"
      )
      // $COVERAGE-ON
      throw e
    }
  }
}
