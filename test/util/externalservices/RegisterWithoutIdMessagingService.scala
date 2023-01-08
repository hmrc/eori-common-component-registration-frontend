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
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status.OK

object RegisterWithoutIdMessagingService {

  val RegistrationPath: UrlPattern = urlMatching("/register-without-id")

  val AValidResponse: String =
    """
      |{
      |   "registerWithoutIDResponse":{
      |      "responseCommon":{
      |         "status":"OK",
      |         "processingDate":"2016-03-17T09:31:05Z",
      |         "returnParameters":[
      |            {
      |               "paramName":"SAP_NUMBER",
      |               "paramValue":"sapNumber-123"
      |            }
      |         ]
      |      },
      |      "responseDetail":{
      |         "SAFEID":"XE0000123456789",
      |         "ARN":"ZARN1234567"
      |      }
      |   }
      |}
    """.stripMargin

  def returnTheResponseWhenReceiveRequest(url: String, request: String, response: String): Unit =
    returnTheResponseWhenReceiveRequest(url, request, response, OK)

  def returnTheResponseWhenReceiveRequest(url: String, request: String, response: String, status: Int): Unit =
    stubFor(
      post(urlMatching(url))
        .withRequestBody(equalTo(request))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
