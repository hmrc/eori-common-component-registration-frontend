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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, __}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.EisError.Value

sealed trait EisError {
  val summary: String
  val value: Value
}

object EisError {
  case class SourceFaultDetail(detail: Seq[String])

  case class ErrorDetail(
    correlationId: String,
    errorCode: String,
    errorMessage: String,
    source: String,
    sourceFaultDetail: SourceFaultDetail,
    timestamp: String
  )

  case class Value(errorDetail: ErrorDetail)

  implicit val sourceFaultDetailReads: Reads[SourceFaultDetail] = Json.reads[SourceFaultDetail]
  implicit val errorDetailReads: Reads[ErrorDetail] = Json.reads[ErrorDetail]
  implicit val valueReads: Reads[Value] = Json.reads[Value]

  implicit val etmpErrorReads: Reads[EisError] = {
    (
      (__ \ "summary").read[String] and
        (__ \ "value").read[Value]
    )((s, v) =>
      new EisError {
        override val summary: String = s
        override val value: Value = v
      }
    )
  }

}
