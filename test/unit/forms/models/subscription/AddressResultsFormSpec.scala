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

package unit.forms.models.subscription

import base.UnitSpec
import play.api.data.FormError
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressResultsForm

class AddressResultsFormSpec extends UnitSpec {

  "Address Results Form" should {

    "return error" when {

      "address has not been chosen" in {

        val form = AddressResultsForm.form(Seq("allowedAddress")).bind(Map("address" -> ""))

        val expectedErrors = Seq(FormError("address", "ecc.address-lookup.postcode.address.error"))

        form.errors shouldBe expectedErrors
      }

      "address is not on the list" in {

        val form = AddressResultsForm.form(Seq("allowedAddress")).bind(Map("address" -> ""))

        val expectedErrors = Seq(FormError("address", "ecc.address-lookup.postcode.address.error"))

        form.errors shouldBe expectedErrors
      }
    }

    "return no errors" when {

      "address is correct" in {

        val form = AddressResultsForm.form(Seq("allowedAddress")).bind(Map("address" -> "allowedAddress"))

        form.errors shouldBe Seq.empty
      }
    }
  }
}
