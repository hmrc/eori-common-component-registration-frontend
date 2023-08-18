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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.email

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.models.email.{
  EmailVerificationStatus,
  ResponseWithURI,
  VerificationStatus
}
import uk.gov.hmrc.http.HeaderCarrier
import cats.data.EitherT
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.ResponseError
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (emailVerificationConnector: EmailVerificationConnector, appConfig: AppConfig)(
  implicit ec: ExecutionContext
) {

  val verifiedResponse: Future[Either[ResponseError, EmailVerificationStatus]] =
    Future.successful(Right(EmailVerificationStatus.Verified))

  def getVerificationStatus(email: String, credId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ResponseError, EmailVerificationStatus] =
    if (appConfig.emailVerificationEnabled)
      emailVerificationConnector.getVerificationStatus(credId).map { statusResponse =>
        val emailStatus: Option[VerificationStatus] = statusResponse.emails.find(_.emailAddress == email)

        emailStatus match {
          case Some(status) if status.locked   => EmailVerificationStatus.Locked
          case Some(status) if status.verified => EmailVerificationStatus.Verified
          case _                               => EmailVerificationStatus.Unverified
        }
      }
    else EitherT(verifiedResponse)

  def startVerificationJourney(credId: String, service: Service, email: String)(implicit
    hc: HeaderCarrier,
    messages: Messages
  ): EitherT[Future, ResponseError, ResponseWithURI] =
    emailVerificationConnector.startVerificationJourney(credId, service, email)

}
