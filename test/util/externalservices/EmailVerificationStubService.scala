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
import play.mvc.Http.Status._

object EmailVerificationStubService {

  private val expectedVerifiedEmailPostUrl = "/email-verification/verified-email-check"

  private val expectedPostUrl = "/email-verification/verification-requests"

  val emailVerifiedResponseJson: JsValue = Json.parse("""{"email": "john.doe@example.com"}""")

  val emailVerificationNotFoundJson: JsValue = Json.parse("""{
      |  "code": "EMAIL_NOT_FOUND_OR_NOT_VERIFIED",
      |  "message":"Email not verified."
      |}""".stripMargin)

  val internalServerErrorJson: JsValue = Json.parse("""{
      |  "code": "UNEXPECTED_ERROR",
      |  "message":"An unexpected error occurred."
      |}""".stripMargin)

  def stubEmailVerified() =
    stubTheVerifiedEmailResponse(expectedVerifiedEmailPostUrl, emailVerifiedResponseJson.toString, OK)

  def stubEmailNotVerified() =
    stubTheVerifiedEmailResponse(expectedVerifiedEmailPostUrl, emailVerificationNotFoundJson.toString, NOT_FOUND)

  def stubEmailVerifiedInternalServerError() =
    stubTheVerifiedEmailResponse(expectedVerifiedEmailPostUrl, internalServerErrorJson.toString, INTERNAL_SERVER_ERROR)

  def stubVerificationRequestSent() =
    stubVerificationRequest(expectedPostUrl, "", CREATED)

  def stubEmailAlreadyVerified() =
    stubVerificationRequest(expectedPostUrl, "", CONFLICT)

  def stubVerificationRequestError() =
    stubVerificationRequest(expectedPostUrl, internalServerErrorJson.toString(), INTERNAL_SERVER_ERROR)

  def stubTheVerifiedEmailResponse(url: String, response: String, status: Int): Unit =
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  def stubVerificationRequest(url: String, response: String, status: Int): Unit =
    stubFor(
      post(urlMatching(url))
        .willReturn(
          aResponse()
            .withBody(response)
            .withStatus(status)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
