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

package unit.models.address

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.AddressLookupSuccess

class AddressLookupSuccessSpec extends UnitSpec {

  "Address Lookup Success" should {

    "correctly sort the addresses with numbers combined with letters at the end of the address line" in {

      val address1 = Address("Flat 1", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address2 = Address("Flat 1A", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address3 = Address("Flat 1B", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address4 = Address("Flat 2", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address5 = Address("Flat 3", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the addresses with numbers" in {

      val address1 = Address("121 Test Street", None, None, Some("City"), Some("postcode"), "GB")
      val address2 = Address("122 Test Street", None, None, Some("City"), Some("postcode"), "GB")
      val address3 = Address("123 Test Street", None, None, Some("City"), Some("postcode"), "GB")
      val address4 = Address("124 Test Street", None, None, Some("City"), Some("postcode"), "GB")
      val address5 = Address("125 Test Street", None, None, Some("City"), Some("postcode"), "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the alphabetic addresses" in {

      val address1 = Address("A Test address", None, Some("Test Street"), Some("City"), Some("postcode"), "GB")
      val address2 = Address("B Test address", None, Some("Test Street"), Some("City"), Some("postcode"), "GB")
      val address3 = Address("C Test address", None, Some("Test Street"), Some("City"), Some("postcode"), "GB")
      val address4 = Address("D Test address", None, Some("Test Street"), Some("City"), Some("postcode"), "GB")
      val address5 = Address("E Test address", None, Some("Test Street"), Some("City"), Some("postcode"), "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the alphanumeric addresses with different middle part" in {

      val address1 = Address("Test 120 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address2 = Address("Test 121 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address3 = Address("Test 122 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address4 = Address("Test 123 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address5 = Address("Test 124 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the mixture of alphabetic and alphanumeric addresses" in {

      val address1 = Address("Test 121 address", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address2 = Address("Test 122 address", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address3 = Address("Test 123 address", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address4 = Address("Test first address", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")
      val address5 = Address("Test second address", Some("Test Street"), Some("City"), None, Some("postcode"), "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the special characters" in {

      val address1 = Address("Test 3-5 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address2 = Address("Test 10 address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address3 = Address("b/y", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address4 = Address("Test & Address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")
      val address5 = Address("Test address", Some("Test Street"), None, Some("City"), Some("postcode"), "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }
  }
}
