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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{
  CharityPublicBodyNotForProfitId,
  IndividualId,
  SoleTraderId,
  ThirdCountryIndividualId,
  ThirdCountryOrganisationId,
  ThirdCountrySoleTraderId
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.MatchOrganisationUtrViewModel
import util.ControllerSpec

class MatchOrganisationUtrViewModelSpec extends UnitSpec with ControllerSpec {

  val headerAndTitleExpected = Seq[(String, String)](
    (ThirdCountryOrganisationId, messages("cds.matching.row-organisation.utr.title-and-heading")),
    (ThirdCountryIndividualId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (ThirdCountrySoleTraderId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (IndividualId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (SoleTraderId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (CharityPublicBodyNotForProfitId, messages("cds.matching.row-charity-public-body.utr.title-and-heading"))
  )

  val isNotSoleTraderExpected = Seq[(String, Boolean)](
    (SoleTraderId, false),
    (IndividualId, false),
    (ThirdCountryIndividualId, false),
    (ThirdCountrySoleTraderId, false),
    (CharityPublicBodyNotForProfitId, true)
  )

  "headerAndTitle" should {
    headerAndTitleExpected.foreach { expectedHeaderAndTitle =>
      s"return correct header and title for cdsOrgType: ${expectedHeaderAndTitle._1}" in {
        MatchOrganisationUtrViewModel.headerAndTitle(
          expectedHeaderAndTitle._1,
          UserLocation.Uk
        ) shouldBe expectedHeaderAndTitle._2
      }
    }
  }

  "isNotSoleTrader" should {
    isNotSoleTraderExpected.foreach { expectedIsNotSoleTrader =>
      s"return correct hint for cdsOrgType: ${expectedIsNotSoleTrader._1}" in {
        MatchOrganisationUtrViewModel.isNotSoleTrader(expectedIsNotSoleTrader._1) shouldBe expectedIsNotSoleTrader._2
      }
    }
  }
}
