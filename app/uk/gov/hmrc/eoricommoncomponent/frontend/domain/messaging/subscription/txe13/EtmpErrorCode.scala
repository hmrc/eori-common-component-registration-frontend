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

import play.api.libs.json.{__, JsObject, Reads}

case class EtmpErrorCode(errorCode: EisError)

object EtmpErrorCode {

  /**
    * We don't know what the key will be at the top level hence we retrieve
    * the key first then do the look up by the key since we are not interested
    * in the key itself
    *
    * key examples according to API Documentation:
    * errorcode007, errorcode133, errorcode135, invalidEdgeCaseType, invalidIoMPostcode
    */
  implicit val reads: Reads[EtmpErrorCode] = {
    __.read[JsObject].map(_.keys.head).flatMap { key =>
      (__ \ key).read[EisError](EisError.etmpErrorReads).map(EtmpErrorCode(_))
    }
  }

}
