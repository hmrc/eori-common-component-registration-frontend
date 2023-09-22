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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.CompanyId
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation

object SicCodeViewModel {

  def detailsContent(cdsOrgType: Option[CdsOrganisationType])(implicit messages: Messages): String =
    messages(s"cds.subscription.sic.page.help.para3.${cdsOrgType.fold(CompanyId)(_.id)}")

  def dontKnowSicDropDownContent(cdsOrgType: Option[CdsOrganisationType])(implicit messages: Messages): String =
    messages(s"cds.subscription.sic.details.${cdsOrgType.fold(CompanyId)(_.id)}")

  def secondHeading(cdsOrgType: Option[CdsOrganisationType], selectedUserLocation: Option[String])(implicit
    messages: Messages
  ): String =
    selectedUserLocation match {
      case Some(UserLocation.ThirdCountryIncEU | UserLocation.ThirdCountry) =>
        messages("cds.subscription.sic.para2.row")
      case _ => messages(s"cds.subscription.sic.description.para2.${cdsOrgType.fold(CompanyId)(_.id)}")
    }

  def hintTextForSic(selectedUserLocation: Option[String])(implicit messages: Messages): String =
    selectedUserLocation match {
      case Some(UserLocation.ThirdCountryIncEU | UserLocation.ThirdCountry) => messages("cds.subscription.sic.hint.row")
      case _                                                                => messages("cds.subscription.sic.hint.uk")
    }

}
