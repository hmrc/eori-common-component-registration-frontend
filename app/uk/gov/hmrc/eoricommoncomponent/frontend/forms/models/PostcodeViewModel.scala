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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms.models

import play.api.libs.json.{Json, OFormat}

case class PostcodeViewModel(postcode: String, addressLine1: Option[String]) {
  def isEmpty(): Boolean = postcode == "" && addressLine1.forall(_.isEmpty)

  def nonEmpty(): Boolean = !isEmpty()
}

object PostcodeViewModel {
  implicit val jsonFormat: OFormat[PostcodeViewModel] = Json.format[PostcodeViewModel]

  def apply(postcode: String, addressLine1: Option[String]): PostcodeViewModel =
    new PostcodeViewModel(postcode, addressLine1.map(_.trim))

}
