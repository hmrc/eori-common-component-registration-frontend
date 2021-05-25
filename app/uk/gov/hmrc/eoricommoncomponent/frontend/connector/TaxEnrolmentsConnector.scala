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
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest.GovernmentGatewayEnrolmentRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{IssuerCall, IssuerRequest, IssuerResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.HttpStatusCheck
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger         = Logger(this.getClass)
  private val baseUrl        = appConfig.taxEnrolmentsBaseUrl
  val serviceContext: String = appConfig.taxEnrolmentsServiceContext

  /**
    *
    * @param request
    * @param formBundleId
    * @param hc
    * @param e
    * @return
    *  This is a issuer call which ETMP makes but we will do this for migrated users
    *  when subscription status((SUB02 Api CALL)) is 04 (SubscriptionExists)
    */
  def enrol(request: TaxEnrolmentsRequest, formBundleId: String)(implicit hc: HeaderCarrier): Future[Int] = {
    val url = s"$baseUrl/$serviceContext/subscriptions/$formBundleId/issuer"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"[Enrol: $url, body: $request and hc: $hc")
    // $COVERAGE-ON

    http.PUT[TaxEnrolmentsRequest, HttpResponse](url, request) map { response: HttpResponse =>
      logResponse("Enrol", response)
      auditCall(url, request, response)
      response.status
    }
  }

  def enrolAndActivate(enrolmentKey: String, request: GovernmentGatewayEnrolmentRequest)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val url = s"$baseUrl/$serviceContext/service/$enrolmentKey/enrolment"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"[EnrolAndActivate: $url, body: $request and hc: $hc")
    // $COVERAGE-ON

    http.PUT[GovernmentGatewayEnrolmentRequest, HttpResponse](url = url, body = request) map { response =>
      logResponse("EnrolAndActivate", response)
      response
    }
  }

  private def auditCall(url: String, request: TaxEnrolmentsRequest, response: HttpResponse)(implicit
    hc: HeaderCarrier
  ): Unit = {
    val issuerRequest  = IssuerRequest(request)
    val issuerResponse = IssuerResponse(response)

    audit.sendExtendedDataEvent(
      transactionName = "ecc-issuer-call",
      path = url,
      details = Json.toJson(IssuerCall(issuerRequest, issuerResponse)),
      eventType = "IssuerCall"
    )
  }

  private def logResponse(service: String, response: HttpResponse): Unit =
    if (HttpStatusCheck.is2xx(response.status))
      logger.debug(s"$service request is successful")
    else
      logger.warn(s"$service request is failed with response $response")

}
