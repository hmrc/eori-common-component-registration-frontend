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

package unit.forms.embassy

import base.UnitSpec
import play.api.data.{Form, FormError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EmbassyAddressMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.embassy.EmbassyAddressForm

class EmbassyAddressFormSpec extends UnitSpec {

  val form: Form[EmbassyAddressMatchModel] = new EmbassyAddressForm().form

  val validAddress: Map[String, String] = Map(
    "line-1"      -> "33 Nine Elms Ln",
    "line-2"      -> "Nine Elms",
    "townCity"    -> "London",
    "postcode"    -> "SW11 7US",
    "countryCode" -> "GB"
  )

  "embassy address form" should {
    "accept valid address" in {
      form.bind(validAddress).errors shouldBe empty
    }

    "fail" when {
      "line one is empty" in {
        form.bind(validAddress.updated("line-1", "")).errors shouldBe List(FormError("line-1", List("cds.matching.embassy-address.line-1.error.empty"), List()))
      }

      "line one contains tags" in {
        form.bind(validAddress.updated("line-1", "<108 Lily Drive>")).errors shouldBe List(
          FormError("line-1", List("cds.matching.embassy-address.line-1.error.invalid-chars"), List())
        )
      }

      "line two contains tags" in {
        form.bind(validAddress.updated("line-2", "<108 Lily Drive>")).errors shouldBe List(
          FormError("line-2", List("cds.matching.embassy-address.line-2.error.invalid-chars"), List())
        )
      }

      "town is empty" in {
        form.bind(validAddress.updated("townCity", "")).errors shouldBe List(
          FormError("townCity", List("cds.matching.embassy-address.town-city.error.empty"), List())
        )
      }

      "town is too long" in {
        form.bind(validAddress.updated("townCity", "A much longer than usual town city name")).errors shouldBe List(
          FormError("townCity", List("cds.matching.embassy-address.town-city.error.too-long"), List())
        )
      }

      "town contains tags" in {
        form.bind(validAddress.updated("townCity", "<London>")).errors shouldBe List(
          FormError("townCity", List("cds.matching.embassy-address.line-3.error.invalid-chars"), List())
        )
      }
    }
  }
}
