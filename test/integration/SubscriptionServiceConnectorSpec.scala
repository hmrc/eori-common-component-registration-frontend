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
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.SubscriptionServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{
  SubscriptionRequest,
  SubscriptionResponse
}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.externalservices.ExternalServicesConfig.{etmpFormBundleId, Host, Port}
import util.externalservices.{AuditService, SubscriptionService}

class SubscriptionServiceConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.host"              -> Host,
        "microservice.services.eori-common-component-hods-proxy.port"              -> Port,
        "microservice.services.eori-common-component-hods-proxy.subscribe.context" -> "subscribe",
        "auditing.enabled"                                                         -> true,
        "auditing.consumer.baseUri.host"                                           -> Host,
        "auditing.consumer.baseUri.port"                                           -> Port
      )
    )
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  private lazy val subscriptionServiceConnector = app.injector.instanceOf[SubscriptionServiceConnector]

  val expectedPostUrl: String = "/subscribe"

  val EORI: String = "ZZZ1ZZZZ23ZZZZZZZ"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  val serviceRequestJson: JsValue =
    Json.parse(s"""
         |{
         |  "subscriptionCreateRequest": {
         |    "requestCommon": {
         |       "regime": "CDS",
         |       "receiptDate": "2016-08-18T14:00:05Z",
         |       "acknowledgementReference": "4482baa81c844d23a8db3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "SAFE": "012345678900000000000000000000000000000000",
         |      "CDSFullName": "John Doe",
         |      "CDSEstablishmentAddress": {
         |        "streetAndNumber": "Line 1",
         |        "city": "city name",
         |        "postalCode": "SE28 1AA",
         |        "countryCode": "ZZ"
         |      },
         |    "dateOfEstablishment": "1963-05-01"
         |    }
         |  }
         |}
    """.stripMargin)

  val serviceSubscriptionGenerateResponseJson: JsValue =
    Json.parse(s"""
         |{
         |  "subscriptionCreateResponse": {
         |    "responseCommon": {
         |      "status": "OK",
         |      "processingDate": "2016-08-18T14:01:05Z",
         |      "returnParameters": [
         |        {
         |          "paramName": "ETMPFORMBUNDLENUMBER",
         |          "paramValue": "$etmpFormBundleId"
         |        },
         |        {
         |          "paramName": "POSITION",
         |          "paramValue": "GENERATE"
         |        }
         |      ]
         |    },
         |    "responseDetail": {
         |      "EORINo": "ZZZ1ZZZZ23ZZZZZZZ"
         |    }
         |  }
         |}
      """.stripMargin)

  val serviceSubscriptionLinkResponseJson: JsValue =
    Json.parse(s"""
         |{
         |  "subscriptionCreateResponse": {
         |    "responseCommon": {
         |      "status": "OK",
         |      "processingDate": "2016-08-18T14:01:05Z",
         |      "returnParameters": [
         |        {
         |          "paramName": "ETMPFORMBUNDLENUMBER",
         |          "paramValue": "$etmpFormBundleId"
         |        },
         |        {
         |          "paramName": "POSITION",
         |          "paramValue": "LINK"
         |        }
         |      ]
         |    },
         |    "responseDetail": {
         |      "EORINo": "ZZZ1ZZZZ23ZZZZZZZ"
         |    }
         |  }
         |}
      """.stripMargin)

  val serviceSubscriptionPendingResponseJson: JsValue =
    Json.parse(s"""
         |{
         |  "subscriptionCreateResponse": {
         |    "responseCommon": {
         |      "status": "OK",
         |      "processingDate": "2016-08-18T14:01:05Z",
         |      "returnParameters": [
         |        {
         |          "paramName": "ETMPFORMBUNDLENUMBER",
         |          "paramValue": "$etmpFormBundleId"
         |        },
         |        {
         |          "paramName": "POSITION",
         |          "paramValue": "WORKLIST"
         |        }
         |      ]
         |    }
         |  }
         |}
      """.stripMargin)

  val serviceSubscriptionFailedResponseJson: JsValue =
    Json.parse(s"""
         |{
         |  "subscriptionCreateResponse": {
         |    "responseCommon": {
         |      "status": "OK",
         |      "statusText": "068 - Subscription already in-progress or active",
         |      "processingDate": "2016-08-18T14:01:05Z",
         |      "returnParameters": [
         |        {
         |          "paramName": "POSITION",
         |          "paramValue": "FAIL"
         |        }
         |      ]
         |    }
         |  }
         |}
      """.stripMargin)

  val subscribe400ErrorResponse: JsValue = Json.parse("""
      |{
      |  "errorDetail": {
      |    "timestamp": "2014­01­19T11:31:47Z",
      |    "correlationId": "f05uigd6­02f7­4d3f­942e­904365e8cde5",
      |    "errorCode": "400",
      |    "errorMessage": " REGIME missing or invalid",
      |    "source": "Back End",
      |    "sourceFaultDetail": {
      |      "detail": [
      |        "001 ­ REGIME missing or invalid"
      |      ]
      |    }
      |  }
      |}
    """.stripMargin)

  val subscribe500ErrorResponse: JsValue = Json.parse("""
      |{
      |  "errorDetail": {
      |    "timestamp": "2016­09­08T10:55:32.766+0100",
      |    "correlationId": "392a2c98­1fa2­4ef3­a0db-2a68b739eb20",
      |    "errorCode": "500",
      |    "errorMessage": "Send timeout",
      |    "source": "ct­api",
      |    "sourceFaultDetail": {
      |      "detail": [
      |        "101504 ­ Send timeout"
      |      ]
      |    }
      |  }
      |}
    """.stripMargin)

  "subscriptionServiceConnector" should {

    "return subscription generate status successful response when subscription service returns 200 and make 2 AuditWrites" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        serviceSubscriptionGenerateResponseJson.toString()
      )
      await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest])) must be(
        serviceSubscriptionGenerateResponseJson.as[SubscriptionResponse]
      )
      eventually(AuditService.verifyXAuditWrite(1))
    }

    "return subscription link status successful response when subscription service returns 200" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        serviceSubscriptionLinkResponseJson.toString()
      )
      await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest])) must be(
        serviceSubscriptionLinkResponseJson.as[SubscriptionResponse]
      )
    }

    "return subscription pending status successful response when subscription service returns 200" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        serviceSubscriptionPendingResponseJson.toString()
      )
      await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest])) must be(
        serviceSubscriptionPendingResponseJson.as[SubscriptionResponse]
      )
    }

    "return subscription failed status response when subscription service returns 200 and response has 'FAIL' as param value" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        serviceSubscriptionFailedResponseJson.toString()
      )
      await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest])) must be(
        serviceSubscriptionFailedResponseJson.as[SubscriptionResponse]
      )
    }

    "return Exception when Subscription service returns a downstream 500 response (Internal Service Error)" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        subscribe500ErrorResponse.toString(),
        INTERNAL_SERVER_ERROR
      )
      val caught = intercept[UpstreamErrorResponse] {
        await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest]))
      }

      caught.statusCode mustBe 500
      caught.message must include(s"Response body: '$subscribe500ErrorResponse'")
      eventually(AuditService.verifyXAuditWrite(0))
    }

    "return Exception when Subscription service fails with 4xx (any 4xx response apart from 400)" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        "Forbidden",
        FORBIDDEN
      )
      val caught = intercept[UpstreamErrorResponse] {
        await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest]))
      }

      caught.statusCode mustBe 403
      caught.message must include("Response body: 'Forbidden'")
      eventually(AuditService.verifyXAuditWrite(0))
    }

    "return Exception when Subscription service returns a 400 response" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        subscribe400ErrorResponse.toString(),
        BAD_REQUEST
      )
      val caught = intercept[UpstreamErrorResponse] {
        await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest]))
      }
      caught.statusCode mustBe 400
      caught.message must include(s"Response body: '$subscribe400ErrorResponse'")
      eventually(AuditService.verifyXAuditWrite(0))
    }

    "audit a successful request" in {
      SubscriptionService.returnResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        serviceSubscriptionGenerateResponseJson.toString()
      )
      await(subscriptionServiceConnector.subscribe(serviceRequestJson.as[SubscriptionRequest]))

      eventually(AuditService.verifyXAuditWrite(1))
    }
  }
}
