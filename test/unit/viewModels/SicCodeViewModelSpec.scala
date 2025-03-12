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

package unit.viewModels

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.SicCodeViewModel
import util.ControllerSpec

class SicCodeViewModelSpec extends UnitSpec with ControllerSpec {

  private val sicViewModel = SicCodeViewModel

  private val organisationsToTest =
    Seq(Company, Partnership, LimitedLiabilityPartnership, CharityPublicBodyNotForProfit, SoleTrader)

  "detailsContent  " should {
    "display correctly " in organisationsToTest.foreach { orgType =>
      sicViewModel.detailsContent(Some(orgType)) shouldBe messages(
        s"cds.subscription.sic.page.help.para3.${orgType.id}"
      )
    }
    "Display for company if No orgType been passed though " in {
      sicViewModel.detailsContent(None) shouldBe messages(s"cds.subscription.sic.page.help.para3.${Company.id}")
    }
  }
  "dontKnowSicDropDownContent" should {
    "display correct messages" in organisationsToTest.foreach { orgType =>
      sicViewModel.dontKnowSicDropDownContent(Some(orgType)) shouldBe messages(
        s"cds.subscription.sic.details.${orgType.id}"
      )
    }
    "Display for company if No orgType been passed though " in {
      sicViewModel.dontKnowSicDropDownContent(None) shouldBe messages(s"cds.subscription.sic.details.${Company.id}")
    }
  }
  "secondHeading" should {
    "display correctly for UK" in {
      sicViewModel.secondHeading(Some(Company), Some(UserLocation.Uk)) shouldBe messages(
        s"cds.subscription.sic.description.para2.$CompanyId"
      )
    }
    "display correctly for ROW" in {
      sicViewModel.secondHeading(
        Some(ThirdCountryOrganisation),
        Some(UserLocation.ThirdCountryIncEU)
      ) shouldBe messages("cds.subscription.sic.para2.row")
    }
  }
  "hintTextForSic" should {
    "display correctly for UK" in {
      sicViewModel.hintTextForSic(Some(UserLocation.Uk)) shouldBe messages("cds.subscription.sic.hint.uk")
    }
    "display correctly for ROW" in {
      sicViewModel.hintTextForSic(Some(UserLocation.ThirdCountryIncEU)) shouldBe messages(
        "cds.subscription.sic.hint.row"
      )
    }
  }

}
