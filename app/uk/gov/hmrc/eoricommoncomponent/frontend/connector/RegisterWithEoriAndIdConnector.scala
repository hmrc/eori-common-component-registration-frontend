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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  RegisterWithEoriAndIdRequest,
  RegisterWithEoriAndIdRequestHolder,
  RegisterWithEoriAndIdResponse,
  RegisterWithEoriAndIdResponseHolder
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{Registration, RegistrationResult, RegistrationSubmitted}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegisterWithEoriAndIdConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val url    = appConfig.getServiceUrl("register-with-eori-and-id")

  def register(
    request: RegisterWithEoriAndIdRequest
  )(implicit hc: HeaderCarrier): Future[RegisterWithEoriAndIdResponse] = {

    // $COVERAGE-OFF$Loggers
    logger.debug(s"REG06 Register: $url, requestCommon: ${request.requestCommon} and hc: $hc")
    // $COVERAGE-ON

    http.POST[RegisterWithEoriAndIdRequestHolder, RegisterWithEoriAndIdResponseHolder](
      url,
      RegisterWithEoriAndIdRequestHolder(request)
    ) map { resp =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"REG06 Register: responseCommon: ${resp.registerWithEORIAndIDResponse.responseCommon}")
      // $COVERAGE-ON

      auditCall(url, request, resp)
      resp.registerWithEORIAndIDResponse.withAdditionalInfo(request.requestDetail.registerModeID)
    } recover {
      case e: Throwable =>
        logger.warn(
          s"REG06 Register failed. postUrl: $url, acknowledgement ref: ${request.requestCommon.acknowledgementReference}, error: $e"
        )
        throw e
    }
  }

  private def auditCall(
    url: String,
    request: RegisterWithEoriAndIdRequest,
    response: RegisterWithEoriAndIdResponseHolder
  )(implicit hc: HeaderCarrier): Unit = {
    val registrationSubmitted = RegistrationSubmitted(request)
    val registrationResult    = RegistrationResult(response)

    audit.sendExtendedDataEvent(
      transactionName = "ecc-registration",
      path = url,
      details = Json.toJson(Registration(registrationSubmitted, registrationResult)),
      eventType = "Registration"
    )
  }

}
