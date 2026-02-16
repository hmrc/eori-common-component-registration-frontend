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

import play.api.Logging
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.*
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.RegistrationDisplay
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RegistrationDisplayConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditor) extends Logging {

  protected val url = url"${appConfig.getServiceUrl("registration-display")}"

  def registrationDisplay(
    request: RegistrationDisplayRequestHolder
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[EoriHttpResponse, RegistrationDisplayResponse]] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(
      s"RegistrationDisplay: $url, requestCommon: ${request.registrationDisplayRequest.requestCommon} and hc: $hc"
    )
    // $COVERAGE-ON

    httpClient
      .post(url)
      .withBody(Json.toJson(request))
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[RegistrationDisplayResponseHolder] map { response =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"[RegistrationDisplay: response: $response")
      // $COVERAGE-ON

      val detail = Json.toJson(RegistrationDisplay(request, response))
      audit.sendRegistrationDisplayEvent(url.toString, detail)
      Right(response.registrationDisplayResponse)
    } recover { case NonFatal(e) =>
      // $COVERAGE-OFF$Loggers
      logger.warn(s"registration-display failed. url: $url, error: $e")
      // $COVERAGE-ON
      Left(ServiceUnavailableResponse)
    }
  }
}
