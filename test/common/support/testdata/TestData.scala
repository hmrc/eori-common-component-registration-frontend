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

package common.support.testdata

import java.time.{LocalDateTime, ZoneId, ZoneOffset}

object TestData {
  val Eori: String                    = "EN123456789012345"
  val Name: String                    = "John Doe"
  val ProcessedDate: String           = "01 May 2016"
  val TaxPayerID: String              = "0100086619"
  val emailVerificationTimestamp      = LocalDateTime.now(ZoneId.of("Europe/London"))
  val zonedEmailVerificationTimestamp = emailVerificationTimestamp.atZone(ZoneOffset.UTC)
}
