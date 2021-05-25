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
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationKeys.{
  LinkExpiryDurationKey,
  TemplateIdKey,
  _
}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationRequestHttpParser.EmailVerificationRequestResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationStateHttpParser.EmailVerificationStateResponse
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  private[connector] lazy val checkVerifiedEmailUrl: String =
    s"${appConfig.emailVerificationBaseUrl}/${appConfig.emailVerificationServiceContext}/verified-email-check"

  private[connector] lazy val createEmailVerificationRequestUrl: String =
    s"${appConfig.emailVerificationBaseUrl}/${appConfig.emailVerificationServiceContext}/verification-requests"

  def getEmailVerificationState(
    emailAddress: String
  )(implicit hc: HeaderCarrier): Future[EmailVerificationStateResponse] = {

    val body = Json.obj("email" -> emailAddress)

    // $COVERAGE-OFF$Loggers
    logger.debug(s"GetEmailVerificationState: $checkVerifiedEmailUrl, body: $body and hc: $hc")
    // $COVERAGE-ON

    def logResponse(response: EmailVerificationStateResponse): Unit =
      // $COVERAGE-OFF$Loggers
      response match {
        case Right(success) => logger.debug(s"GetEmailVerificationState succeeded $success")
        case Left(failed)   => logger.warn(s"GetEmailVerificationState failed $failed")
      }
    // $COVERAGE-ON

    http.POST[JsObject, EmailVerificationStateResponse](checkVerifiedEmailUrl, body).map {
      response =>
        logResponse(response)
        response
    }
  }

  def createEmailVerificationRequest(emailAddress: String, continueUrl: String)(implicit
    hc: HeaderCarrier
  ): Future[EmailVerificationRequestResponse] = {
    val jsonBody =
      Json.obj(
        EmailKey              -> emailAddress,
        TemplateIdKey         -> appConfig.emailVerificationTemplateId,
        TemplateParametersKey -> Json.obj(),
        LinkExpiryDurationKey -> appConfig.emailVerificationLinkExpiryDuration,
        ContinueUrlKey        -> continueUrl
      )

    // $COVERAGE-OFF$Loggers
    logger.debug(s"CreateEmailVerificationRequest: $createEmailVerificationRequestUrl, body: $jsonBody and hc: $hc")
    // $COVERAGE-ON

    def logResponse(response: EmailVerificationRequestResponse): Unit =
      // $COVERAGE-OFF$Loggers
      response match {
        case Right(success) => logger.debug(s"CreateEmailVerificationRequest succeeded $success")
        case Left(failed)   => logger.warn(s"CreateEmailVerificationRequest failed $failed")
      }
    // $COVERAGE-ON

    http.POST[JsObject, EmailVerificationRequestResponse](createEmailVerificationRequestUrl, jsonBody).map {
      response =>
        logResponse(response)
        response
    }
  }

}

object EmailVerificationKeys {
  val EmailKey              = "email"
  val TemplateIdKey         = "templateId"
  val TemplateParametersKey = "templateParameters"
  val LinkExpiryDurationKey = "linkExpiryDuration"
  val ContinueUrlKey        = "continueUrl"
}
