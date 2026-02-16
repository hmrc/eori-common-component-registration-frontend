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

package unit.forms

import base.UnitSpec
import play.api.data.Form
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.SixLineAddressMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.sixlineaddress.SixLineAddressFormProvider

class MatchingFormsSpec extends UnitSpec {

  "thirdCountrySixLineAddressForm" should {
    def addressForm: Form[SixLineAddressMatchModel] = new SixLineAddressFormProvider().thirdCountrySixLineAddressForm

    "remove any white space from postcode" in {
      val data = Map(
        "line-1"      -> "Some Line-1",
        "line-2"      -> "Some Line-2",
        "line-3"      -> "Some Line-3",
        "line-4"      -> "Some Line-4",
        "postcode"    -> "   1  1   1111   111   ",
        "countryCode" -> "LV"
      )
      val res = addressForm.bind(data)
      res.errors shouldBe Seq.empty
      res.value shouldBe Some(
        SixLineAddressMatchModel(
          lineOne = "Some Line-1",
          lineTwo = Some("Some Line-2"),
          lineThree = "Some Line-3",
          lineFour = Some("Some Line-4"),
          postcode = Some("111111111"),
          country = "LV"
        )
      )
    }

  }

}
