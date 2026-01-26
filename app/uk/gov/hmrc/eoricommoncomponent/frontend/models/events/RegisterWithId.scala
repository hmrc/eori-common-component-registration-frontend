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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.events

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{JsPath, Writes, __}

case class RegisterWithId(`type`: String, request: RegisterWithIdSubmitted, response: RegisterWithIdConfirmation)

object RegisterWithId {

  // writes for flattened Audit event
  implicit val writes: Writes[RegisterWithId] =
    (__ \ "type")
      .write[String]
      .and(
        JsPath
          .write[RegisterWithIdSubmitted]
      )
      .and(JsPath.write[RegisterWithIdConfirmation])(unlift(RegisterWithId.unapply))

  def apply(request: RegisterWithIdSubmitted, response: RegisterWithIdConfirmation): RegisterWithId =
    RegisterWithId(`type` = "RegisterWithId", request = request, response = response)

}
