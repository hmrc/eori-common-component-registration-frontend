/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.events

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Writes, __}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{RegisterWithoutIDRequest, RegisterWithoutIdResponseHolder}

case class RegisterWithoutId(`type`: String, request: RegisterWithoutIdSubmitted, response: RegisterWithoutIdResult)

object RegisterWithoutId {

  // writes for flattened Audit event
  implicit val writes: Writes[RegisterWithoutId] =
    (__ \ "type")
      .write[String]
      .and(JsPath.write[RegisterWithoutIdSubmitted])
      .and(JsPath.write[RegisterWithoutIdResult])((regWithoutId: RegisterWithoutId) => (regWithoutId.`type`, regWithoutId.request, regWithoutId.response))

  def apply(request: RegisterWithoutIDRequest, response: RegisterWithoutIdResponseHolder): RegisterWithoutId =
    RegisterWithoutId(
      `type` = "RegisterWithoutId",
      request = RegisterWithoutIdSubmitted(request),
      response = RegisterWithoutIdResult(response)
    )

}
