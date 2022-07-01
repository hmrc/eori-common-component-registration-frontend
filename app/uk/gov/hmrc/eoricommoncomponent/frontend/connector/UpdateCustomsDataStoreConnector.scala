/*
 * Copyright 2022 HM Revenue & Customs
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
import play.mvc.Http.MimeTypes
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.CustomsDataStoreRequest
import uk.gov.hmrc.http.HeaderNames.explicitlyIncludedHeaders
import uk.gov.hmrc.http.{HttpClient, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

@Singleton
class UpdateCustomsDataStoreConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable) {

  val LoggerComponentId = "UpdateCustomsDataStoreConnector"
  private val logger    = Logger(this.getClass)

  def updateCustomsDataStore(request: CustomsDataStoreRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"${appConfig.handleSubscriptionBaseUrl}/customs/update/datastore"
    logger.info(s"[$LoggerComponentId][call] postUrl: $url")
    val headers           = Seq(ACCEPT -> "application/vnd.hmrc.1.0+json", CONTENT_TYPE -> MimeTypes.JSON)
    val headersForLogging = hc.headers(explicitlyIncludedHeaders) ++ hc.extraHeaders ++ headers
    auditCallRequest(url, request)
    http.POST[CustomsDataStoreRequest, HttpResponse](url, request, headers) map { response =>
      auditCallResponse(url, response)
      response.status match {
        case OK | NO_CONTENT =>
          logger.info(
            s"[$LoggerComponentId][call] complete for call to $url and headers $headersForLogging. Status:${response.status}"
          )
          ()
        case _ => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case e: BadRequestException =>
        logger.error(
          s"[$LoggerComponentId][call] request failed with BAD_REQUEST status for call to $url and headers $headersForLogging: ${e.getMessage}",
          e
        )
        Future.failed(e)
      case NonFatal(e) =>
        logger.error(
          s"[$LoggerComponentId][call] request failed for call to $url and headers $headersForLogging: ${e.getMessage}",
          e
        )
        Future.failed(e)
    }
  }

  private def auditCallRequest(url: String, request: CustomsDataStoreRequest)(implicit hc: HeaderCarrier): Unit =
    Future.successful {
      audit.sendDataEvent(
        transactionName = "update-data-store",
        path = url,
        detail = request.toMap(),
        eventType = "Customs-Data-Store-Update-Request"
      )
    }

  private def auditCallResponse(url: String, response: HttpResponse)(implicit hc: HeaderCarrier): Unit =
    Future.successful {
      audit.sendDataEvent(
        transactionName = "customs-data-store",
        path = url,
        detail = Map("status" -> response.status.toString),
        eventType = "Customs-Data-Store-Update-Response"
      )
    }

}
