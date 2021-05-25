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

package unit.domain

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{SubscriptionStatusQueryParams, TaxPayerId}
import org.joda.time.DateTime

class SubscriptionStatusQueryParamsSpec extends UnitSpec {

  val receiptDate = DateTime.parse("2016-3-17T9:30:47.114")
  val taxPayerId  = TaxPayerId("1234567890").mdgTaxPayerId
  "SubscriptionStatusQueryParamsSpec" should {
    "create a valid URL query string" in {
      val query: SubscriptionStatusQueryParams = SubscriptionStatusQueryParams(
        receiptDate = receiptDate,
        regime = "CDS",
        "taxPayerID",
        TaxPayerId("1234567890").mdgTaxPayerId
      )

      query.queryParams shouldBe Seq(
        "receiptDate" -> "2016-03-17T09:30:47Z",
        "regime"      -> "CDS",
        "taxPayerID"  -> taxPayerId
      )
    }
  }

}
