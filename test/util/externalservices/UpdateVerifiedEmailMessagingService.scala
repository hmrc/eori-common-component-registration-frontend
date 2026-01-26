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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status.OK

object UpdateVerifiedEmailMessagingService {

  val validResponse: String =
    s"""{
       |  "updateVerifiedEmailResponse": {
       |    "responseCommon": {
       |      "status": "OK",
       |      "processingDate": "2016-08-17T19:33:47Z",
       |      "returnParameters": [
       |        {
       |          "paramName": "name",
       |          "paramValue": "value"
       |        }
       |      ]
       |    }
       |  }
       |}
       | """.stripMargin

  def returnTheResponseWhenReceiveRequest(url: String, response: String): Unit =
    stubTheResponse(url, response, OK)

  def stubTheResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      put(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
