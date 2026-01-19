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
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import play.mvc.Http.MimeTypes
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.HandleSubscriptionRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class HandleSubscriptionConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url = url"${appConfig.handleSubscriptionBaseUrl}/${appConfig.handleSubscriptionServiceContext}"

  def call(request: HandleSubscriptionRequest)(implicit hc: HeaderCarrier): Future[Unit] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"Call: $url, eori: ${request.eori}, and hc: $hc")
    // $COVERAGE-ON

    httpClient
      .post(url)
      .withBody(Json.toJson(request))
      .setHeader(ACCEPT -> "application/vnd.hmrc.1.0+json")
      .setHeader(CONTENT_TYPE -> MimeTypes.JSON)
      .execute map { response =>
      response.status match {
        case OK | NO_CONTENT =>
          // $COVERAGE-OFF$Loggers
          logger.debug(s"Call complete for call to ${url.toString} and  hc: $hc. Status:${response.status}")
        // $COVERAGE-ON
        case _ => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      // $COVERAGE-OFF$Loggers
      case e: BadRequestException =>
        logger.warn(s"Call failed with BAD_REQUEST status for call to ${url.toString} and  hc: $hc: ${e.getMessage}", e)
        Future.failed(e)
      case NonFatal(e) =>
        logger.warn(s"Call failed for call to ${url.toString}: ${e.getMessage}", e)
        Future.failed(e)
      // $COVERAGE-ON
    }
  }

}
