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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{RegisterWithoutIdRequestHolder, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.RegisterWithoutId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterWithoutIdConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url    = appConfig.getServiceUrl("register-without-id")

  def register(request: RegisterWithoutIDRequest)(implicit hc: HeaderCarrier): Future[RegisterWithoutIDResponse] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"Register: $url, body: $request and hc: $hc")
    // $COVERAGE-ON

    http.POST[RegisterWithoutIdRequestHolder, RegisterWithoutIdResponseHolder](
      url,
      RegisterWithoutIdRequestHolder(request)
    ) map { response =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"Register: responseCommon: ${response.registerWithoutIDResponse.responseCommon}")
      // $COVERAGE-ON

      auditCall(url, request, response)
      response.registerWithoutIDResponse
    } recover {
      case e: Throwable =>
        logger.warn(
          s"Failure. postUrl: $url, acknowledgement ref: ${request.requestCommon.acknowledgementReference}, error: $e"
        )
        throw e
    }
  }

  private def auditCall(url: String, request: RegisterWithoutIDRequest, response: RegisterWithoutIdResponseHolder)(
    implicit hc: HeaderCarrier
  ): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "ecc-registration-without-id",
      path = url,
      details = Json.toJson(RegisterWithoutId(request, response)),
      eventType = "RegistrationWithoutId"
    )

}
