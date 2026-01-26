/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.SubscriptionVatUKDetailsFormModel

class SubscriptionVatUKDetailsFormModelSpec extends UnitSpec {

  val gbVatNumbers = List("123456789", "223456789")

  val gbVatIdentifications =
    List(VatIdentification(Some("GB"), Some("123456789")), VatIdentification(Some("GB"), Some("223456789")))

  "SubscriptionVatUKDetailsFormModel" should {

    "convert to GB vat identifications when vat numbers for GB are provided as a list" in {
      SubscriptionVatUKDetailsFormModel.convertRequestForGbVatsToModel(Some(gbVatNumbers)) shouldBe Some(
        gbVatIdentifications
      )
    }

    "convert to list of GB vat number strings when vat identification objects are provided as a list" in {
      SubscriptionVatUKDetailsFormModel.convertModelForGbVatsToRequest(Some(gbVatIdentifications)) shouldBe Some(
        gbVatNumbers
      )
    }

    "create empty model when empty list of vat identifications is provided as an input" in {
      val model = SubscriptionVatUKDetailsFormModel(hasGbVats = false, None)

      model shouldBe SubscriptionVatUKDetailsFormModel(hasGbVats = false, gbVats = None)
    }

    "create gb model when list of only UK vat identifications is provided as an input" in {
      val model = SubscriptionVatUKDetailsFormModel(hasGbVats = true, Some(gbVatIdentifications))

      model shouldBe SubscriptionVatUKDetailsFormModel(hasGbVats = true, gbVats = Some(gbVatIdentifications))
    }
  }
}
