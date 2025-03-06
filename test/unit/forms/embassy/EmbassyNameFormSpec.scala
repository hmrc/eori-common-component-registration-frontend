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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.embassy.EmbassyNameForm

class EmbassyNameFormSpec extends UnitSpec {

  val form: Form[String] = new EmbassyNameForm().embassyNameForm()

  "embassy name form" should {
    "fail" when {
      "name is empty" in {
        form.bind(Map("name" -> ""))
          .errors shouldBe List(FormError("name", List("cds.matching.embassy-name.error.name"), List()))
      }

      "name contains tags" in {
        form.bind(Map("name" -> "<name>"))
          .errors shouldBe List(
          FormError("name", List("cds.matching-error.business-details.embassy-name.invalid-char"), List())
        )

      }
    }

    "succeed" when {
      "name is valid" in {
        form.bind(Map("name" -> "U.S. Embassy")).errors shouldBe empty
      }
    }
  }
}
