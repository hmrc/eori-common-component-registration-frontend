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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status.OK

object TaxEnrolmentsService {

  private val responseWithOk: JsValue =
    Json.parse("""[{
        |    "created": 1482329348256,
        |    "lastModified": 1482329348256,
        |    "credId": "d8474a25-71b6-45ed-859e-77dd5f087be6",
        |    "serviceName": "516b9976-00fd-4da6-b59c-4d09054912bb",
        |    "identifiers": [{
        |        "key": "f52b4104-7e69-4bb8-baec-5aaf9897e849",
        |        "value": "97541e00-a712-452b-af21-0be4db0b7b1d"
        |    }, {
        |        "key": "d3e222b8-b9ff-4571-8f83-1a0fbc221195",
        |        "value": "547d7434-8c33-431d-bffa-35a7d1103c30"
        |    }],
        |    "callback": "url passed in by the subscriber service",
        |    "state": "PENDING",
        |    "etmpId": "da4053bf-2ea3-4cb8-bb9c-65b70252b656",
        |    "groupIdentifier": "c808798d-0d81-4a34-82c2-bbf13b3ac2fa"
        |}]""".stripMargin)

  private def endpoint(safeId: String) = s"/tax-enrolments/businesspartners/$safeId/subscriptions"

  def returnTheTaxEnrolmentsResponseOK(safeId: String): Unit =
    stubTheTaxEnrolmentsResponse(endpoint(safeId), responseWithOk.toString(), OK)

  def stubTheTaxEnrolmentsResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def returnEnrolmentResponseWhenReceiveRequest(url: String, request: String, status: Int): Unit =
    stubFor(
      put(urlEqualTo(url))
        .withRequestBody(equalToJson(request))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
