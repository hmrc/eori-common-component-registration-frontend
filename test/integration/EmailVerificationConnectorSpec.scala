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

package integration

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.http._
import util.externalservices.ExternalServicesConfig._
import util.externalservices.EmailVerificationStubService
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import play.api.inject.bind
import uk.gov.hmrc.eoricommoncomponent.frontend.models.email.{
  ResponseWithURI,
  VerificationStatus,
  VerificationStatusResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.ResponseError
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import play.api.i18n._

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
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  implicit val messages: Messages = MessagesImpl(Lang("en"), app.injector.instanceOf[MessagesApi])

  private lazy val connector      = app.injector.instanceOf[EmailVerificationConnector]
  private val email               = "john.doe@example.com"
  private val expectedContinueUrl = "/customs-enrolment-services/test-email-continue/"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val verifyEmailSuccessResponse: JsValue = Json.parse("""{"redirectUri": "google.com"}""")

  before {
    resetMockServer()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  "startVerificationJourney" should {

    val credId  = "123"
    val service = Service.cds
    val email   = "123@abc.com"

    "return a Right containing the URI when a CREATED is returned" in {
      EmailVerificationStubService.stubVerifyEmailSuccess()

      val expected = Right(ResponseWithURI("google.com"))
      val result: Either[ResponseError, ResponseWithURI] =
        await(connector.startVerificationJourney(credId, service, email).value)

      result mustBe expected
    }

    "return a Left containing the response details when anything but CREATED is returned" in {
      EmailVerificationStubService.stubVerifyEmailFailure()

      val expected = Left(ResponseError(500, "Unexpected response from verify-email: Something went wrong"))
      val result: Either[ResponseError, ResponseWithURI] =
        await(connector.startVerificationJourney(credId, service, email).value)

      result mustBe expected
    }

    "return a Left describing that the json was invalid when CREATED is returned but the JSON is not in the expected format" in {
      EmailVerificationStubService.stubVerifyEmailInvalid()

      val expected = Left(ResponseError(500, """Invalid JSON returned: {"something":"google.com"}"""))
      val result: Either[ResponseError, ResponseWithURI] =
        await(connector.startVerificationJourney(credId, service, email).value)

      result mustBe expected
    }

  }

  "getVerificationStatus" should {

    val credId = "123"

    "return a Right containing verification status' when an OK is returned" in {
      EmailVerificationStubService.stubVerificationStatusSuccess(credId)

      val emailVerificationStatuses = Seq(
        VerificationStatus(emailAddress = "fredbloggs@hotmail.com", verified = true, locked = false),
        VerificationStatus(emailAddress = "somename@live.com", verified = false, locked = true)
      )

      val expected = Right(VerificationStatusResponse(emailVerificationStatuses))
      val result: Either[ResponseError, VerificationStatusResponse] =
        await(connector.getVerificationStatus(credId).value)

      result mustBe expected
    }

    "return a Left containing the response details when anything but OK is returned" in {
      EmailVerificationStubService.stubVerificationStatusFailure(credId)

      val expected = Left(ResponseError(500, "Unexpected response from verification-status: Something went wrong"))
      val result: Either[ResponseError, VerificationStatusResponse] =
        await(connector.getVerificationStatus(credId).value)

      result mustBe expected
    }

    "return a Left describing that the json was invalid when OK is returned but the JSON is not in the expected format" in {
      EmailVerificationStubService.stubVerificationStatusInvalid(credId)

      val expected = Left(ResponseError(500, """Invalid JSON returned: {"something":"google.com"}"""))
      val result: Either[ResponseError, VerificationStatusResponse] =
        await(connector.getVerificationStatus(credId).value)

      result mustBe expected
    }
  }

}
