/*
 * Copyright 2026 HM Revenue & Customs
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

import ch.qos.logback.classic.Logger
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.shouldBe
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.*
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.HandleSubscriptionConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.HandleSubscriptionRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.{AuditService, HandleSubscriptionService}

class HandleSubscriptionConnectorSpec extends IntegrationTestsSpec with ScalaFutures with LogCapturing {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val formBundleId = "bundle-id"

  private val sapNumber = "sap-number"

  private val safeId = "safe-id"

  val expectedPostUrl = "/handle-subscription"

  private val emailVerificationTimestamp = "timestamp"

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
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

  private lazy val handleSubscriptionConnector = app.injector.instanceOf[HandleSubscriptionConnector]

  val connectorLogger: Logger =
    LoggerFactory
      .getLogger(classOf[HandleSubscriptionConnector])
      .asInstanceOf[Logger]

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

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  "handleSubscriptionConnector" should {
    "call handle subscription endpoint with correct url and payload" in {
      HandleSubscriptionService.returnHandleSubscriptionResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )

      val res = handleSubscriptionConnector.call(handleSubscriptionRequest)
      withCaptureOfLoggingFrom(connectorLogger) { events =>
        whenReady(res) { _ =>
          events
            .collectFirst { case event =>
              event.getLevel.levelStr shouldBe "DEBUG"
            }
            .getOrElse(fail("No log was captured"))

          WireMock.verify(
            postRequestedFor(urlEqualTo(expectedPostUrl))
              .withRequestBody(equalToJson(serviceRequestJson.toString))
              .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
              .withHeader(HeaderNames.ACCEPT, equalTo("application/vnd.hmrc.1.0+json"))
          )
        }
      }
    }

    "return successful future when handle subscription endpoint returns 204" in {
      HandleSubscriptionService.returnHandleSubscriptionResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )

      val res = handleSubscriptionConnector.call(handleSubscriptionRequest)
        whenReady(res) { result =>
          result mustBe ((): Unit)
      }
    }

    "return a failed future when handle subscription endpoint returns 400" in {
      HandleSubscriptionService.returnHandleSubscriptionResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        BAD_REQUEST
      )

      val res = handleSubscriptionConnector.call(handleSubscriptionRequest)

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        val ex = await(res.failed)

        events
          .collectFirst { case event =>
            event.getLevel.levelStr shouldBe "WARN"
          }
          .getOrElse(fail("No log was captured"))

        ex mustBe a[BadRequestException]
      }
    }
  }
}
