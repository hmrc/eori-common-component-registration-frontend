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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  SubscriptionDisplayResponse,
  SubscriptionDisplayResponseHolder
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{
  SubscriptionDisplay,
  SubscriptionDisplayResult,
  SubscriptionDisplaySubmitted
}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SUB09SubscriptionDisplayConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url    = appConfig.getServiceUrl("subscription-display")

  def subscriptionDisplay(
    sub09Request: Seq[(String, String)]
  )(implicit hc: HeaderCarrier): Future[Either[EoriHttpResponse, SubscriptionDisplayResponse]] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"SubscriptionDisplay SUB09: $url, body: $sub09Request and hc: $hc")
    // $COVERAGE-ON

    http.GET[SubscriptionDisplayResponseHolder](url, sub09Request) map { resp =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"SubscriptionDisplay SUB09: responseCommon: ${resp.subscriptionDisplayResponse.responseCommon}")
      // $COVERAGE-ON

      auditCall(url, sub09Request, resp)
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
