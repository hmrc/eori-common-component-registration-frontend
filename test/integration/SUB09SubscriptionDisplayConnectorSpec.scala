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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatest.time.{Seconds, Span}
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status.*
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{InvalidResponse, SUB09SubscriptionDisplayConnector, ServiceUnavailableResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.{SubscriptionDisplayResponse, SubscriptionDisplayResponseHolder}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import util.externalservices.ExternalServicesConfig.*
import util.externalservices.SubscriptionDisplayMessagingService

import java.time.temporal.ChronoUnit

class SUB09SubscriptionDisplayConnectorSpec extends IntegrationTestsSpec with ScalaFutures with LogCapturing {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.host"                         -> Host,
        "microservice.services.eori-common-component-hods-proxy.port"                         -> Port,
        "microservice.services.eori-common-component-hods-proxy.subscription-display.context" -> "subscription-display",
        "auditing.enabled"                                                                    -> false,
        "auditing.consumer.baseUri.host"                                                      -> Host,
        "auditing.consumer.baseUri.port"                                                      -> Port
      )
    )
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  private lazy val connector = app.injector.instanceOf[SUB09SubscriptionDisplayConnector]

  private val connectorLogger: Logger =
    LoggerFactory
      .getLogger(classOf[SUB09SubscriptionDisplayConnector])
      .asInstanceOf[Logger]
  private val requestTaxPayerId = "GBE9XSDF10BCKEYAX"
  private val requestAcknowledgementReference = "1234567890ABCDEFG"

  private val expectedResponse = Json
    .parse(SubscriptionDisplayMessagingService.validResponse(typeOfLegalEntity = "0001"))
    .as[SubscriptionDisplayResponseHolder]
    .subscriptionDisplayResponse

  implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    resetMockServer()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  "SubscriptionDisplay SUB09" should {

    "return successful response with OK status when subscription display service returns 200, for taxPayerId and journey is Register" in {

      SubscriptionDisplayMessagingService.returnSubscriptionDisplayWhenReceiveRequest(
        requestTaxPayerId,
        requestAcknowledgementReference
      )
      val res = connector.subscriptionDisplay(requestTaxPayerId, requestAcknowledgementReference)

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        whenReady(res) { result =>
          eventually(timeout(Span(30, Seconds))) {
            events should not be empty
            events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
          }

          result.map(truncateTimestamp) mustBe Right(truncateTimestamp(expectedResponse))
        }
      }
    }

    "return Service Unavailable Response when subscription display service returns an exception" in {

      SubscriptionDisplayMessagingService.returnSubscriptionDisplayWhenReceiveRequest(
        requestTaxPayerId,
        requestAcknowledgementReference,
        returnedStatus = SERVICE_UNAVAILABLE
      )
      val res = connector.subscriptionDisplay(requestTaxPayerId, requestAcknowledgementReference)

      whenReady(res) { result =>
        result.map(truncateTimestamp) mustBe Left(ServiceUnavailableResponse)
      }
    }

    "return InvalidResponse when subscription display service returns a failure" in {

      SubscriptionDisplayMessagingService.returnSubscriptionDisplayFailureWhenReceiveRequest(
        requestTaxPayerId,
        requestAcknowledgementReference
      )
      val res = connector.subscriptionDisplay(requestTaxPayerId, requestAcknowledgementReference)

      withCaptureOfLoggingFrom(connectorLogger) { events =>
        whenReady(res) { result =>
          eventually(timeout(Span(30, Seconds))) {
            events should not be empty
            events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
          }

          result.map(truncateTimestamp) mustBe Left(InvalidResponse)
        }
      }
    }
  }

  private def truncateTimestamp(r: SubscriptionDisplayResponse): SubscriptionDisplayResponse =
    r.copy(
      responseDetail = r.responseDetail.copy(
        contactInformation = r.responseDetail.contactInformation.map(ci =>
          ci.copy(
            emailVerificationTimestamp = Some(ci.emailVerificationTimestamp.get.truncatedTo(ChronoUnit.MINUTES))
          )
        )
      )
    )

}
