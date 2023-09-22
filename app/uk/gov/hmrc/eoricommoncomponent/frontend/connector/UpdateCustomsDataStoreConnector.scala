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
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import play.mvc.Http.MimeTypes
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.CustomsDataStoreRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{CustomsDataStoreUpdate, UpdateRequest, UpdateResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class UpdateCustomsDataStoreConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditable)(
  implicit ec: ExecutionContext
) {

  val LoggerComponentId = "UpdateCustomsDataStoreConnector"
  private val logger    = Logger(this.getClass)

  def updateCustomsDataStore(request: CustomsDataStoreRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = url"${appConfig.handleSubscriptionBaseUrl}/customs/update/datastore"
    // $COVERAGE-OFF$Loggers
    logger.info(s"[$LoggerComponentId][call] postUrl: $url")
    // $COVERAGE-ON

    httpClient
      .post(url)
      .withBody(Json.toJson(request))
      .setHeader(ACCEPT -> "application/vnd.hmrc.1.0+json")
      .setHeader(CONTENT_TYPE -> MimeTypes.JSON)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[HttpResponse] map { response =>
      auditCall(url.toString, request, response)
      response.status match {
        case OK | NO_CONTENT =>
          // $COVERAGE-OFF$Loggers
          logger.info(s"[$LoggerComponentId][call] complete for call to $url with status:${response.status}")
        // $COVERAGE-ON
        case _ => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case e: BadRequestException =>
        // $COVERAGE-OFF$Loggers
        logger.warn(
          s"[$LoggerComponentId][call] request failed with BAD_REQUEST status for call to $url: ${e.getMessage}",
          e
        )
        // $COVERAGE-ON
        Future.failed(e)
      case NonFatal(e) =>
        logger.error(s"[$LoggerComponentId][call] request failed for call to $url: ${e.getMessage}", e)
        Future.failed(e)
    }
  }

  private def auditCall(url: String, request: CustomsDataStoreRequest, response: HttpResponse)(implicit
    hc: HeaderCarrier
  ): Unit = {
    val updateRequest  = UpdateRequest(request)
    val updateResponse = UpdateResponse(response)
    audit.sendExtendedDataEvent(
      transactionName = "customs-data-store",
      path = url,
      details = Json.toJson(CustomsDataStoreUpdate(updateRequest, updateResponse)),
      eventType = "CustomsDataStoreUpdate"
    )
  }

}
