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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressLookupParams

import scala.util.Random

class AddressLookupParamsSpec extends UnitSpec {

  "Address Lookup Params" should {

    "return correct value for isEmpty method" in {

      AddressLookupParams("", None).isEmpty() shouldBe true
      AddressLookupParams("", Some("")).isEmpty() shouldBe true

      AddressLookupParams("postcode", None).isEmpty() shouldBe false
      AddressLookupParams("postcode", Some("Line1")).isEmpty() shouldBe false

    }

    "return correct value for nonEmpty method" in {

      AddressLookupParams("", None).nonEmpty() shouldBe false
      AddressLookupParams("", Some("")).nonEmpty() shouldBe false

      AddressLookupParams("postcode", None).nonEmpty() shouldBe true
      AddressLookupParams("postcode", Some("Line1")).nonEmpty() shouldBe true
    }
  }

  "Address Lookup Params form" should {

    "return error" when {

      "postcode is empty" in {

        val form = AddressLookupParams.form().bind(Map("postcode" -> ""))

        val expectedErrors = Seq(FormError("postcode", "cds.subscription.contact-details.error.postcode"))

        form.errors shouldBe expectedErrors
      }

      "postcode is invalid" in {

        val form = AddressLookupParams.form().bind(Map("postcode" -> "invalid"))

        val expectedErrors = Seq(FormError("postcode", "cds.subscription.contact-details.error.postcode"))

        form.errors shouldBe expectedErrors
      }

      "line1 is longer than 35 characters" in {

        val incorrectLine1: String = Random.alphanumeric.take(36).mkString("")
        val form                   = AddressLookupParams.form().bind(Map("postcode" -> "AA11 1AA", "line1" -> incorrectLine1))

        val expectedErrors = Seq(FormError("line1", "ecc.address-lookup.postcode.line1.error"))

        form.errors shouldBe expectedErrors
      }
    }

    "return no errors" when {

      "postcode is valid" in {

        val form = AddressLookupParams.form().bind(Map("postcode" -> "AA11 1AA"))

        form.errors shouldBe Seq.empty
      }

      "postcode and line1 are valid" in {

        val form = AddressLookupParams.form().bind(Map("postcode" -> "AA11 1AA", "line1" -> "Line 1"))

        form.errors shouldBe Seq.empty
      }
    }

    "convert correct postcode to upper case" in {

      val form = AddressLookupParams.form().bind(Map("postcode" -> "aa11 1aa"))

      form.value.get.postcode shouldBe "AA11 1AA"
    }
  }
}
