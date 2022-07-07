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
import play.mvc.Http.Status.NO_CONTENT

object CustomsDataStoreStubService {

  private val customsDataStoreUrl = "/customs/update/datastore"

  def stubCustomsDataStoreEndpoint(returnStatus: Int = NO_CONTENT): Unit =
    stubFor(
      post(urlEqualTo(customsDataStoreUrl))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
        )
    )

  def returnCustomsDataStoreEndpointWhenReceiveRequest(url: String, request: String, status: Int): Unit =
    stubFor(
      post(urlEqualTo(url))
        .withRequestBody(equalToJson(request))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

  def verifyRcmEndpointCalled(): Unit = verify(postRequestedFor(urlEqualTo(customsDataStoreUrl)))
}
