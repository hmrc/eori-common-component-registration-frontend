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

package integration

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationStateHttpParser.EmailVerificationStateResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationRequestHttpParser.{
  EmailAlreadyVerified,
  EmailVerificationRequestFailure,
  EmailVerificationRequestResponse,
  EmailVerificationRequestSent
}
import uk.gov.hmrc.http._
import util.externalservices.ExternalServicesConfig._
import util.externalservices.EmailVerificationStubService
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.EmailVerificationStateHttpParser._

class EmailVerificationConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.email-verification.host"               -> Host,
        "microservice.services.email-verification.port"               -> Port,
        "microservice.services.email-verification.context"            -> "email-verification",
        "microservice.services.email-verification.templateId"         -> "verifyEmailAddresssbt",
        "microservice.services.email-verification.LinkExpiryDuration" -> "P1D",
        "auditing.enabled"                                            -> false,
        "auditing.consumer.baseUri.host"                              -> Host,
        "auditing.consumer.baseUri.port"                              -> Port
      )
    )
    .build()

  private lazy val connector      = app.injector.instanceOf[EmailVerificationConnector]
  private val email               = "john.doe@example.com"
  private val expectedContinueUrl = "/customs-enrolment-services/test-email-continue/"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val emailVerifiedResponseJson: JsValue = Json.parse("""{"email": "john.doe@example.com"}""")

  val emailVerificationNotFoundJson: JsValue = Json.parse("""{
      |  "code": "NOT_VERIFIED",
      |  "message":"Email not verified."
      |}""".stripMargin)

  val internalServerErrorJson: JsValue = Json.parse("""{
      |  "code": "UNEXPECTED_ERROR",
      |  "message":"An unexpected error occurred."
      |}""".stripMargin)

  before {
    resetMockServer()
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "Calling getEmailVerificationState" when {

    "the email is verified" should {
      "return an EmailVerified response" in {
        EmailVerificationStubService.stubEmailVerified

        val expected                               = Right(EmailVerified)
        val result: EmailVerificationStateResponse = await(connector.getEmailVerificationState(email))

        result mustBe expected
      }
    }
    "the email is not verified" should {

      "return an EmailNotVerified response" in {
        EmailVerificationStubService.stubEmailNotVerified

        val expected = Right(EmailNotVerified)
        val result: EmailVerificationStateResponse =
          await(connector.getEmailVerificationState("notverified@example.com"))

        result mustBe expected
      }
    }

    "the email service Internal Server Error" should {

      "return an Internal Server Error" in {
        EmailVerificationStubService.stubEmailVerifiedInternalServerError
        val expected = Left(
          EmailVerificationStateErrorResponse(
            INTERNAL_SERVER_ERROR,
            EmailVerificationStubService.internalServerErrorJson.toString
          )
        )
        val result: EmailVerificationStateResponse = await(connector.getEmailVerificationState(email))

        result mustBe expected
      }
    }

  }

  "Calling createEmailVerificationRequest" when {

    "the post is successful" should {
      "return an EmailVerificationRequestSent" in {
        val expected = Right(EmailVerificationRequestSent)
        EmailVerificationStubService.stubVerificationRequestSent
        val result: EmailVerificationRequestResponse =
          await(connector.createEmailVerificationRequest(email, expectedContinueUrl))

        result mustBe expected
      }
    }

    "the email is already verified" should {

      "return an EmailAlreadyVerified" in {
        val expected = Right(EmailAlreadyVerified)
        EmailVerificationStubService.stubEmailAlreadyVerified

        val result: EmailVerificationRequestResponse =
          await(connector.createEmailVerificationRequest(email, expectedContinueUrl))

        result mustBe expected
      }
    }

    "the email service Internal Server Error" should {

      "return an Internal Server Error" in {

        val expected = Left(
          EmailVerificationRequestFailure(
            INTERNAL_SERVER_ERROR,
            EmailVerificationStubService.internalServerErrorJson.toString
          )
        )
        EmailVerificationStubService.stubVerificationRequestError

        val result: EmailVerificationRequestResponse =
          await(connector.createEmailVerificationRequest("scala@example.com", "/home"))

        result mustBe expected

      }
    }

  }

}
