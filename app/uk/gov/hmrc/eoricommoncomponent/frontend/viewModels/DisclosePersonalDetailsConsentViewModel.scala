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
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData

import javax.inject.Inject

case class DisclosePersonalDetailsConsentViewModel @Inject() (requestSessionData: RequestSessionData) {

  def textPara2()(implicit messages: Messages, request: Request[AnyContent]): String = {
    val org = requestSessionData.userSelectedOrganisationType.getOrElse(CdsOrganisationType.Individual)
    org match {
      case CdsOrganisationType.Company =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.org.para2")
      case CdsOrganisationType.Partnership | CdsOrganisationType.LimitedLiabilityPartnership =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.partnership.para2")
      case CdsOrganisationType.CharityPublicBodyNotForProfit | CdsOrganisationType.ThirdCountryOrganisation =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.para2")
      case _ => messages("ecc.subscription.organisation-disclose-personal-details-consent.individual.para2")
    }
  }

  def questionLabel()(implicit messages: Messages, request: Request[AnyContent]): String = {
    val org = requestSessionData.userSelectedOrganisationType.getOrElse(CdsOrganisationType.Individual)
    org match {
      case CdsOrganisationType.Company =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.org.question")
      case CdsOrganisationType.Partnership | CdsOrganisationType.LimitedLiabilityPartnership =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.partnership.question")
      case CdsOrganisationType.CharityPublicBodyNotForProfit | CdsOrganisationType.ThirdCountryOrganisation =>
        messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.question")
      case _ => messages("ecc.subscription.organisation-disclose-personal-details-consent.individual.question")
    }
  }

}
