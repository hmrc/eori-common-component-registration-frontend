/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.domain.messaging

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.postcodeRegex
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel

class mdgModelsSpec extends UnitSpec {

  "isValidAddress" when {

    val addressLine1 = "some building"
    val addressLine2 = "some street"
    val addressLine3 = "some area"
    val addressLine4 = "some town"
    val countryCode  = "GB"

    def addressSetter(postCodeVal: String) =
      Address(addressLine1, Some(addressLine2), Some(addressLine3), Some(addressLine4), Some(postCodeVal), countryCode)

    "supplied with a valid postcode" in {
      addressSetter("PC55 5AA").isValidAddress shouldBe true
      addressSetter("M5 5AA").isValidAddress shouldBe true
      addressSetter("M5   5AA").isValidAddress shouldBe true
    }

    "supplied with an invalid postcode" in {
      addressSetter("P1C55 5AA").isValidAddress shouldBe false
      addressSetter("            M1").isValidAddress shouldBe false
      addressSetter("").isValidAddress shouldBe false
    }

    "supplied with an invalid countryCode" in {
      val invalidCountryCodeAddress =
        Address(addressLine1, Some(addressLine2), Some(addressLine3), Some(addressLine4), Some("PC55 5AA"), "EN")
      invalidCountryCodeAddress.isValidAddress shouldBe false
    }
  }
}
