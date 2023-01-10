/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status.{NOT_FOUND, OK}

object VatControlListMessagingService {

  private val vatEndPoint = s"/vat-known-facts-control-list?vrn=123456789"

  private val responseWithOk: JsValue =
    Json.parse("""
        |{
        |      "traderName": "John Doe",
        |      "postcode": "SE28 1AA",
        |      "dateOfReg": "2017-01-01",
        |      "lastReturnMonthPeriod": "MAR",
        |      "lastNetDue": 10000.02,
        |      "controlListInformation": "10000000000000000000000000000000"
        |}
      """.stripMargin)

  def returnTheVatControlListResponseOK(): Unit =
    stubTheVatControlListResponse(vatEndPoint, responseWithOk.toString(), OK)

  def stubTheVatControlListResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def returnNotFoundVatControlListResponse(url: String, response: String): Unit =
    stubTheVatControlListResponse(url, response, NOT_FOUND)

}
