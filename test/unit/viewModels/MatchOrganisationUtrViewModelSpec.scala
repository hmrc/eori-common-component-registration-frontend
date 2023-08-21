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
import play.twirl.api.Html
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{
  CharityPublicBodyNotForProfitId,
  CompanyId,
  IndividualId,
  SoleTraderId,
  ThirdCountryIndividualId,
  ThirdCountryOrganisationId,
  ThirdCountrySoleTraderId
}
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.MatchOrganisationUtrViewModel
import util.ControllerSpec

class MatchOrganisationUtrViewModelSpec extends UnitSpec with ControllerSpec {

  val utrLinkText = Html("some link text")

  val utrLink = Html("a link")

  val headerAndTitleExpected = Seq[(String, String)](
    (ThirdCountryOrganisationId, messages("cds.matching.row-organisation.utr.title-and-heading")),
    (ThirdCountryIndividualId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (ThirdCountrySoleTraderId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (IndividualId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (SoleTraderId, messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")),
    (CharityPublicBodyNotForProfitId, messages("cds.matching.organisation.utr.title-and-heading"))
  )

  val hintTextExpected = Seq[(String, Html)](
    (ThirdCountryOrganisationId, Html(None)),
    (ThirdCountryIndividualId, Html(None)),
    (ThirdCountrySoleTraderId, Html(None)),
    (IndividualId, Html(messages("cds.matching.row-sole-trader-individual.utr.paragraph", utrLink))),
    (SoleTraderId, Html(messages("cds.matching.row-sole-trader-individual.utr.paragraph", utrLink))),
    (CompanyId, Html(messages("cds.matching.organisation.utr.paragraph", utrLink))),
    (CharityPublicBodyNotForProfitId, utrLinkText)
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
        MatchOrganisationUtrViewModel.headerAndTitle(expectedHeaderAndTitle._1) shouldBe expectedHeaderAndTitle._2
      }
    }
  }

  "hintText" should {
    hintTextExpected.foreach { expectedHintText =>
      s"return correct hint for cdsOrgType: ${expectedHintText._1}" in {
        MatchOrganisationUtrViewModel.hintText(expectedHintText._1, utrLinkText, utrLink) shouldBe expectedHintText._2
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
