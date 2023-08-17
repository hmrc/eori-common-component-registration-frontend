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

  private val expectedPostUrl = "/email-verification/verify-email"

  val verifyEmailSuccessResponse: JsValue = Json.parse("""{"redirectUri": "google.com"}""")
  val verifyEmailFailureResponse          = "Something went wrong"
  val verifyEmailInvalidResponse: JsValue = Json.parse("""{"something": "google.com"}""")

  def stubVerifyEmailSuccess() =
    stubVerifyEmailResponse(verifyEmailSuccessResponse.toString, CREATED)

  def stubVerifyEmailFailure() =
    stubVerifyEmailResponse(verifyEmailFailureResponse, INTERNAL_SERVER_ERROR)

  def stubVerifyEmailInvalid() =
    stubVerifyEmailResponse(verifyEmailInvalidResponse.toString, CREATED)

  def stubVerifyEmailResponse(response: String, status: Int): Unit =
    stubFor(
      post(urlEqualTo(expectedPostUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

  private def expectedVerificationStatusUrl(credId: String) = s"/email-verification/verification-status/$credId"

  val verificationStatusSuccessResponse: JsValue = Json.parse("""{
   | "emails":[
   |  {
   |     "emailAddress":"fredbloggs@hotmail.com",
   |      "verified":true,
   |      "locked":false
   |   },
   |   {
   |      "emailAddress": "somename@live.com",
   |      "verified":false,
   |      "locked":true
   |   }
   |]
   |}""".stripMargin)

  def stubVerificationStatusSuccess(credId: String) =
    stubVerificationStatusResponse(verificationStatusSuccessResponse.toString, OK, credId)

  def stubVerificationStatusFailure(credId: String) =
    stubVerificationStatusResponse(verifyEmailFailureResponse, INTERNAL_SERVER_ERROR, credId)

  def stubVerificationStatusInvalid(credId: String) =
    stubVerificationStatusResponse(verifyEmailInvalidResponse.toString, OK, credId)

  def stubVerificationStatusResponse(response: String, status: Int, credId: String): Unit =
    stubFor(
      get(urlEqualTo(expectedVerificationStatusUrl(credId)))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
