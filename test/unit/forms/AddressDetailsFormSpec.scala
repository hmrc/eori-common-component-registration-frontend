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

package unit.forms

import base.UnitSpec
import play.api.data.{Form, FormError}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel

class AddressDetailsFormSpec extends UnitSpec {

  "Address Details Form" should {
    "fail street validation" when {
      "street is empty" in {
        val formData = Map("street" -> "", "city" -> "London", "postcode" -> "SW3 5DA", "countryCode" -> "GB")
        val res: Form[AddressViewModel] = AddressDetailsForm.addressDetailsCreateForm().bind(formData)
        res.errors shouldBe Seq(FormError("street", "cds.subscription.address-details.street.empty.error"))
      }

      "street is longer than 70 characters" in {
        val formData = Map(
          "street"      -> "ofdwbagfpdisjafbddshfgdlsjgfdsaiuwpafdbsldgfsfjdofdwbagfpdisjafbddshfgdlsjgfdsaiuwpafdbsldgfsfjd",
          "city"        -> "London",
          "postcode"    -> "SW3 5DA",
          "countryCode" -> "GB"
        )
        val res: Form[AddressViewModel] = AddressDetailsForm.addressDetailsCreateForm().bind(formData)
        res.errors shouldBe Seq(FormError("street", "cds.subscription.address-details.street.too-long.error"))
      }

      "street contains invalid characters" in {
        val formData = Map("street" -> "^[^<>]+$", "city" -> "London", "postcode" -> "SW3 5DA", "countryCode" -> "GB")
        val res: Form[AddressViewModel] = AddressDetailsForm.addressDetailsCreateForm().bind(formData)
        res.errors shouldBe Seq(FormError("street", "cds.subscription.address-details.street.error.invalid-chars"))
      }
    }

    "fail country validation" when {
      "country code length is not 2" in {
        val formData =
          Map("street" -> "Chambers Lane", "city" -> "London", "postcode" -> "SW3 5DA", "countryCode" -> "GBC")
        val res: Form[AddressViewModel] = AddressDetailsForm.addressDetailsCreateForm().bind(formData)
        res.errors shouldBe Seq(FormError("countryCode", "cds.matching-error.country.invalid"))
      }

      "country code is empty" in {
        val formData =
          Map("street" -> "Chambers Lane", "city" -> "London", "postcode" -> "SW3 5DA", "countryCode" -> "")
        val res: Form[AddressViewModel] = AddressDetailsForm.addressDetailsCreateForm().bind(formData)
        res.errors shouldBe Seq(FormError("countryCode", "cds.matching-error.country.invalid"))
      }
    }
  }
}
