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

package unit.forms

import base.UnitSpec
import play.api.data.{Form, FormError}
import play.api.data.validation.Constraint
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.ContactDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.{ContactDetailsViewModel, VatDetails, VatDetailsForm}

class VatDetailsFormSpec extends UnitSpec {

  def form: Form[VatDetails] = VatDetailsForm.vatDetailsForm

  "contactDetailsCreateForm" should {

    "fail when mandatory fields are empty" in {
      val data = Map("postcode" -> "", "vat-number" -> "")
      val res  = form.bind(data)
      res.errors shouldBe Seq(
        FormError("postcode", "cds.subscription.vat-details.postcode.required.error"),
        FormError("vat-number", List("cds.subscription.vat-uk.required.error"))
      )
    }

    "accept a valid postcode and vat-number" in {
      val data = Map("postcode" -> "AA1 1AA", "vat-number" -> "123456789")
      val res  = form.bind(data)
      res.errors shouldBe Seq.empty
      res.value shouldBe Some(VatDetails("AA1 1AA", "123456789"))
    }

    "accept a valid postcode and vat-number with spaces" in {
      val data = Map("postcode" -> "AA1 1AA", "vat-number" -> "1 2 3 4 5 6 7 8 9")
      val res  = form.bind(data)
      res.errors shouldBe Seq.empty
      res.value shouldBe Some(VatDetails("AA1 1AA", "123456789"))
    }

    "fail when vat-number is too long" in {
      val data = Map("postcode" -> "AA1 1AA", "vat-number" -> "1 2 3 4 5 6 7 8 96666")
      val res  = form.bind(data)
      res.errors shouldBe Seq(FormError("vat-number", "cds.subscription.vat-uk.length.error"))
    }

  }

}
