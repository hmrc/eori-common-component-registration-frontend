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

package unit.models.events

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SubscriptionStatusQueryParams
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.SubscriptionStatusSubmitted

import java.time.LocalDateTime

class SubscriptionStatusSubmittedSpec extends AnyWordSpec with Matchers with EitherValues with OptionValues {

  val service: String = Service.cds.code

  "SubscriptionStatusSubmitted" should {
    "have taxPayerId set to request.id if request.idType is taxPayerId and safeId set to None if request.idType is not SAFE" in {
      val request = SubscriptionStatusQueryParams(
        receiptDate = LocalDateTime.now(),
        regime = "testRegime",
        idType = "taxPayerId",
        id = "testId"
      )

      val result = SubscriptionStatusSubmitted(request, service)
      result.taxPayerId mustBe Some(request.id)
      result.safeId mustNot (be(defined))
    }

    "have safeId set to request.id if request.idType is SAFE and taxPayerId set to None if request.idType is not taxPayerId" in {
      val request = SubscriptionStatusQueryParams(
        receiptDate = LocalDateTime.now(),
        regime = "testRegime",
        idType = "SAFE",
        id = "testId"
      )

      val result = SubscriptionStatusSubmitted(request, service)
      result.safeId mustBe Some(request.id)
      result.taxPayerId mustNot (be(defined))
    }
  }
}
