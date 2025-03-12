/*
 * Copyright 2025 HM Revenue & Customs
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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, LENGTH_REQUIRED, SERVICE_UNAVAILABLE}
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status._

object GetVatInformationMessagingService {

  private val vatEndPoint = s"/vat-customer-information/123456789"

  private val responseWithOk: JsValue =
    Json.parse("""
        |{
        |      "effectiveRegistrationDate": "2021-01-31",
        |      "postCode": "SE28 1AA"
        |}
      """.stripMargin)

  private val responseWithNotFound: String =
    Json.parse("""{
        |  "failures": {
        |    "code": "NOT_FOUND",
        |    "reason": "The back end has indicated that No subscription can be found."
        |  }
        |}""".stripMargin).toString()

  private val responseWithBadRequest: String =
    Json.parse("""{
        |  "failures": {
        |    "code": "INVALID_IDVALUE",
        |    "reason": "Submission has not passed validation. Invalid path parameter idValue."
        |  }
        |}""".stripMargin).toString()

  private val responseWithForbidden: String =
    Json.parse(
      """{
        |  "failures": {
        |    "code": "MIGRATION",
        |    "reason": "The back end has indicated that a migration is in progress for this identification number"
        |  }
        |}""".stripMargin
    ).toString()

  private val responseWithInternalServerError: String =
    Json.parse("""{
        |  "failures": {
        |    "code": "SERVER_ERROR.",
        |    "reason": "IF is currently experiencing problems that require live service intervention."
        |  }
        |}""".stripMargin).toString()

  private val responseWithBadGateway: String =
    Json.parse("""{
        |  "failures": [
        |    {
        |      "code": "BAD_GATEWAY",
        |      "reason": "Dependent systems are currently not responding."
        |    }
        |  ]
        |}""".stripMargin).toString()

  private val responseWithServiceUnavailable: String =
    Json.parse("""{
        |  "failures": [
        |    {
        |      "code": "SERVICE_UNAVAILABLE",
        |      "reason": "Dependent systems are currently not responding."
        |    }
        |  ]
        |}""".stripMargin).toString()

  def returnTheVatCustomerInformationResponseOK(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithOk.toString(), OK)

  def stubGetVatInformationResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def returnTheVatCustomerInformationResponseNotFound(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithNotFound, NOT_FOUND)

  def returnTheVatCustomerInformationResponseBadRequest(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithBadRequest, BAD_REQUEST)

  def returnTheVatCustomerInformationResponseForbidden(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithForbidden, FORBIDDEN)

  def returnTheVatCustomerInformationResponseInternalServerError(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithInternalServerError, INTERNAL_SERVER_ERROR)

  def returnTheVatCustomerInformationResponseBadGateway(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithBadGateway, BAD_GATEWAY)

  def returnTheVatCustomerInformationResponseServiceUnavailable(): Unit =
    stubGetVatInformationResponse(vatEndPoint, responseWithServiceUnavailable, SERVICE_UNAVAILABLE)

  def returnTheVatCustomerInformationResponseUnexpectedStatus(): Unit =
    stubGetVatInformationResponse(vatEndPoint, "", LENGTH_REQUIRED)

}
