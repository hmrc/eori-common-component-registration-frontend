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

package integration

import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import com.github.tomakehurst.wiremock.http.{Request, Response}
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.HandleSubscriptionConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.HandleSubscriptionRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.{AuditService, HandleSubscriptionService}

object HandleSubscriptionConnectorSpec {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def requestReceived(inRequest: Request, inResponse: Response): Unit = {
    logger.info("Test logging")
    logger.info(s"WireMock request at URL: ${inRequest.getAbsoluteUrl}")
    logger.info(s"WireMock request headers: ${inRequest.getAbsoluteUrl}")
    logger.info(s"WireMock response body: ${inRequest.getAbsoluteUrl}")

    println("Test logging")
    println(s"WireMock request at URL: ${inRequest.getAbsoluteUrl}")
    println(s"WireMock request headers: ${inRequest.getHeaders}")
    println(s"WireMock response body: ${Json.prettyPrint(Json.parse(inRequest.getBodyAsString))}")
  }

}

class HandleSubscriptionConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val formBundleId = "bundle-id"

  private val sapNumber = "sap-number"

  private val safeId = "safe-id"

  val expectedPostUrl = "/handle-subscription"

  private val emailVerificationTimestamp = "timestamp"

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.handle-subscription.host" -> Host,
        "microservice.services.handle-subscription.port" -> Port,
        "auditing.enabled"                               -> true,
        "auditing.consumer.baseUri.host"                 -> Host,
        "auditing.consumer.baseUri.port"                 -> Port
      )
    )
    .build()

  private lazy val handleSubscriptionConnector = app.injector.instanceOf[HandleSubscriptionConnector]

  private val serviceRequestJson =
    Json.parse(s"""
        | {
        |        "formBundleId": "$formBundleId",
        |        "recipientDetails": {
        |            "journey" : "Register",
        |            "enrolmentKey" : "HMRC-ATAR-ORG",
        |            "serviceName" : "Advance Tariff Rulings",
        |            "recipientFullName": "John Doe",
        |            "recipientEmailAddress": "john.doe@example.com",
        |            "orgName": "orgName",
        |            "completionDate": "9 July 2018"
        |        },
        |        "sapNumber": "$sapNumber",
        |        "emailVerificationTimestamp": "$emailVerificationTimestamp",
        |        "safeId": "$safeId"
        |    }
      """.stripMargin)

  private val handleSubscriptionRequest = serviceRequestJson.as[HandleSubscriptionRequest]

  private val logger = LoggerFactory.getLogger(this.getClass)

  def requestReceived(inRequest: Request, inResponse: Response): Unit = {
    logger.asInstanceOf[ch.qos.logback.classic.Logger].setLevel(Level.INFO) // Comment out this line to avoid logging
    logger.info("Test logging")
    logger.info(s"WireMock request at URL: ${inRequest.getAbsoluteUrl}")
    logger.info(s"WireMock request headers: ${inRequest.getHeaders}")
    logger.info(s"WireMock response body: ${Json.prettyPrint(Json.parse(inRequest.getBodyAsString))}}")
    logger.info(s"WireMock response headers: ${inRequest.getHeaders}")
  }

  before {
    resetMockServer()
    AuditService.stubAuditService()
    wireMockServer.addMockServiceRequestListener(requestReceived)
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "handleSubscriptionConnector" should {
    "call handle subscription endpoint with correct url and payload" in {
      HandleSubscriptionService.returnHandleSubscriptionResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )
      scala.concurrent.Await.ready(handleSubscriptionConnector.call(handleSubscriptionRequest), defaultTimeout)
      WireMock.verify(
        postRequestedFor(urlEqualTo(expectedPostUrl))
          .withRequestBody(equalToJson(serviceRequestJson.toString))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withHeader(HeaderNames.ACCEPT, equalTo("application/vnd.hmrc.1.0+json"))
      )
    }

    "return successful future when handle subscription endpoint returns 204" in {
      HandleSubscriptionService.returnHandleSubscriptionResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )
      handleSubscriptionConnector.call(handleSubscriptionRequest).futureValue mustBe ((): Unit)
    }

    "return a failed future when handle subscription endpoint returns 400" in {
      HandleSubscriptionService.returnHandleSubscriptionResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        BAD_REQUEST
      )

      a[BadRequestException] should be thrownBy {
        await(handleSubscriptionConnector.call(handleSubscriptionRequest))
      }
    }
  }
}
