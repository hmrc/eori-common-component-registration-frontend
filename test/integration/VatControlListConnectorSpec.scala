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
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  ResponseError,
  ServiceUnavailableResponse,
  VatControlListConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatControlListRequest, VatControlListResponse}
import uk.gov.hmrc.http._
import util.externalservices.ExternalServicesConfig._
import util.externalservices.VatControlListMessagingService

class VatControlListConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.host"                                 -> Host,
        "microservice.services.eori-common-component-hods-proxy.port"                                 -> Port,
        "microservice.services.eori-common-component-hods-proxy.vat-known-facts-control-list.context" -> "vat-known-facts-control-list",
        "auditing.enabled"                                                                            -> false,
        "auditing.consumer.baseUri.host"                                                              -> Host,
        "auditing.consumer.baseUri.port"                                                              -> Port
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

  private lazy val vatControlListConnector = app.injector.instanceOf[VatControlListConnector]
  private val AValidVatRegistrationNumber  = "123456789"
  private val expectedGetUrl               = s"/vat-known-facts-control-list?vrn=$AValidVatRegistrationNumber"
  private val request                      = VatControlListRequest(vrn = AValidVatRegistrationNumber)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val responseWithOk: JsValue =
    Json.parse("""
        |{
        |      "traderName": "John Doe",
        |      "postcode": "SE28 1AA",
        |      "dateOfReg": "2017-01-01",
        |      "lastReturnMonthPeriod": "MAR",
        |      "lastNetDue": 10000.02,
        |      "controlListInformation": "10000000000000000000000000000000"
        |}
      """.stripMargin)

  val responseWithNotFound: JsValue =
    Json.parse("""{
      |  "code": "NOT_FOUND",
      |  "reason": "The back end has indicated that vat known facts cannot be returned"
      |}""".stripMargin)

  "Vat Sign Up Control List" should {
    "return successful response with OK status when VatControlList service returns 200" in {

      VatControlListMessagingService.returnTheVatControlListResponseOK()
      await(vatControlListConnector.vatControlList(request).value) must be(
        Right(responseWithOk.as[VatControlListResponse])
      )
    }

    "return Not Found status when VatControlList service returns 404" in {

      VatControlListMessagingService.returnNotFoundVatControlListResponse(expectedGetUrl, responseWithNotFound.toString)
      await(vatControlListConnector.vatControlList(request).value) must be(
        Left(
          ResponseError(
            NOT_FOUND,
            """{"code":"NOT_FOUND","reason":"The back end has indicated that vat known facts cannot be returned"}"""
          )
        )
      )
    }

    "fail when Not Found" in {
      VatControlListMessagingService.stubTheVatControlListResponse(
        expectedGetUrl,
        responseWithNotFound.toString,
        NOT_FOUND
      )

      await(vatControlListConnector.vatControlList(request).value) mustBe Left(
        ResponseError(
          NOT_FOUND,
          """{"code":"NOT_FOUND","reason":"The back end has indicated that vat known facts cannot be returned"}"""
        )
      )
    }

    "fail when Bad Request" in {
      VatControlListMessagingService.stubTheVatControlListResponse(expectedGetUrl, "bad request", BAD_REQUEST)

      await(vatControlListConnector.vatControlList(request).value) mustBe Left(
        ResponseError(BAD_REQUEST, "bad request")
      )
    }

    "return left when Internal Server Error" in {
      VatControlListMessagingService.stubTheVatControlListResponse(
        expectedGetUrl,
        responseWithOk.toString,
        INTERNAL_SERVER_ERROR
      )

      val result = await(vatControlListConnector.vatControlList(request).value)
      result mustBe Left(ResponseError(INTERNAL_SERVER_ERROR, "Incorrect VAT Known facts response"))
    }

    "fail when Service Unavailable" in {
      VatControlListMessagingService.stubTheVatControlListResponse(
        expectedGetUrl,
        responseWithOk.toString,
        SERVICE_UNAVAILABLE
      )

      val result = await(vatControlListConnector.vatControlList(request).value)
      result mustBe Left(ResponseError(503, responseWithOk.toString()))
    }

    "return InternalServerError when different status" in {

      VatControlListMessagingService.stubTheVatControlListResponse(expectedGetUrl, responseWithOk.toString, FORBIDDEN)

      val result = await(vatControlListConnector.vatControlList(request).value)
      result mustBe Left(ResponseError(INTERNAL_SERVER_ERROR, "Incorrect VAT Known facts response"))
    }
  }
}
