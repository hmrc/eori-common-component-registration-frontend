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

package unit.domain

import java.time.LocalDateTime
import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{SubscriptionStatusQueryParams, TaxPayerId}

class SubscriptionStatusQueryParamsSpec extends UnitSpec {

  private val taxPayerId = TaxPayerId("1234567890").mdgTaxPayerId

  "SubscriptionStatusQueryParamsSpec" should {

    "create a valid URL query string" in {
      val query: SubscriptionStatusQueryParams =
        SubscriptionStatusQueryParams(LocalDateTime.of(2016, 3, 17, 9, 30, 47, 0), "CDS", "taxPayerID", taxPayerId)

      query.queryParams shouldBe Seq(
        "receiptDate" -> "2016-03-17T09:30:47Z",
        "regime"      -> "CDS",
        "taxPayerID"  -> taxPayerId
      )
    }

    "correctly format the receiptDate to include seconds" in {
      val query: SubscriptionStatusQueryParams = SubscriptionStatusQueryParams(
        LocalDateTime.of(2016, 3, 17, 9, 30, 0, 0),
        regime = "CDS",
        "taxPayerID",
        taxPayerId
      )

      query.queryParams shouldBe Seq(
        "receiptDate" -> "2016-03-17T09:30:00Z",
        "regime"      -> "CDS",
        "taxPayerID"  -> taxPayerId
      )
    }
  }
}
