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

package unit.connector

import base.UnitSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{
  AddressLookup,
  AddressLookupFailure,
  AddressLookupSuccess
}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import unit.connector.AddressLookupConnectorSpec.{jsonResponseWithOneResult, jsonResponseWithTwoResults}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AddressLookupConnectorSpec
    extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with IntegrationPatience {

  private val httpClient = mock[HttpClient]
  private val appConfig  = mock[AppConfig]

  private val connector = new AddressLookupConnector(httpClient, appConfig)(global)

  implicit val hc = HeaderCarrier()

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(appConfig.addressLookup).thenReturn("http://localhost:6754/v2/uk/addresses")
  }

  override protected def afterEach(): Unit = {
    reset(httpClient, appConfig)

    super.afterEach()
  }

  "Address Lookup Connector" should {

    "build a correct url with replacing space with plus" when {

      "there is only postcode" in {

        when(httpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, "[]")))

        val postcode = "AA11 1AA"

        val expectedResponse = AddressLookupSuccess(Seq.empty)
        val expectedUrl      = "http://localhost:6754/v2/uk/addresses?postcode=AA11+1AA"

        val urlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

        val result = connector.lookup(postcode, None)(hc)

        result.futureValue shouldBe expectedResponse

        verify(httpClient).GET[HttpResponse](urlCaptor.capture(), any(), any())(any(), any(), any())

        urlCaptor.getValue shouldBe expectedUrl
      }

      "postcode and line 1 is specified" in {

        when(httpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, "[]")))

        val postcode = "AA11 1AA"
        val line1    = "Address Line 1"

        val expectedResponse = AddressLookupSuccess(Seq.empty)
        val expectedUrl      = "http://localhost:6754/v2/uk/addresses?postcode=AA11+1AA&line1=Address+Line+1"

        val urlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

        val result = connector.lookup(postcode, Some(line1))(hc)

        result.futureValue shouldBe expectedResponse

        verify(httpClient).GET[HttpResponse](urlCaptor.capture(), any(), any())(any(), any(), any())

        urlCaptor.getValue shouldBe expectedUrl
      }
    }

    "return Address Lookup Success" when {

      "address lookup returns list of multiple addresses" in {

        val addressLookupResponse = HttpResponse(status = 200, json = jsonResponseWithTwoResults, headers = Map.empty)

        when(httpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(addressLookupResponse))

        val postcode = "AA11 1AA"

        val expectedFirstAddress =
          AddressLookup("First Address Line 1, First Address Line 2", "First town", "AA11 1AA", "GB")
        val expectedSecondAddress =
          AddressLookup("Second Address Line 1, Second Address Line 2", "Second town", "AA11 1AA", "GB")
        val expectedResponse = AddressLookupSuccess(Seq(expectedFirstAddress, expectedSecondAddress))

        val result = connector.lookup(postcode, None)(hc)

        result.futureValue shouldBe expectedResponse
      }

      "address lookup returns only one address" in {

        val addressLookupResponse = HttpResponse(status = 200, json = jsonResponseWithOneResult, headers = Map.empty)

        when(httpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(addressLookupResponse))

        val postcode = "AA11 1AA"

        val expectedAddress  = AddressLookup("Address Line 1, Address Line 2", "Town", "AA11 1AA", "GB")
        val expectedResponse = AddressLookupSuccess(Seq(expectedAddress))

        val result = connector.lookup(postcode, None)(hc)

        result.futureValue shouldBe expectedResponse
      }

      "address lookup didn't return any addresses" in {

        when(httpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, "[]")))

        val postcode = "AA11 1AA"

        val expectedResponse = AddressLookupSuccess(Seq.empty)

        val result = connector.lookup(postcode, None)(hc)

        result.futureValue shouldBe expectedResponse
      }
    }

    "return Address Lookup Failure" when {

      "address lookup return different status than OK (200)" in {

        when(httpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(500, "Internal Server Error")))

        val postcode = "AA11 1AA"

        val result = connector.lookup(postcode, None)(hc)

        result.futureValue shouldBe AddressLookupFailure
      }
    }
  }
}

object AddressLookupConnectorSpec {

  val jsonResponseWithTwoResults = Json.parse("""
      |[
      |    {
      |        "id": "123",
      |        "uprn": 123,
      |        "address": {
      |            "lines": [
      |                "First Address Line 1",
      |                "First Address Line 2"
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
      |                "Second Address Line 2"
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

  val jsonResponseWithOneResult = Json.parse("""
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
