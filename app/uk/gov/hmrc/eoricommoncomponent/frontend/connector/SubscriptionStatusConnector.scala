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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{
  SubscriptionStatus,
  SubscriptionStatusResult,
  SubscriptionStatusSubmitted
}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionStatusConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url    = appConfig.getServiceUrl("subscription-status")

  def status(
    request: SubscriptionStatusQueryParams
  )(implicit hc: HeaderCarrier, originatingService: Service): Future[SubscriptionStatusResponse] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"Status SUB01: $url, queryParams: ${request.queryParams} and hc: $hc")
    // $COVERAGE-ON

    http.GET[SubscriptionStatusResponseHolder](url, request.queryParams) map { resp =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"Status SUB01: responseCommon: ${resp.subscriptionStatusResponse.responseCommon}")
      // $COVERAGE-ON

      auditCall(url, request, resp)
      resp.subscriptionStatusResponse
    } recover {
      case e: Throwable =>
        logger.warn(s"Status SUB01 failed. url: $url, error: $e", e)
        throw e
    }
  }

  private def auditCall(
    url: String,
    request: SubscriptionStatusQueryParams,
    response: SubscriptionStatusResponseHolder
  )(implicit hc: HeaderCarrier, originatingService: Service): Unit = {

    val subscriptionStatusSubmitted = SubscriptionStatusSubmitted(request, originatingService.code)
    val subscriptionStatusResult    = SubscriptionStatusResult(response)

    audit.sendExtendedDataEvent(
      transactionName = "ecc-subscription-status",
      path = url,
      details = Json.toJson(SubscriptionStatus(subscriptionStatusSubmitted, subscriptionStatusResult)),
      eventType = "SubscriptionStatus"
    )
  }

}
