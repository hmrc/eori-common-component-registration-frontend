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

package util.builders

import java.util.UUID

import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.{CSRFTokenHelper, FakeRequest}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionDataKeys
import uk.gov.hmrc.http.SessionKeys

object SessionBuilder {

  def sessionMap(authToken: String): List[(String, String)] = {
    val sessionId = s"session-${UUID.randomUUID}"
    List(SessionKeys.sessionId -> sessionId, SessionKeys.authToken -> authToken)
  }

  def addToken[T](fakeRequest: FakeRequest[T]): FakeRequest[T] =
    new FakeRequest(CSRFTokenHelper.addCSRFToken(fakeRequest))

  def buildRequestWithSession(authtoken: String) =
    addToken(FakeRequest().withSession(sessionMap(authtoken): _*))

  def buildRequestWithSessionAndFormValues(
    userId: String,
    form: Map[String, String]
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    buildRequestWithSession(userId).withFormUrlEncodedBody(form.toList: _*)

  def buildRequestWithFormValues(form: Map[String, String]): FakeRequest[AnyContentAsFormUrlEncoded] =
    buildRequestWithSessionNoUserAndToken.withFormUrlEncodedBody(form.toList: _*)

  def buildRequestWithSessionNoUser = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(SessionKeys.sessionId -> sessionId)
  }

  def buildRequestWithSessionNoUserAndToken = {
    val sessionId = s"session-${UUID.randomUUID}"
    addToken(FakeRequest().withSession(SessionKeys.sessionId -> sessionId))
  }

  def buildRequestWithSessionAndPathNoUserAndBasedInUkNotSelected(method: String, path: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest(method, path).withSession(SessionKeys.sessionId -> sessionId)
  }

  def buildRequestWithSessionAndPathNoUser(method: String, path: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest(method, path).withSession(SessionKeys.sessionId -> sessionId, "visited-uk-page" -> "true")
  }

  def buildRequestWithSessionAndPath(path: String, authToken: String, method: String = "GET") =
    addToken(FakeRequest(method, path)).withSession(sessionMap(authToken): _*)

  def buildRequestWithSessionAndPathAndFormValues(
    method: String,
    path: String,
    authToken: String,
    form: Map[String, String]
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(method, path).withSession(sessionMap(authToken): _*).withFormUrlEncodedBody(form.toList: _*)

  def buildRequestWithSessionAndOrgType(authToken: String, orgTypeId: String) = {
    val list    = (RequestSessionDataKeys.selectedOrganisationType -> orgTypeId) :: sessionMap(authToken)
    val request = FakeRequest().withSession(list: _*)
    addToken(request)
  }

}
