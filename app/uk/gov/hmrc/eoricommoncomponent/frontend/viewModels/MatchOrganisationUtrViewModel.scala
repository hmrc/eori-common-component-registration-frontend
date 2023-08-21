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

package uk.gov.hmrc.eoricommoncomponent.frontend.viewModels

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._

object MatchOrganisationUtrViewModel {

  def headerAndTitle(cdsOrgTypeString: String)(implicit messages: Messages): String = cdsOrgTypeString match {
    case ThirdCountryOrganisationId =>
      messages("cds.matching.row-organisation.utr.title-and-heading")
    case orgType if individualOrganisationIds.contains(orgType) =>
      messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")
    case _ => messages("cds.matching.organisation.utr.title-and-heading")
  }

  def hintText(cdsOrgTypeString: String, utrLinkText: Html, utrLink: Html)(implicit messages: Messages): Html =
    cdsOrgTypeString match {
      case CharityPublicBodyNotForProfitId                                                  => utrLinkText
      case ThirdCountryOrganisationId | ThirdCountryIndividualId | ThirdCountrySoleTraderId => Html(None)
      case IndividualId | SoleTraderId =>
        Html(messages("cds.matching.row-sole-trader-individual.utr.paragraph", utrLink))
      case _ => Html(messages("cds.matching.organisation.utr.paragraph", utrLink))
    }

  def isNotSoleTrader(cdsOrgTypeString: String) = cdsOrgTypeString match {
    case orgType if individualOrganisationIds.contains(orgType) => false
    case _                                                      => true
  }

}
