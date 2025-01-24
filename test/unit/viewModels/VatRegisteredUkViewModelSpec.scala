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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.VatRegisteredUkViewModel
import util.ControllerSpec

class VatRegisteredUkViewModelSpec extends UnitSpec with ControllerSpec {

  private val viewModel = VatRegisteredUkViewModel

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  "titleAndHeaderLabel" should {

    "display correct message for individual" in {
      viewModel.titleAndHeadingLabel(
        isIndividualSubscriptionFlow = true,
        isPartnership = false,
        UserLocation.Uk
      ) shouldBe "Are you VAT registered in the UK?"
    }

    "display correct message for partnership" in {
      viewModel.titleAndHeadingLabel(
        isIndividualSubscriptionFlow = false,
        isPartnership = true,
        UserLocation.Uk
      ) shouldBe "Is your partnership VAT registered in the UK?"
    }

    "display correct message for other orgType" in {
      viewModel.titleAndHeadingLabel(
        isIndividualSubscriptionFlow = false,
        isPartnership = false,
        UserLocation.Uk
      ) shouldBe "Is your organisation VAT registered in the UK?"
    }
  }

  "formAction" should {
    "call VatRegisteredUkController submit" in servicesToTest.foreach { service =>
      viewModel.formAction(
        isInReviewMode = false,
        service
      ).url shouldBe s"/customs-registration-services/${service.code}/register/vat-registered-uk"
    }
  }

}
