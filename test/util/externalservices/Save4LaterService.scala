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
import play.api.libs.json.{JsValue, Json, OFormat}
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status._

object Save4LaterService {

  val body: String = """{"name":"test","tel":12}""".stripMargin

  val responseJson: JsValue = Json.parse(body)

  val id       = "id-12345678"
  val emailKey = "email"

  case class User(name: String, tel: Int)

  object User {
    implicit val jsonFormat: OFormat[User] = Json.format[User]
  }

  val expectedUrl          = s"/save4later/$id/$emailKey"
  val expectedDeleteUrl    = s"/save4later/$id"
  val expectedDeleteKeyUrl = s"/save4later/$id/$emailKey"

  def stubSave4LaterGET_OK(): Unit = stubSave4LaterGETResponse(expectedUrl, body, OK)

  def stubSave4LaterGET_NOTFOUND(): Unit = stubSave4LaterGETResponse(expectedUrl, body, NOT_FOUND)

  def stubSave4LaterGET_BAD_REQUEST(): Unit = stubSave4LaterGETResponse(expectedUrl, body, BAD_REQUEST)

  def stubSave4LaterPUT(): Unit = stubSave4LaterPUTResponse(expectedUrl, body, CREATED)

  def stubSave4LaterPUT_BAD_REQUEST(): Unit = stubSave4LaterPUTResponse(expectedUrl, body, BAD_REQUEST)

  def stubSave4LaterDELETE(): Unit = stubSave4LaterDeleteResponse(expectedDeleteUrl, NO_CONTENT)

  def stubSave4LaterNotFoundDELETE(): Unit = stubSave4LaterDeleteResponse(expectedDeleteUrl, NOT_FOUND)

  def stubSave4LaterDELETEKey(): Unit = stubSave4LaterDeleteResponse(expectedDeleteKeyUrl, NO_CONTENT)

  def stubSave4LaterNotFoundDELETEKey(): Unit = stubSave4LaterDeleteResponse(expectedDeleteKeyUrl, NOT_FOUND)

  def stubSave4LaterGETResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      get(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def stubSave4LaterPUTResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      put(urlMatching(url))
        .withRequestBody(containing(body))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def stubSave4LaterDeleteResponse(url: String, status: Int): Unit =
    stubFor(
      delete(urlMatching(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
