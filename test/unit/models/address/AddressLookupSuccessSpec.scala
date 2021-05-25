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

package unit.models.address

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{AddressLookup, AddressLookupSuccess}

class AddressLookupSuccessSpec extends UnitSpec {

  "Address Lookup Success" should {

    "correctly sort the addresses with numbers combined with letters at the end of the address line" in {

      val address1 = AddressLookup("Flat 1, Test Street", "City", "postcode", "GB")
      val address2 = AddressLookup("Flat 1A, Test Street", "City", "postcode", "GB")
      val address3 = AddressLookup("Flat 1B, Test Street", "City", "postcode", "GB")
      val address4 = AddressLookup("Flat 2, Test Street", "City", "postcode", "GB")
      val address5 = AddressLookup("Flat 3, Test Street", "City", "postcode", "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the addresses with numbers" in {

      val address1 = AddressLookup("121 Test Street", "City", "postcode", "GB")
      val address2 = AddressLookup("122 Test Street", "City", "postcode", "GB")
      val address3 = AddressLookup("123 Test Street", "City", "postcode", "GB")
      val address4 = AddressLookup("124 Test Street", "City", "postcode", "GB")
      val address5 = AddressLookup("125 Test Street", "City", "postcode", "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the alphabetic addresses" in {

      val address1 = AddressLookup("A Test address, Test Street", "City", "postcode", "GB")
      val address2 = AddressLookup("B Test address, Test Street", "City", "postcode", "GB")
      val address3 = AddressLookup("C Test address, Test Street", "City", "postcode", "GB")
      val address4 = AddressLookup("D Test address, Test Street", "City", "postcode", "GB")
      val address5 = AddressLookup("E Test address, Test Street", "City", "postcode", "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the alphanumeric addresses with different middle part" in {

      val address1 = AddressLookup("Test 120 address, Test Street", "City", "postcode", "GB")
      val address2 = AddressLookup("Test 121 address, Test Street", "City", "postcode", "GB")
      val address3 = AddressLookup("Test 122 address, Test Street", "City", "postcode", "GB")
      val address4 = AddressLookup("Test 123 address, Test Street", "City", "postcode", "GB")
      val address5 = AddressLookup("Test 124 address, Test Street", "City", "postcode", "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the mixture of alphabetic and alphanumeric addresses" in {

      val address1 = AddressLookup("Test 121 address, Test Street", "City", "postcode", "GB")
      val address2 = AddressLookup("Test 122 address, Test Street", "City", "postcode", "GB")
      val address3 = AddressLookup("Test 123 address, Test Street", "City", "postcode", "GB")
      val address4 = AddressLookup("Test first address, Test Street", "City", "postcode", "GB")
      val address5 = AddressLookup("Test second address, Test Street", "City", "postcode", "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }

    "correctly sort the special characters" in {

      val address1 = AddressLookup("Test 3-5 address, Test Street", "City", "postcode", "GB")
      val address2 = AddressLookup("Test 10 address, Test Street", "City", "postcode", "GB")
      val address3 = AddressLookup("b/y, Test Street", "City", "postcode", "GB")
      val address4 = AddressLookup("Test & Address, Test Street", "City", "postcode", "GB")
      val address5 = AddressLookup("Test address, Test Street", "City", "postcode", "GB")

      val addresses = Seq(address3, address4, address2, address1, address5)

      val expectedOrder = Seq(address1, address2, address3, address4, address5)

      AddressLookupSuccess(addresses).sorted().addresses shouldBe expectedOrder
    }
  }
}
