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

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  SUB09SubscriptionDisplayConnector,
  ServiceUnavailableResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionDisplayResponseHolder
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.http._
import util.externalservices.ExternalServicesConfig._
import util.externalservices.SubscriptionDisplayMessagingService

class SUB09SubscriptionDisplayConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
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
    .build()

  private lazy val connector                  = app.injector.instanceOf[SUB09SubscriptionDisplayConnector]
  private val requestTaxPayerId               = "GBE9XSDF10BCKEYAX"
  private val requestEori                     = "GB083456789000"
  private val requestAcknowledgementReference = "1234567890ABCDEFG"

  private val reqTaxPayerId = Seq(
    ("regime", "CDS"),
    ("taxPayerID", requestTaxPayerId),
    ("acknowledgementReference", requestAcknowledgementReference)
  )

  private val reqEori =
    Seq(("regime", "CDS"), ("EORI", requestEori), ("acknowledgementReference", requestAcknowledgementReference))

  private val expectedResponse = Json
    .parse(SubscriptionDisplayMessagingService.validResponse(typeOfLegalEntity = "0001"))
    .as[SubscriptionDisplayResponseHolder]
    .subscriptionDisplayResponse

  implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    resetMockServer()
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "SubscriptionDisplay SUB09" should {
    "return successful response with OK status when subscription display service returns 200, for EoriNo and journey is Subscription" in {

      SubscriptionDisplayMessagingService.returnSubscriptionDisplayWhenReceiveRequest(
        requestEori,
        requestAcknowledgementReference,
        Journey.Subscribe
      )
      await(connector.subscriptionDisplay(reqEori)) mustBe Right(expectedResponse)
    }

    "return successful response with OK status when subscription display service returns 200, for taxPayerId and journey is Register" in {

      SubscriptionDisplayMessagingService.returnSubscriptionDisplayWhenReceiveRequest(
        requestTaxPayerId,
        requestAcknowledgementReference,
        Journey.Register
      )
      await(connector.subscriptionDisplay(reqTaxPayerId)) mustBe Right(expectedResponse)
    }

    "return Service Unavailable Response when subscription display service returns an exception" in {

      SubscriptionDisplayMessagingService.returnSubscriptionDisplayWhenReceiveRequest(
        requestTaxPayerId,
        requestAcknowledgementReference,
        Journey.Register,
        returnedStatus = SERVICE_UNAVAILABLE
      )
      await(connector.subscriptionDisplay(reqTaxPayerId)) mustBe Left(ServiceUnavailableResponse)
    }
  }
}
