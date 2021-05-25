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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.NotifyRcmConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.NotifyRcmRequest
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.NotifyRcmStubService

class NotifyRcmConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val email = "a@b.com"

  private val serviceName = "Some Service"

  private val eori = "GBXXXXXXXXX0000"

  private val name = "FullName"

  val expectedPostUrl = "/notify/rcm"

  private val timestamp = "timestamp"

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

  private lazy val notifyRcmConnector = app.injector.instanceOf[NotifyRcmConnector]

  private val serviceRequestJson =
    Json.parse(s"""
        | {
        |        "timestamp": "$timestamp",
        |        "name": "$name",
        |        "eori": "$eori",
        |        "email": "$email",
        |        "serviceName": "$serviceName"
        |    }
      """.stripMargin)

  private val request = serviceRequestJson.as[NotifyRcmRequest]

  before {
    resetMockServer()
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "NotifyRcmConnector" should {
    "call notifyRCM endpoint with correct url and payload" in {
      NotifyRcmStubService.returnRcmEndpointWhenReceiveRequest(expectedPostUrl, serviceRequestJson.toString, NO_CONTENT)
      scala.concurrent.Await.ready(notifyRcmConnector.notifyRCM(request), defaultTimeout)
      WireMock.verify(
        postRequestedFor(urlEqualTo(expectedPostUrl))
          .withRequestBody(equalToJson(serviceRequestJson.toString))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withHeader(HeaderNames.ACCEPT, equalTo("application/vnd.hmrc.1.0+json"))
      )
    }

    "return successful future when notifyRCM endpoint returns 204" in {
      NotifyRcmStubService.returnRcmEndpointWhenReceiveRequest(expectedPostUrl, serviceRequestJson.toString, NO_CONTENT)
      notifyRcmConnector.notifyRCM(request).futureValue mustBe ((): Unit)
    }

    "return a failed future when notifyRCM endpoint returns 400" in {
      NotifyRcmStubService.returnRcmEndpointWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        BAD_REQUEST
      )

      a[BadRequestException] should be thrownBy {
        await(notifyRcmConnector.notifyRCM(request))
      }
    }
  }
}
