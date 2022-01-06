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

package util.stubs

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.MimeTypes
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.Status._
import util.WireMockRunner

trait PdfGeneratorServiceStub extends WireMockRunner {

  def returnResponseForGenerateRequestWithBody(html: String, responseBody: Array[Byte]): Unit =
    stubForRequest(request().withRequestBody(matching(s"""\\{\\"html\\":\\"$html\\"\\}""")), responseBody)

  private def stubForRequest(request: MappingBuilder, responseBody: Array[Byte]): Any =
    stubFor(
      request
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(responseBody)
        )
    )

  private def request() =
    post(urlPathEqualTo("/pdf-generator-service/generate"))
      .withHeader(CONTENT_TYPE, matching(MimeTypes.JSON))

}
