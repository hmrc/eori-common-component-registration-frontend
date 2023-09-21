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
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  SubscriptionDisplayResponse,
  SubscriptionDisplayResponseHolder
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{
  SubscriptionDisplay,
  SubscriptionDisplayResult,
  SubscriptionDisplaySubmitted
}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SUB09SubscriptionDisplayConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditable)(
  implicit ec: ExecutionContext
) {

  private val logger  = Logger(this.getClass)
  private val baseUrl = appConfig.getServiceUrl("subscription-display")

  def subscriptionDisplay(safeId: String, acknowledgementReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[EoriHttpResponse, SubscriptionDisplayResponse]] = {

    val url = url"$baseUrl?regime=CDS&taxPayerID=$safeId&acknowledgementReference=$acknowledgementReference"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"SubscriptionDisplay SUB09: $url and hc: $hc")
    // $COVERAGE-ON

    httpClient
      .get(url)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[SubscriptionDisplayResponseHolder] map { resp =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"SubscriptionDisplay SUB09: responseCommon: ${resp.subscriptionDisplayResponse.responseCommon}")
      // $COVERAGE-ON

      auditCall(
        url.toString,
        Seq(
          "regime"                   -> Service.regimeCDS,
          "taxPayerID"               -> safeId,
          "acknowledgementReference" -> acknowledgementReference
        ),
        resp
      )
      Right(resp.subscriptionDisplayResponse)
    } recover {
      case NonFatal(e) =>
        logger.error(s"SubscriptionDisplay SUB09 failed. url: $url, error: $e")
        Left(ServiceUnavailableResponse)
    }
  }

  private def auditCall(url: String, request: Seq[(String, String)], response: SubscriptionDisplayResponseHolder)(
    implicit hc: HeaderCarrier
  ): Unit = {

    val subscriptionDisplaySubmitted = SubscriptionDisplaySubmitted.applyAndAlignKeys(request.toMap)
    val subscriptionDisplayResult    = SubscriptionDisplayResult(response)

    audit.sendExtendedDataEvent(
      transactionName = "ecc-subscription-display",
      path = url,
      details = Json.toJson(SubscriptionDisplay(subscriptionDisplaySubmitted, subscriptionDisplayResult)),
      eventType = "SubscriptionDisplay"
    )
  }

}
