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

package unit.connectors

import base.UnitSpec
import ch.qos.logback.classic.Logger
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.LoggerFactory
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{AddressLookupFailure, AddressLookupSuccess}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import unit.connectors.AddressLookupConnectorSpec.{jsonResponseWithOneResult, jsonResponseWithTwoResults}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressLookupConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with IntegrationPatience with LogCapturing {

  private val httpClient = mock[HttpClientV2]
  private val appConfig = mock[AppConfig]

  private val connector = new AddressLookupConnector(httpClient, appConfig)

  val connectorLogger: Logger =
    LoggerFactory
      .getLogger(classOf[AddressLookupConnector])
      .asInstanceOf[Logger]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(appConfig.addressLookup).thenReturn("http://localhost:6754/lookup")
  }

  override protected def afterEach(): Unit = {
    reset(httpClient)
    reset(appConfig)
    super.afterEach()
  }

  "Address Lookup Connector" should {

    "return Address Lookup Success" when {

      "address lookup returns list of multiple addresses" in {

        val addressLookupResponse = HttpResponse(status = 200, json = jsonResponseWithTwoResults, headers = Map.empty)

        val mockRequestBuilder = mock[RequestBuilder]
        when(httpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(addressLookupResponse))

        val postcode = "AA11 1AA"

        val expectedFirstAddress =
          Address("First Address Line 1", None, None, Some("First town"), Some("AA11 1AA"), "GB")
        val expectedSecondAddress =
          Address(
            "Second Address Line 1",
            Some("Second Address Line 2"),
            Some("Second Address Line 3"),
            Some("Second town"),
            Some("AA11 1AA"),
            "GB"
          )
        val expectedResponse = AddressLookupSuccess(Seq(expectedFirstAddress, expectedSecondAddress))

        withCaptureOfLoggingFrom(connectorLogger) { events =>
          val res = connector.lookup(postcode, None)(hc)
          whenReady(res) { result =>
            eventually(timeout(Span(30, Seconds))) {
              events should not be empty
              events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
            }

            result shouldBe expectedResponse
          }
        }
      }

      "address lookup returns only one address" in {

        val addressLookupResponse = HttpResponse(status = 200, json = jsonResponseWithOneResult, headers = Map.empty)

        val mockRequestBuilder = mock[RequestBuilder]
        when(httpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(addressLookupResponse))

        val postcode = "AA11 1AA"

        val expectedAddress =
          Address("Address Line 1", Some("Address Line 2"), None, Some("Town"), Some("AA11 1AA"), "GB")
        val expectedResponse = AddressLookupSuccess(Seq(expectedAddress))

        withCaptureOfLoggingFrom(connectorLogger) { events =>
          val res = connector.lookup(postcode, None)(hc)
          whenReady(res) { result =>
            eventually(timeout(Span(30, Seconds))) {
              events should not be empty
              events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
            }

            result shouldBe expectedResponse
          }
        }
      }

      "address lookup didn't return any addresses" in {

        val mockRequestBuilder = mock[RequestBuilder]
        when(httpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "[]")))

        val postcode = "AA11 1AA"

        val expectedResponse = AddressLookupSuccess(Seq.empty)

        withCaptureOfLoggingFrom(connectorLogger) { events =>
          val res = connector.lookup(postcode, None)(hc)
          whenReady(res) { result =>
            eventually(timeout(Span(30, Seconds))) {
              events should not be empty
              events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
            }

            result shouldBe expectedResponse
          }
        }
      }
    }

    "return Address Lookup Failure" when {

      "address lookup return different status than OK (200)" in {

        val mockRequestBuilder = mock[RequestBuilder]
        when(httpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any, any, any)).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any, any))
          .thenReturn(Future.successful(HttpResponse(500, "Internal Server Error")))

        val postcode = "AA11 1AA"

        withCaptureOfLoggingFrom(connectorLogger) { events =>
          val res = connector.lookup(postcode, None)(hc)
          whenReady(res) { result =>
            eventually(timeout(Span(30, Seconds))) {
              events should not be empty
              events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
            }

            result shouldBe AddressLookupFailure
          }
        }
      }
    }
  }
}

object AddressLookupConnectorSpec {

  val jsonResponseWithTwoResults: JsValue = Json.parse("""
                                                         |[
                                                         |    {
                                                         |        "id": "123",
                                                         |        "uprn": 123,
                                                         |        "address": {
                                                         |            "lines": [
                                                         |                "First Address Line 1"
                                                         |            ],
                                                         |            "town": "First town",
                                                         |            "county": "County",
                                                         |            "postcode": "AA11 1AA",
                                                         |            "subdivision": {
                                                         |                "code": "GB-ENG",
                                                         |                "name": "England"
                                                         |            },
                                                         |            "country": {
                                                         |                "code": "UK",
                                                         |                "name": "United Kingdom"
                                                         |            }
                                                         |        },
                                                         |        "language": "en",
                                                         |        "localCustodian": {
                                                         |            "code": 121,
                                                         |            "name": "Name"
                                                         |        }
                                                         |    },
                                                         |    {
                                                         |        "id": "321",
                                                         |        "uprn": 321,
                                                         |        "address": {
                                                         |            "lines": [
                                                         |                "Second Address Line 1",
                                                         |                "Second Address Line 2",
                                                         |                "Second Address Line 3"
                                                         |            ],
                                                         |            "town": "Second town",
                                                         |            "county": "County",
                                                         |            "postcode": "AA11 1AA",
                                                         |            "subdivision": {
                                                         |                "code": "GB-ENG",
                                                         |                "name": "England"
                                                         |            },
                                                         |            "country": {
                                                         |                "code": "UK",
                                                         |                "name": "United Kingdom"
                                                         |            }
                                                         |        },
                                                         |        "language": "en",
                                                         |        "localCustodian": {
                                                         |            "code": 121,
                                                         |            "name": "Name"
                                                         |        }
                                                         |    }
                                                         |]""".stripMargin)

  val jsonResponseWithOneResult: JsValue = Json.parse("""
                                                        |[
                                                        |    {
                                                        |        "id": "123",
                                                        |        "uprn": 123,
                                                        |        "address": {
                                                        |            "lines": [
                                                        |                "Address Line 1",
                                                        |                "Address Line 2"
                                                        |            ],
                                                        |            "town": "Town",
                                                        |            "county": "County",
                                                        |            "postcode": "AA11 1AA",
                                                        |            "subdivision": {
                                                        |                "code": "GB-ENG",
                                                        |                "name": "England"
                                                        |            },
                                                        |            "country": {
                                                        |                "code": "UK",
                                                        |                "name": "United Kingdom"
                                                        |            }
                                                        |        },
                                                        |        "language": "en",
                                                        |        "localCustodian": {
                                                        |            "code": 121,
                                                        |            "name": "Name"
                                                        |        }
                                                        |    }
                                                        |]""".stripMargin)
}
