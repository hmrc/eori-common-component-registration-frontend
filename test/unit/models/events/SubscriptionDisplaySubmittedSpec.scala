/*
 * Copyright 2024 HM Revenue & Customs
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

package unit.models.events

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.SubscriptionDisplaySubmitted

class SubscriptionDisplaySubmittedSpec extends AnyWordSpec with Matchers {

  "SubscriptionDisplaySubmitted" when {

    "create an instance of SubscriptionDisplaySubmitted for the input params" in {
      val params = Seq(
        "regime"                   -> Service.regimeCDS,
        "EORI"                     -> "EORI1234",
        "taxPayerID"               -> "safeId",
        "acknowledgementReference" -> "acknowledgementReference"
      )
      SubscriptionDisplaySubmitted.applyAndAlignKeys(params.toMap) mustBe SubscriptionDisplaySubmitted(Map("regime" -> "CDS", "eori" -> "EORI1234", "taxPayerID" -> "safeId", "acknowledgementReference" -> "acknowledgementReference"))
    }
  }
}
