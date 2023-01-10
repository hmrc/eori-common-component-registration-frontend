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

package unit.forms.models.subscription

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormValidation.postcodeRegex

class PostcodeRegexSpec extends UnitSpec {
  "PostcodeRegex  should match" when {

    "supplied with a valid 2char-3char post code format" in {
      "S90DL".matches(postcodeRegex.regex) shouldBe true
      "S9 0DL".matches(postcodeRegex.regex) shouldBe true
    }

    "supplied with a valid 3char-3char post code format" in {
      "SE77JU".matches(postcodeRegex.regex) shouldBe true
      "SE7 7JU".matches(postcodeRegex.regex) shouldBe true
      "S1W 0NY".matches(postcodeRegex.regex) shouldBe true
      "W1W0NY".matches(postcodeRegex.regex) shouldBe true
    }

    "supplied with a valid 4char-3char post code format" in {
      "SA201JA".matches(postcodeRegex.regex) shouldBe true
      "SA20 1JA".matches(postcodeRegex.regex) shouldBe true
      "SW4W0NY".matches(postcodeRegex.regex) shouldBe true
      "NW1W 0NY".matches(postcodeRegex.regex) shouldBe true
    }

    "supplied with a lowercase post code" in {
      "sa201ja".matches(postcodeRegex.regex) shouldBe true
      "sa20 1ja".matches(postcodeRegex.regex) shouldBe true
      "sw4w0ny".matches(postcodeRegex.regex) shouldBe true
      "nw1w 0ny".matches(postcodeRegex.regex) shouldBe true
    }

    "supplied with a mixed case post code" in {
      "sA201ja".matches(postcodeRegex.regex) shouldBe true
      "sw1W0ny".matches(postcodeRegex.regex) shouldBe true
    }

  }

}
