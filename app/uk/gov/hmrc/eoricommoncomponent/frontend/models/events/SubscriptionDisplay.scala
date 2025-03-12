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
import play.api.libs.json.{JsPath, Writes}

case class SubscriptionDisplay(request: SubscriptionDisplaySubmitted, response: SubscriptionDisplayResult)

object SubscriptionDisplay {

  //writes for flattened Audit event
  implicit val writes: Writes[SubscriptionDisplay] =
    JsPath
      .write[SubscriptionDisplaySubmitted]
      .and(JsPath.write[SubscriptionDisplayResult])(unlift(SubscriptionDisplay.unapply))

}
