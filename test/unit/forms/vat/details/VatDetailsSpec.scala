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

package unit.forms.vat.details

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.vat.details.VatDetails

class VatDetailsSpec extends AnyWordSpec with Matchers {

  "VatDetails.isGiant" should {

    "return true when number starts with 654" in {
      val vat = VatDetails("AB12CD", "65412345")
      vat.isGiant shouldBe true
    }

    "return true when number starts with 8888" in {
      val vat = VatDetails("AB12CD", "88881234")
      vat.isGiant shouldBe true
    }

    "return false when number does not start with 654 or 8888" in {
      val vat = VatDetails("AB12CD", "12345678")
      vat.isGiant shouldBe false
    }

    "return false for empty number" in {
      val vat = VatDetails("AB12CD", "")
      vat.isGiant shouldBe false
    }
  }

  "VatDetails JSON format" should {

    "write to JSON correctly" in {
      val vat = VatDetails("AB12CD", "65412345")

      val json = Json.toJson(vat)

      (json \ "postcode").as[String] shouldBe "AB12CD"
      (json \ "number").as[String] shouldBe "65412345"
    }

    "read from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "postcode": "AB12CD",
          |  "number": "65412345"
          |}
        """.stripMargin
      )

      val result = json.as[VatDetails]

      result shouldBe VatDetails("AB12CD", "65412345")
    }

    "round-trip correctly" in {
      val vat = VatDetails("ZZ99ZZ", "88881234")

      val json = Json.toJson(vat)
      val result = json.as[VatDetails]

      result shouldBe vat
    }
  }
}
