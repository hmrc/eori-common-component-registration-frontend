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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers

import play.api.Logger
import play.api.http.Status.{CONFLICT, CREATED}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object EmailVerificationRequestHttpParser {

  private val logger = Logger(this.getClass)

  type EmailVerificationRequestResponse = Either[EmailVerificationRequestFailure, EmailVerificationRequestSuccess]

  implicit object CreateEmailVerificationRequestHttpReads extends HttpReads[EmailVerificationRequestResponse] {

    override def read(method: String, url: String, response: HttpResponse): EmailVerificationRequestResponse =
      response.status match {
        case CREATED =>
          logger.debug("[CreateEmailVerificationRequestHttpReads][read] - Email request sent successfully")
          Right(EmailVerificationRequestSent)
        case CONFLICT =>
          logger.debug("[CreateEmailVerificationRequestHttpReads][read] - Email already verified")
          Right(EmailAlreadyVerified)
        case status =>
          logger.warn(
            "[CreateEmailVerificationRequestHttpParser][CreateEmailVerificationRequestHttpReads][read] - " +
              s"Failed to create email verification. Received status: $status Response body: ${response.body}"
          )
          Left(EmailVerificationRequestFailure(status, response.body))
      }

  }

  sealed trait EmailVerificationRequestSuccess

  case object EmailAlreadyVerified extends EmailVerificationRequestSuccess

  case object EmailVerificationRequestSent extends EmailVerificationRequestSuccess

  // TODO Why this doesn't extends EmailVerificationRequestSuccess? Why trait is just for success?
  case class EmailVerificationRequestFailure(status: Int, body: String)
}
