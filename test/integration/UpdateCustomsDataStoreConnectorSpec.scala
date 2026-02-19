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
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.*
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.UpdateCustomsDataStoreConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.CustomsDataStoreRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.{AuditService, CustomsDataStoreStubService}

import scala.concurrent.ExecutionContext

class UpdateCustomsDataStoreConnectorSpec extends IntegrationTestsSpec with ScalaFutures with LogCapturing {
  implicit val ex: ExecutionContext = ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val email = "a@example.com"

  private val eori = "GBXXXXXXXXX0000"

  val expectedPostUrl = "/customs/update/datastore"

  private val timestamp = "timestamp"

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

  private lazy val customsDataStoreConnector = app.injector.instanceOf[UpdateCustomsDataStoreConnector]

  val connectorLogger: Logger =
    LoggerFactory
      .getLogger(classOf[UpdateCustomsDataStoreConnector])
      .asInstanceOf[Logger]

  private val serviceRequestJson =
    Json.parse(s"""
        | {
        |        "timestamp": "$timestamp",
        |        "eori": "$eori",
        |        "address": "$email"
        |    }
      """.stripMargin)

  private val request = serviceRequestJson.as[CustomsDataStoreRequest]

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  val expectedAuditEventJson: JsValue =
    Json.parse("""{
   |  "auditSource" : "eori-common-component-registration-frontend",
   |  "auditType" : "CustomsDataStoreUpdate",
   |  "tags" : {
   |    "clientIP" : "-",
   |    "path" : "http://localhost:11111/customs/update/datastore",
   |    "X-Session-ID" : "-",
   |    "Akamai-Reputation" : "-",
   |    "X-Request-ID" : "-",
   |    "deviceID" : "-",
   |    "clientPort" : "-",
   |    "transactionName" : "customs-data-store"
   |  },
   |  "detail" : {
   |      "eori" : "GBXXXXXXXXX0000",
   |      "address" : "a@example.com",
   |      "timestamp" : "timestamp",
   |      "status" : "204"
   |  },
   |  "dataPipeline" : {
   |    "redaction" : {
   |      "containsRedactions" : false
   |    }
   |  },
   |  "metadata" : {
   |    "metricsKey" : null
   |  }
   |}""".stripMargin)

  "CustomsDataStoreConnector" should {
    "call update email endpoint with correct url and payload" in {
      CustomsDataStoreStubService.returnCustomsDataStoreEndpointWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )
      val res = customsDataStoreConnector.updateCustomsDataStore(request)

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        whenReady(res) { _ =>
          eventually {
            events should not be empty
            events.exists(_.getLevel.levelStr == "INFO") shouldBe true
          }

          WireMock.verify(
            postRequestedFor(urlEqualTo(expectedPostUrl))
              .withRequestBody(equalToJson(serviceRequestJson.toString))
              .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
              .withHeader(HeaderNames.ACCEPT, equalTo("application/vnd.hmrc.1.0+json"))
          )
        }
      }
    }

    "call audit endpoint with correct audit event" in {
      CustomsDataStoreStubService.returnCustomsDataStoreEndpointWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )

      val res = customsDataStoreConnector.updateCustomsDataStore(request)

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        whenReady(res) { _ =>
          eventually {
            events should not be empty
            events.exists(_.getLevel.levelStr == "INFO") shouldBe true
          }

          eventually(AuditService.verifyXAuditWriteWithBody(expectedAuditEventJson))
        }
      }

    }

    "return successful future when update email endpoint returns 204" in {
      CustomsDataStoreStubService.returnCustomsDataStoreEndpointWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        NO_CONTENT
      )

      val res = customsDataStoreConnector.updateCustomsDataStore(request)
      withCaptureOfLoggingFrom(connectorLogger) { events =>
        whenReady(res) { result =>
          eventually {
            events should not be empty
            events.exists(_.getLevel.levelStr == "INFO") shouldBe true
          }

          result mustBe ()
        }
      }
    }

    "return a failed future when update email endpoint returns 500" in {
      CustomsDataStoreStubService.returnCustomsDataStoreEndpointWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        INTERNAL_SERVER_ERROR
      )

      val res = customsDataStoreConnector.updateCustomsDataStore(request)

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        val ex = await(res.failed)

        eventually {
          events should not be empty
          events.exists(_.getLevel.levelStr == "WARN") shouldBe true
        }

      val ex = await(res.failed)
      ex mustBe a[BadRequestException]
    }
  }
}