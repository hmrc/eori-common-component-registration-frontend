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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{
  RegisterWithId,
  RegisterWithIdConfirmation,
  RegisterWithIdSubmitted
}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import play.api.http.HeaderNames.AUTHORIZATION
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingServiceConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  private val url          = url"${appConfig.getServiceUrl("register-with-id")}"
  private val NoMatchFound = "002 - No Match Found"

  private def handleResponse(response: MatchingResponse): Option[MatchingResponse] = {
    val statusTxt = response.registerWithIDResponse.responseCommon.statusText
    if (statusTxt.exists(_.equalsIgnoreCase(NoMatchFound))) None
    else Some(response)
  }

  def lookup(req: MatchingRequestHolder)(implicit hc: HeaderCarrier): Future[Option[MatchingResponse]] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"REG01 Lookup: ${url.toString}, requestCommon: ${req.registerWithIDRequest.requestCommon} and hc: $hc")
    // $COVERAGE-ON

    httpClient
      .post(url)
      .withBody(Json.toJson(req))
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[MatchingResponse] map { resp =>
        // $COVERAGE-OFF$Loggers
        logger.debug(s"REG01 Lookup: responseCommon: ${resp.registerWithIDResponse.responseCommon}")
        // $COVERAGE-ON

        auditCall(url.toString, req, resp)
        handleResponse(resp)
      } recover {
        case e: Throwable =>
          // $COVERAGE-OFF$Loggers
          logger.warn(
            s"REG01 Lookup failed for acknowledgement ref: ${req.registerWithIDRequest.requestCommon.acknowledgementReference}. Reason: $e"
          )
          // $COVERAGE-ON
          throw e
      }

  }

  private def auditCall(url: String, request: MatchingRequestHolder, response: MatchingResponse)(implicit
    hc: HeaderCarrier
  ): Unit = {
    val registerWithIdSubmitted    = RegisterWithIdSubmitted(request)
    val registerWithIdConfirmation = RegisterWithIdConfirmation(response)

    audit.sendExtendedDataEvent(
      transactionName = "ecc-registration",
      path = url,
      details = Json.toJson(RegisterWithId(registerWithIdSubmitted, registerWithIdConfirmation)),
      eventType = "Registration"
    )
  }

}
