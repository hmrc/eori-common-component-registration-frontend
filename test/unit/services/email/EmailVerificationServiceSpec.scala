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

package unit.services.email

import base.UnitSpec
import org.mockito.Mockito._
import org.mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationRequestHttpParser.{
  EmailAlreadyVerified,
  EmailVerificationRequestFailure,
  EmailVerificationRequestResponse,
  EmailVerificationRequestSent
}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationStateHttpParser.{
  EmailNotVerified,
  EmailVerificationStateErrorResponse,
  EmailVerificationStateResponse,
  EmailVerified
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailVerificationServiceSpec
    extends UnitSpec with ScalaFutures with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {
  private val mockConnector = mock[EmailVerificationConnector]

  implicit val hc: HeaderCarrier       = mock[HeaderCarrier]
  implicit val rq: Request[AnyContent] = mock[Request[AnyContent]]

  val service = new EmailVerificationService(mockConnector)

  private val email       = "test@example.com"
  private val continueUrl = "/customs-enrolment-services/test-continue-url"

  override protected def beforeEach(): Unit =
    reset(mockConnector)

  def mockGetEmailVerificationState(emailAddress: String)(response: Future[EmailVerificationStateResponse]): Unit =
    when(
      mockConnector.getEmailVerificationState(ArgumentMatchers.eq(emailAddress))(ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response

  def mockCreateEmailVerificationRequest(emailAddress: String, continueUrl: String)(
    response: Future[EmailVerificationRequestResponse]
  ): Unit =
    when(
      mockConnector.createEmailVerificationRequest(ArgumentMatchers.eq(emailAddress), ArgumentMatchers.eq(continueUrl))(
        ArgumentMatchers.any[HeaderCarrier]
      )
    ) thenReturn response

  "Checking email verification status" when {

    "the email is verified" should {

      "return Some(true)" in {

        mockGetEmailVerificationState(email)(Future.successful(Right(EmailVerified)))
        val res: Option[Boolean] =
          await(service.isEmailVerified(email))
        res shouldBe Some(true)
      }

      "the email is not verified" should {

        "return Some(false)" in {

          mockGetEmailVerificationState(email)(Future.successful(Right(EmailNotVerified)))
          val res: Option[Boolean] =
            await(service.isEmailVerified(email))
          res shouldBe Some(false)
        }
      }

      "the email is check failed" should {

        "return None" in {

          mockGetEmailVerificationState(email)(
            Future.successful(Left(EmailVerificationStateErrorResponse(BAD_REQUEST, "")))
          )
          val res: Option[Boolean] =
            await(service.isEmailVerified(email))
          res shouldBe None
        }
      }
    }
  }

  "Creating an email verification request" when {

    "the email verification request is sent successfully" should {

      "return Some(true)" in {
        mockCreateEmailVerificationRequest(email, continueUrl)(Future.successful(Right(EmailVerificationRequestSent)))
        val res: Option[Boolean] =
          await(service.createEmailVerificationRequest(email, continueUrl))
        res shouldBe Some(true)
      }
    }

    "the email address has already been verified" should {

      "return Some(false)" in {

        mockCreateEmailVerificationRequest(email, continueUrl)(Future.successful(Right(EmailAlreadyVerified)))
        val res: Option[Boolean] =
          await(service.createEmailVerificationRequest(email, continueUrl))
        res shouldBe Some(false)
      }
    }

    "the email address verification request failed" should {

      "return None" in {
        mockCreateEmailVerificationRequest(email, continueUrl)(
          Future.successful(Left(EmailVerificationRequestFailure(BAD_REQUEST, "")))
        )
        val res: Option[Boolean] =
          await(service.createEmailVerificationRequest(email, continueUrl))
        res shouldBe None
      }
    }
  }
}
