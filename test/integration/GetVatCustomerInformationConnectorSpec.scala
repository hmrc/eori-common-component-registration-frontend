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
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.GetVatCustomerInformationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.GetVatInformationResponse
import uk.gov.hmrc.http._
import util.externalservices.ExternalServicesConfig._
import util.externalservices.GetVatInformationMessagingService

class GetVatCustomerInformationConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.handle-subscription.host" -> Host,
        "microservice.services.handle-subscription.port" -> Port,
        "auditing.enabled"                               -> true,
        "auditing.consumer.baseUri.host"                 -> Host,
        "auditing.consumer.baseUri.port"                 -> Port
      )
    )
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  before {
    resetMockServer()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  private lazy val connector = app.injector.instanceOf[GetVatCustomerInformationConnector]
  private val vrn            = "123456789"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getVatCustomerInformation" should {
    "return successful GetVatInformationResponse response with OK status" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseOK()

      val format   = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val expected = Right(GetVatInformationResponse(Some(format.parse("2021-01-31")), Some("SE28 1AA")))
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)

      result mustBe expected
    }

    "return NOT FOUND response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseNotFound()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(NOT_FOUND)
    }

    "return BAD REQUEST response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseBadRequest()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(BAD_REQUEST)
    }

    "return FORBIDDEN response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseForbidden()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(FORBIDDEN)
    }

    "return INTERNAL_SERVER_ERROR response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseInternalServerError()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(INTERNAL_SERVER_ERROR)
    }

    "return BAD_GATEWAY response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseBadGateway()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(BAD_GATEWAY)
    }

    "return SERVICE_UNAVAILABLE response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseServiceUnavailable()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(SERVICE_UNAVAILABLE)
    }

    "handle response for unexpected status " in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseUnexpectedStatus()
      val result: Either[Int, GetVatInformationResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(LENGTH_REQUIRED)
    }

  }
}
