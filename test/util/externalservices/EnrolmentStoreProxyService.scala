/*
 * Copyright 2022 HM Revenue & Customs
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
import play.mvc.Http.Status._

object EnrolmentStoreProxyService {

  private val responseWithOk: JsValue =
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

  private def endpoint(groupId: String) =
    s"/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments?type=principal"

  def returnEnrolmentStoreProxyResponseOk(groupId: String): Unit =
    stubTheEnrolmentStoreProxyResponse(endpoint(groupId), responseWithOk.toString(), OK)

  def stubTheEnrolmentStoreProxyResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def stubTheEnrolmentStoreProxyPostResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
