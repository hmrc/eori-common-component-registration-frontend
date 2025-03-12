/*
 * Copyright 2025 HM Revenue & Customs
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

/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{GetVatCustomerInformationConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.VatControlListResponse
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

      val expected                                              = Right(VatControlListResponse(Some("SE28 1AA"), Some("2021-01-31")))
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)

      result mustBe expected
    }

    "return NOT FOUND response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseNotFound()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(
        ResponseError(
          NOT_FOUND,
          """{"failures":{"code":"NOT_FOUND","reason":"The back end has indicated that No subscription can be found."}}"""
        )
      )
    }

    "return BAD REQUEST response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseBadRequest()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)

      result mustBe Left(
        ResponseError(
          400,
          """{"failures":{"code":"INVALID_IDVALUE","reason":"Submission has not passed validation. Invalid path parameter idValue."}}"""
        )
      )
    }

    "return FORBIDDEN response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseForbidden()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(
        ResponseError(
          FORBIDDEN,
          """{"failures":{"code":"MIGRATION","reason":"The back end has indicated that a migration is in progress for this identification number"}}"""
        )
      )
    }

    "return INTERNAL_SERVER_ERROR response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseInternalServerError()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(
        ResponseError(
          INTERNAL_SERVER_ERROR,
          """{"failures":{"code":"SERVER_ERROR.","reason":"IF is currently experiencing problems that require live service intervention."}}"""
        )
      )
    }

    "return BAD_GATEWAY response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseBadGateway()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(
        ResponseError(
          BAD_GATEWAY,
          """{"failures":[{"code":"BAD_GATEWAY","reason":"Dependent systems are currently not responding."}]}"""
        )
      )
    }

    "return SERVICE_UNAVAILABLE response" in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseServiceUnavailable()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(
        ResponseError(
          SERVICE_UNAVAILABLE,
          """{"failures":[{"code":"SERVICE_UNAVAILABLE","reason":"Dependent systems are currently not responding."}]}"""
        )
      )
    }

    "handle response for unexpected status " in {
      GetVatInformationMessagingService.returnTheVatCustomerInformationResponseUnexpectedStatus()
      val result: Either[ResponseError, VatControlListResponse] =
        await(connector.getVatCustomerInformation(vrn).value)
      result mustBe Left(ResponseError(LENGTH_REQUIRED, ""))
    }

  }
}
