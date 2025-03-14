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

package uk.gov.hmrc.eoricommoncomponent.frontend.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation

object MatchOrganisationUtrViewModel {

  def headerAndTitle(cdsOrgTypeString: String, userLocation: UserLocation)(implicit messages: Messages): String =
    cdsOrgTypeString match {
      case ThirdCountryOrganisationId =>
        messages("cds.matching.row-organisation.utr.title-and-heading")
      case orgType if individualOrganisationIds.contains(orgType) =>
        messages("ecc.matching.row-sole-trader-individual.utr.title-and-heading")
      case CharityPublicBodyNotForProfitId =>
        if (userLocation == UserLocation.Iom) {
          messages("cds.matching.row-charity-public-body.utr.iom.title-and-heading")
        } else {
          messages("cds.matching.row-charity-public-body.utr.title-and-heading")
        }
      case _ => messages("cds.matching.organisation.utr.title-and-heading")
    }

  def isNotSoleTrader(cdsOrgTypeString: String) = !individualOrganisationIds.contains(cdsOrgTypeString)
}
