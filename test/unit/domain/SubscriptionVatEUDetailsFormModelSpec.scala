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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.SubscriptionVatEUDetailsFormModel

class SubscriptionVatEUDetailsFormModelSpec extends UnitSpec {

  val gbVatNumbers = List("123456789", "223456789")

  val gbVatIdentifications =
    List(VatIdentification(Some("GB"), Some("123456789")), VatIdentification(Some("GB"), Some("223456789")))

  val euVatNumbers   = List("123456789", "223456789")
  val euCountryCodes = List("ES", "PL")

  val euVatIdentifications =
    List(VatIdentification(Some("ES"), Some("123456789")), VatIdentification(Some("PL"), Some("223456789")))

  "SubscriptionVatEUDetailsFormModel" should {

    "convert vat numbers list and country codes list to vat identifications" in {
      SubscriptionVatEUDetailsFormModel.stringListsToVats(euCountryCodes, euVatNumbers) shouldBe euVatIdentifications
    }

    "convert vat identifications list to Some tuple of vat numbers list and country codes list" in {
      SubscriptionVatEUDetailsFormModel.vatsToStringLists(euVatIdentifications) shouldBe Some(
        euCountryCodes -> euVatNumbers
      )
    }

    "convert empty list of vat identifications to None instead of tuple of vat numbers list and country codes list" in {
      SubscriptionVatEUDetailsFormModel.vatsToStringLists(List.empty) shouldBe None
    }
  }
}
