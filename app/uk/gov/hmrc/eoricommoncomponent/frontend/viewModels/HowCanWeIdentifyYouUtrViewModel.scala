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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, CorporateBody, EtmpOrganisationType}

object HowCanWeIdentifyYouUtrViewModel {

  def forHintMessage(orgType: EtmpOrganisationType)(implicit messages: Messages): String =
    if (orgType == CorporateBody) messages("subscription-journey.how-confirm-identity.utr.hint")
    else messages("cds.matching.partnership.utr.hint")

  def heading(orgType: String) = orgType match {
    case CdsOrganisationType.ThirdCountryOrganisationId | CdsOrganisationType.CharityPublicBodyNotForProfitId =>
      "subscription-journey.how-confirm-identity.utr.row.org.heading"
    case _ => "subscription-journey.how-confirm-identity.utr.heading"
  }
    def forMessage(orgType: EtmpOrganisationType)(implicit messages: Messages): String =
    if (orgType == CorporateBody) messages("subscription-journey.how-confirm-identity.utr.row.org.message")
    else messages("subscription-journey.how-confirm-identity.utr.row.message")

    def forSubHeading(orgType: EtmpOrganisationType)(implicit messages: Messages): String =
    if (orgType == CorporateBody) messages("subscription-journey.how-confirm-identity.utr.row.org.subheading")
    else messages("subscription-journey.how-confirm-identity.utr.row.subheading")

    def forLinkText(orgType: EtmpOrganisationType)(implicit messages: Messages): String =
    if (orgType == CorporateBody) messages("subscription-journey.how-confirm-identity.utr.para")
    else messages("subscription-journey.how-confirm-identity.utr.self.para")
}
