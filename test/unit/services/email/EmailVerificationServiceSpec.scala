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

package unit.services.email

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.Mockito._
import org.mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.ResponseError
import cats.data.EitherT
import uk.gov.hmrc.eoricommoncomponent.frontend.models.email.{
  EmailVerificationStatus,
  ResponseWithURI,
  VerificationStatus,
  VerificationStatusResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import org.mockito.ArgumentMatchers.any
import play.api.i18n._
import scala.concurrent.duration._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig

import scala.concurrent.{Await, Future}

class EmailVerificationServiceSpec
    extends AsyncWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterAll
    with BeforeAndAfterEach {

  private val mockConnector            = mock[EmailVerificationConnector]
  private val mockAppConfig: AppConfig = mock[AppConfig]

  implicit val hc: HeaderCarrier       = mock[HeaderCarrier]
  implicit val rq: Request[AnyContent] = mock[Request[AnyContent]]

  val sut = new EmailVerificationService(mockConnector, mockAppConfig)

  implicit val messages: Messages = mock[Messages]

  private val email          = "test@example.com"
  private val differentEmail = "different@example.com"
  private val continueUrl    = "/customs-enrolment-services/test-continue-url"

  override protected def beforeEach(): Unit = {
    reset(mockConnector)
    when(mockAppConfig.emailVerificationEnabled) thenReturn true
  }

  def mockGetVerificationStatus(
    credId: String
  )(response: EitherT[Future, ResponseError, VerificationStatusResponse]): Unit =
    when(
      mockConnector.getVerificationStatus(ArgumentMatchers.eq(credId))(ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response

  val credId = "123"

  "getVerificationStatus" should {

    "return Error when the connector returns an Error" in {

      val expected: Either[ResponseError, VerificationStatusResponse] = Left(ResponseError(500, "Something went wrong"))
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(expected)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Locked where the input email has locked=true" in {

      val expected = Right(EmailVerificationStatus.Locked)
      val response: Either[ResponseError, VerificationStatusResponse] = Right(
        VerificationStatusResponse(Seq(VerificationStatus(emailAddress = email, verified = false, locked = true)))
      )
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Verified where the input email has verified=true" in {

      val expected = Right(EmailVerificationStatus.Verified)
      val response: Either[ResponseError, VerificationStatusResponse] = Right(
        VerificationStatusResponse(Seq(VerificationStatus(emailAddress = email, verified = true, locked = false)))
      )
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Unverified where it doesn't exist but a different email has verified=true" in {

      val expected = Right(EmailVerificationStatus.Unverified)
      val response: Either[ResponseError, VerificationStatusResponse] = Right(
        VerificationStatusResponse(
          Seq(VerificationStatus(emailAddress = differentEmail, verified = true, locked = false))
        )
      )
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Unverified where it doesn't exist but a different email has locked=true" in {

      val expected = Right(EmailVerificationStatus.Unverified)
      val response: Either[ResponseError, VerificationStatusResponse] = Right(
        VerificationStatusResponse(
          Seq(VerificationStatus(emailAddress = differentEmail, verified = false, locked = true))
        )
      )
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Unverified where an empty list is returned" in {

      val expected                                                    = Right(EmailVerificationStatus.Unverified)
      val response: Either[ResponseError, VerificationStatusResponse] = Right(VerificationStatusResponse(Nil))
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Locked where the input email has locked=true and a different email exists" in {

      val expected = Right(EmailVerificationStatus.Locked)
      val sequence = Seq(
        VerificationStatus(emailAddress = email, verified = false, locked = true),
        VerificationStatus(emailAddress = differentEmail, verified = true, locked = false)
      )
      val response: Either[ResponseError, VerificationStatusResponse] = Right(VerificationStatusResponse(sequence))
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Verified where the input email has verified=true and a different email exists" in {

      val expected = Right(EmailVerificationStatus.Verified)
      val sequence = Seq(
        VerificationStatus(emailAddress = email, verified = true, locked = false),
        VerificationStatus(emailAddress = differentEmail, verified = false, locked = true)
      )
      val response: Either[ResponseError, VerificationStatusResponse] = Right(VerificationStatusResponse(sequence))
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus(email, credId).value.map { res =>
        res shouldEqual expected
      }
    }

    "return Verified where they return a verified email all lowercase, but the email in our cache is upper case" in {

      val expected                                                    = Right(EmailVerificationStatus.Verified)
      val sequence                                                    = Seq(VerificationStatus(emailAddress = "test@test.com", verified = true, locked = false))
      val response: Either[ResponseError, VerificationStatusResponse] = Right(VerificationStatusResponse(sequence))
      mockGetVerificationStatus(credId)(EitherT[Future, ResponseError, VerificationStatusResponse] {
        Future.successful(response)
      })

      sut.getVerificationStatus("Test@TEST.com", credId).value.map { res =>
        res shouldEqual expected
      }
    }

  }

  def mockStartVerificationJourney(response: EitherT[Future, ResponseError, ResponseWithURI]): Unit =
    when(mockConnector.startVerificationJourney(any(), any(), any())(any(), any())) thenReturn response

  val service = Service.cds

  "startVerificationJourney" should {

    "return Error when the connector returns an Error" in {

      val expected: Either[ResponseError, ResponseWithURI] = Left(ResponseError(500, "Something went wrong"))
      mockStartVerificationJourney(EitherT[Future, ResponseError, ResponseWithURI](Future.successful(expected)))

      sut.startVerificationJourney(credId, service, email).value.map { res =>
        res shouldEqual expected
      }
    }

    "return a response when the connector returns a response" in {

      val expected: Either[ResponseError, ResponseWithURI] = Right(ResponseWithURI("Some uri"))
      mockStartVerificationJourney(EitherT[Future, ResponseError, ResponseWithURI](Future.successful(expected)))

      sut.startVerificationJourney(credId, service, email).value.map { res =>
        res shouldEqual expected
      }
    }

  }

}
