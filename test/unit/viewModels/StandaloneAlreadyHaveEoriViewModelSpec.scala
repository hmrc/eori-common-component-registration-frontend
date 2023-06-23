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

package unit.viewModels

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.models.viewModels.StandaloneAlreadyHaveEoriViewModel
import util.ControllerSpec

class StandaloneAlreadyHaveEoriViewModelSpec extends UnitSpec with ControllerSpec {

  private val viewModel = StandaloneAlreadyHaveEoriViewModel

  "titleAndHeaderLabel" should {

    "display correct message for admin user" in {
      viewModel(isAdminUser =
        true
      ).titleAndHeaderLabel() shouldBe "Your business or organisation already has an EORI number"
    }

    "display correct message for standard user" in {
      viewModel(isAdminUser = false).titleAndHeaderLabel() shouldBe "You already have an EORI number"
    }
  }

}
