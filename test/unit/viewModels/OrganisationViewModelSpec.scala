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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.OrganisationViewModel
import util.ControllerSpec

class OrganisationViewModelSpec extends UnitSpec with ControllerSpec {

  "OrganisationViewModel" should {

    "return valid options for UK user location" in {

      val selectedUserLocation = Some(UserLocation.Uk)
      val viewModel            = OrganisationViewModel

      val options = viewModel.validOptions(selectedUserLocation)

      options should contain theSameElementsAs Seq(
        CdsOrganisationType.CompanyId     -> messages("cds.matching.organisation-type.radio.company.label"),
        CdsOrganisationType.SoleTraderId  -> messages("cds.matching.organisation-type.radio.sole-trader.label"),
        CdsOrganisationType.IndividualId  -> messages("cds.matching.organisation-type.radio.individual.label"),
        CdsOrganisationType.PartnershipId -> messages("cds.matching.organisation-type.radio.partnership.label"),
        CdsOrganisationType.LimitedLiabilityPartnershipId -> messages(
          "cds.matching.organisation-type.radio.limited-liability-partnership.label"
        ),
        CdsOrganisationType.CharityPublicBodyNotForProfitId -> messages(
          "cds.matching.organisation-type.radio.charity-public-body-not-for-profit.label"
        ),
        CdsOrganisationType.EmbassyId -> messages("cds.matching.organisation-type.radio.embassy.label")
      )
    }

    "return valid options for third-country user location" in {
      val selectedUserLocation = Some(UserLocation.ThirdCountry)
      val viewModel            = OrganisationViewModel

      val options = viewModel.validOptions(selectedUserLocation)

      options should contain theSameElementsAs Seq(
        CdsOrganisationType.ThirdCountryOrganisationId -> messages(
          "cds.matching.organisation-type.radio.organisation.label"
        ),
        CdsOrganisationType.ThirdCountrySoleTraderId -> messages(
          "cds.matching.organisation-type.radio.sole-trader.label"
        ),
        CdsOrganisationType.ThirdCountryIndividualId -> messages(
          "cds.matching.organisation-type.radio.individual.label"
        )
      )
    }

    "return valid options for third-country including EU user location" in {
      val selectedUserLocation = Some(UserLocation.ThirdCountryIncEU)
      val viewModel            = OrganisationViewModel

      val options = viewModel.validOptions(selectedUserLocation)

      options should contain theSameElementsAs Seq(
        CdsOrganisationType.ThirdCountryOrganisationId -> messages(
          "cds.matching.organisation-type.radio.organisation.label"
        ),
        CdsOrganisationType.ThirdCountrySoleTraderId -> messages(
          "cds.matching.organisation-type.radio.sole-trader.label"
        ),
        CdsOrganisationType.ThirdCountryIndividualId -> messages(
          "cds.matching.organisation-type.radio.individual.label"
        )
      )
    }

    "return valid options for any other location user location" in {
      val selectedUserLocation = Some(UserLocation.Islands)
      val viewModel            = OrganisationViewModel

      val options = viewModel.validOptions(selectedUserLocation)

      options should contain theSameElementsAs Seq(
        CdsOrganisationType.CompanyId     -> messages("cds.matching.organisation-type.radio.company.label"),
        CdsOrganisationType.SoleTraderId  -> messages("cds.matching.organisation-type.radio.sole-trader.label"),
        CdsOrganisationType.IndividualId  -> messages("cds.matching.organisation-type.radio.individual.label"),
        CdsOrganisationType.PartnershipId -> messages("cds.matching.organisation-type.radio.partnership.label"),
        CdsOrganisationType.LimitedLiabilityPartnershipId -> messages(
          "cds.matching.organisation-type.radio.limited-liability-partnership.label"
        ),
        CdsOrganisationType.CharityPublicBodyNotForProfitId -> messages(
          "cds.matching.organisation-type.radio.charity-public-body-not-for-profit.label"
        ),
        CdsOrganisationType.EmbassyId -> messages("cds.matching.organisation-type.radio.embassy.label")
      )
    }

    "return hint text options for non-UK user location" in {
      val userLocation = Some(UserLocation.ThirdCountry)
      val viewModel    = OrganisationViewModel
      val result       = viewModel.hintTextOptions(userLocation)(messages)

      val expectedResult = Seq(
        CdsOrganisationType.ThirdCountryOrganisationId -> messages(
          "cds.matching.organisation-type.radio.organisation.hint-text"
        ),
        CdsOrganisationType.ThirdCountrySoleTraderId -> messages(
          "cds.matching.organisation-type.radio.sole-trader.hint-text"
        ),
        CdsOrganisationType.ThirdCountryIndividualId -> messages(
          "cds.matching.organisation-type.radio.individual.hint-text"
        )
      )
      result shouldBe expectedResult

    }

    "return hint text options for UK-option user location" in {
      val userLocation = Some(UserLocation.Uk)
      val viewModel    = OrganisationViewModel

      val result = viewModel.hintTextOptions(userLocation)(messages)

      val expectedResult = Seq.empty
      result shouldBe expectedResult

    }

  }
}
