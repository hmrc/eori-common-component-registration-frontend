/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation

object OrganisationViewModel {

  def validOptions(userLocation: Option[UserLocation], allowNoIdJourney: Boolean)(implicit
    messages: Messages
  ): Seq[(String, String)] = {

    lazy val ukOptionsFirstScreen = Seq(
      CdsOrganisationType.CompanyId                       -> messages("cds.matching.organisation-type.radio.company.label"),
      CdsOrganisationType.SoleTraderId                    -> messages("cds.matching.organisation-type.radio.sole-trader.label"),
      CdsOrganisationType.IndividualId                    -> messages("cds.matching.organisation-type.radio.individual.label"),
      CdsOrganisationType.PartnershipId                   -> messages("cds.matching.organisation-type.radio.partnership.label"),
      CdsOrganisationType.LimitedLiabilityPartnershipId   -> messages(
        "cds.matching.organisation-type.radio.limited-liability-partnership.label"
      ),
      CdsOrganisationType.CharityPublicBodyNotForProfitId -> messages(
        "cds.matching.organisation-type.radio.charity-public-body-not-for-profit.label"
      )
    )

    lazy val iomOptions = Seq(
      CdsOrganisationType.CompanyId                       -> messages("cds.matching.organisation-type.radio.company.label"),
      CdsOrganisationType.SoleTraderId                    -> messages("cds.matching.organisation-type.radio.sole-trader.label"),
      CdsOrganisationType.IndividualId                    -> messages("cds.matching.organisation-type.radio.individual.label"),
      CdsOrganisationType.PartnershipId                   -> messages("cds.matching.organisation-type.radio.partnership.label"),
      CdsOrganisationType.LimitedLiabilityPartnershipId   -> messages(
        "cds.matching.organisation-type.radio.limited-liability-partnership.label"
      ),
      CdsOrganisationType.CharityPublicBodyNotForProfitId -> messages(
        "cds.matching.organisation-type.radio.charity-public-body-not-for-profit.label"
      )
    )

    lazy val thirdCountryOptions = Seq(
      CdsOrganisationType.ThirdCountryOrganisationId -> messages(
        "cds.matching.organisation-type.radio.organisation.label"
      ),
      CdsOrganisationType.ThirdCountrySoleTraderId   -> messages(
        "cds.matching.organisation-type.radio.sole-trader.label"
      ),
      CdsOrganisationType.ThirdCountryIndividualId   -> messages("cds.matching.organisation-type.radio.individual.label")
    )

    userLocation match {
      case Some(UserLocation.Iom) => iomOptions
      case Some(UserLocation.ThirdCountry) | Some(UserLocation.ThirdCountryIncEU) => thirdCountryOptions
      case _ =>
        if (allowNoIdJourney) {
          ukOptionsFirstScreen.:+(
            CdsOrganisationType.EmbassyId -> messages("cds.matching.organisation-type.radio.embassy.label")
          )
        } else {
          ukOptionsFirstScreen
        }
    }
  }

  def hintTextOptions(userLocation: Option[UserLocation])(implicit messages: Messages): Seq[(String, String)] = {

    lazy val nonUkOptionHints = Seq(
      CdsOrganisationType.ThirdCountryOrganisationId -> messages(
        "cds.matching.organisation-type.radio.organisation.hint-text"
      ),
      CdsOrganisationType.ThirdCountrySoleTraderId   -> messages(
        "cds.matching.organisation-type.radio.sole-trader.hint-text"
      ),
      CdsOrganisationType.ThirdCountryIndividualId   -> messages(
        "cds.matching.organisation-type.radio.individual.hint-text"
      )
    )

    if (userLocation.contains(UserLocation.Uk)) Seq.empty else nonUkOptionHints
  }

}
