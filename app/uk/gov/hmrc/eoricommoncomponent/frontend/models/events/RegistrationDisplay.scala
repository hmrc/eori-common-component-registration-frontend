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

package uk.gov.hmrc.eoricommoncomponent.frontend.models.events

import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.{
  RegistrationDisplayRequestHolder,
  RegistrationDisplayResponseHolder
}

case class RegistrationDisplay(request: RegistrationDisplaySubmitted, response: RegistrationDisplayResult)

object RegistrationDisplay {
  implicit val format = Json.format[RegistrationDisplay]

  def apply(
    request: RegistrationDisplayRequestHolder,
    response: RegistrationDisplayResponseHolder
  ): RegistrationDisplay =
    RegistrationDisplay(request = RegistrationDisplaySubmitted(request), response = RegistrationDisplayResult(response))

}
