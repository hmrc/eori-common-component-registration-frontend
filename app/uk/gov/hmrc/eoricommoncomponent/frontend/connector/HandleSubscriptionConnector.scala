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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.HeaderNames._
import play.mvc.Http.MimeTypes
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.HandleSubscriptionRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class HandleSubscriptionConnector @Inject() (http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)
  private val url    = s"${appConfig.handleSubscriptionBaseUrl}/${appConfig.handleSubscriptionServiceContext}"

  def call(request: HandleSubscriptionRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val headers = Seq(ACCEPT -> "application/vnd.hmrc.1.0+json", CONTENT_TYPE -> MimeTypes.JSON)

    // $COVERAGE-OFF$Loggers
    logger.debug(s"Call: $url, eori: ${request.eori}, headers: $headers and hc: $hc")
    // $COVERAGE-ON

    http.POST[HandleSubscriptionRequest, HttpResponse](url, request, headers) map { response =>
      response.status match {
        case OK | NO_CONTENT =>
          logger.debug(s"Call complete for call to $url and  hc: $hc. Status:${response.status}")
          ()
        case _ => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case e: BadRequestException =>
        logger.warn(s"Call failed with BAD_REQUEST status for call to $url and  hc: $hc: ${e.getMessage}", e)
        Future.failed(e)
      case NonFatal(e) =>
        logger.warn(s"Call failed for call to $url: ${e.getMessage}", e)
        Future.failed(e)
    }
  }

}
