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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{SubscriptionRequest, SubscriptionResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{Subscription, SubscriptionResult, SubscriptionSubmitted}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionServiceConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditor)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url = url"${appConfig.getServiceUrl("subscribe")}"

  def subscribe(request: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[SubscriptionResponse] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(
      s"[Subscribe SUB02: $url, requestCommon: ${request.subscriptionCreateRequest.requestCommon} and hc: $hc"
    )
    // $COVERAGE-ON

    httpClient
      .post(url)
      .withBody(Json.toJson(request))
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[SubscriptionResponse] map { response =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"[Subscribe SUB02: responseCommon: ${response.subscriptionCreateResponse.responseCommon}")
      // $COVERAGE-ON

      audit.sendSubscriptionDataEvent(url.toString, Json.toJson(Subscription(SubscriptionSubmitted(request), SubscriptionResult(response))))
      response
    } recoverWith { case e: Throwable =>
      // $COVERAGE-OFF$Loggers
      logger.warn(
        s"Subscribe SUB02 request failed for acknowledgementReference : ${request.subscriptionCreateRequest.requestCommon.acknowledgementReference}. Reason: $e"
      )
      // $COVERAGE-ON
      Future.failed(e)
    }
  }
}
