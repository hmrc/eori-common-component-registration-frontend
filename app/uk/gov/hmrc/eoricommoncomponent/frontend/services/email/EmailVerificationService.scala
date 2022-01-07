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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.email

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationRequestHttpParser.{
  EmailAlreadyVerified,
  EmailVerificationRequestSent
}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationStateHttpParser.{
  EmailNotVerified,
  EmailVerified
}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (emailVerificationConnector: EmailVerificationConnector)(implicit
  ec: ExecutionContext
) {

  def isEmailVerified(email: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    emailVerificationConnector.getEmailVerificationState(email) map {
      case Right(EmailVerified)    => Some(true)
      case Right(EmailNotVerified) => Some(false)
      case Left(_)                 => None
    }

  def createEmailVerificationRequest(email: String, continueUrl: String)(implicit
    hc: HeaderCarrier
  ): Future[Option[Boolean]] =
    emailVerificationConnector.createEmailVerificationRequest(email, continueUrl) map {
      case Right(EmailVerificationRequestSent) => Some(true)
      case Right(EmailAlreadyVerified)         => Some(false)
      case _                                   => None
    }

}
