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
import play.api.libs.json.Json
import play.mvc.Http.MimeTypes
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.NotifyRcmRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.NotifyRcm
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class NotifyRcmConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable) {

  private val logger = Logger(this.getClass)

  def notifyRCM(request: NotifyRcmRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url     = s"${appConfig.handleSubscriptionBaseUrl}/notify/rcm"
    val headers = Seq(ACCEPT -> "application/vnd.hmrc.1.0+json", CONTENT_TYPE -> MimeTypes.JSON)
    http.POST[NotifyRcmRequest, HttpResponse](url, request, headers) map { response =>
      auditCallResponse(url, request, response)
      response.status match {
        case OK | NO_CONTENT => ()
        case _               => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case e: BadRequestException =>
        logger.warn(s"request failed with BAD_REQUEST status for call to $url: ${e.getMessage}", e)
        Future.failed(e)
      case NonFatal(e) =>
        logger.warn(s"request failed for call to $url: ${e.getMessage}", e)
        Future.failed(e)
    }
  }

  private def auditCallResponse(url: String, request: NotifyRcmRequest, response: HttpResponse)(implicit
    hc: HeaderCarrier
  ): Unit =
    Future.successful {
      audit.sendExtendedDataEvent(
        transactionName = "customs-rcm-email",
        path = url,
        details = Json.toJson(NotifyRcm(request, response)),
        eventType = "RcmEmailConfirmation"
      )
    }

}
