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
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.MimeTypes
import play.mvc.Http.Status.*
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{EnrolmentStoreProxyConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, EnrolmentStoreProxyResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import util.externalservices.EnrolmentStoreProxyService
import util.externalservices.ExternalServicesConfig.*

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class EnrolmentStoreProxyConnectorSpec extends IntegrationTestsSpec with ScalaFutures with MockitoSugar with LogCapturing {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.enrolment-store-proxy.host"    -> Host,
        "microservice.services.enrolment-store-proxy.port"    -> Port,
        "microservice.services.enrolment-store-proxy.context" -> "enrolment-store-proxy",
        "auditing.enabled"                                    -> false,
        "auditing.consumer.baseUri.host"                      -> Host,
        "auditing.consumer.baseUri.port"                      -> Port
      )
    )
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  private lazy val enrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]
  private val groupId = "2e4589d9-484c-468a-8099-02a06fb1cd8c"

  private val expectedGetUrl =
    s"/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments?type=principal"

  val connectorLogger: Logger =
    LoggerFactory
      .getLogger(classOf[EnrolmentStoreProxyConnector])
      .asInstanceOf[Logger]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val testEC: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  val responseWithOk: JsValue =
    Json.parse {
      """{
        |	"startRecord": 1,
        |	"totalRecords": 2,
        |	"enrolments": [{
        |			"service": "HMRC-CUS-ORG",
        |			"state": "NotYetActivated",
        |			"friendlyName": "My First Client's SA Enrolment",
        |			"enrolmentDate": "2018-10-05T14:48:00.000Z",
        |			"failedActivationCount": 1,
        |			"activationDate": "2018-10-13T17:36:00.000Z",
        |			"identifiers": [{
        |				"key": "UTR",
        |				"value": "2108834503"
        |			}]
        |		},
        |		{
        |			"service": "HMRC-CUS-ORG",
        |			"state": "Activated",
        |			"friendlyName": "My Second Client's SA Enrolment",
        |			"enrolmentDate": "2017-06-25T12:24:00.000Z",
        |			"failedActivationCount": 1,
        |			"activationDate": "2017-07-01T09:52:00.000Z",
        |			"identifiers": [{
        |				"key": "UTR",
        |				"value": "2234567890"
        |			}]
        |		}
        |	]
        |}""".stripMargin
    }

  before {
    resetMockServer()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  "EnrolmentStoreProxy" should {
    "return successful response with OK status when Enrolment Store Proxy returns 200" in {
      EnrolmentStoreProxyService.returnEnrolmentStoreProxyResponseOk("2e4589d9-484c-468a-8099-02a06fb1cd8c")
      val res = enrolmentStoreProxyConnector.getEnrolmentByGroupId(groupId).value

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
        }

        result.map(res => res must be(responseWithOk.as[EnrolmentStoreProxyResponse]))
      }
    }

    "return No Content status when no data is returned in response" in {
      EnrolmentStoreProxyService.stubTheEnrolmentStoreProxyResponse(expectedGetUrl, "", NO_CONTENT)
      val res = enrolmentStoreProxyConnector.getEnrolmentByGroupId(groupId).value

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
        }

        result.map(res => res mustBe EnrolmentStoreProxyResponse(enrolments = List.empty[EnrolmentResponse]))

      }
    }

    "return left when Service unavailable" in {
      EnrolmentStoreProxyService.stubTheEnrolmentStoreProxyResponse(expectedGetUrl, "", SERVICE_UNAVAILABLE)

      val res = enrolmentStoreProxyConnector.getEnrolmentByGroupId(groupId).value

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "WARN") shouldBe true
        }

        result mustBe Left(ResponseError(SERVICE_UNAVAILABLE, "Enrolment Store Proxy Response : }"))

      }
    }

    "return left when 4xx status code is received" in {
      EnrolmentStoreProxyService.stubTheEnrolmentStoreProxyResponse(expectedGetUrl, "", BAD_REQUEST)

      val res = enrolmentStoreProxyConnector.getEnrolmentByGroupId(groupId).value

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "WARN") shouldBe true
        }

        result mustBe Left(ResponseError(BAD_REQUEST, "Enrolment Store Proxy Response : }"))
      }
    }
  }
}
