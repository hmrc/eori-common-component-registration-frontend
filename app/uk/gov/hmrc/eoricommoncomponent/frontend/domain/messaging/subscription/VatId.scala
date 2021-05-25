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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription

import play.api.libs.json.Json

case class VatId(countryCode: Option[String], vatID: Option[String])

object VatId {
  implicit val jsonFormat = Json.format[VatId]
}

// TODO - When messaging updates their specs with VATID across for all the interfaces,then this case class can be removed.
case class SubscriptionInfoVatId(countryCode: Option[String], VATID: Option[String])

object SubscriptionInfoVatId {
  implicit val jsonFormat = Json.format[SubscriptionInfoVatId]
}
