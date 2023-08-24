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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.rowIndividualOrganisationIds
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.{CountriesInCountryPicker, IslandsInCountryPicker}

object SixLineAddressViewModel {

  def postCodeLabel(countriesInCountryPicker: CountriesInCountryPicker)(implicit messages: Messages) =
    countriesInCountryPicker match {
      case IslandsInCountryPicker => messages("cds.matching.organisation-address.postcode")
      case _                      => messages("cds.matching.organisation-address.postcode-optional")
    }

  def headerLabel(cdsOrgType: String)(implicit messages: Messages) = cdsOrgType match {
    case _ if rowIndividualOrganisationIds.contains(cdsOrgType) => messages("cds.matching.individual-address.header")
    case _                                                      => messages("cds.matching.organisation-address.header")
  }

}
