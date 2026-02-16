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

package unit.domain

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SixLineAddressMatchModel

class SixLineAddressMatchModelSpec extends UnitSpec {

  "SixLineAddressMatchModel" should {

    "trim lines 1 to 4" in {
      val address =
        SixLineAddressMatchModel(" line 1 ", Some(" line 2 "), " line 3 ", Some(" line 4 "), Some(" HJ2 3HJ "), "FR")
      address.lineOne shouldBe "line 1"
      address.lineTwo shouldBe Some("line 2")
      address.lineThree shouldBe "line 3"
      address.lineFour shouldBe Some("line 4")
      address.postcode shouldBe Some("HJ2 3HJ")
      address.country shouldBe "FR"
    }
  }
}
