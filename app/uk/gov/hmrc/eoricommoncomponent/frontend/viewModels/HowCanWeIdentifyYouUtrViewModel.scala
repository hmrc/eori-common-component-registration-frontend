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

  def getPageContent(orgType: EtmpOrganisationType, cdsOrgType: String = "")(implicit
    messages: Messages
  ): Map[String, String] = {
    val hintMessage =
      if (orgType == CorporateBody)
        messages("subscription-journey.how-confirm-identity.utr.hint")
      else
        messages("cds.matching.partnership.utr.hint")

    val headingMessage = cdsOrgType match {
      case CdsOrganisationType.ThirdCountryOrganisationId | CdsOrganisationType.CharityPublicBodyNotForProfitId =>
        messages("subscription-journey.how-confirm-identity.utr.row.org.heading")
      case _ => messages("subscription-journey.how-confirm-identity.utr.heading")
    }

    val message =
      if (orgType == CorporateBody)
        messages("subscription-journey.how-confirm-identity.utr.row.org.message")
      else
        messages("subscription-journey.how-confirm-identity.utr.row.message")

    val subHeading =
      if (orgType == CorporateBody)
        messages("subscription-journey.how-confirm-identity.utr.row.org.subheading")
      else
        messages("subscription-journey.how-confirm-identity.utr.row.subheading")

    val linkText =
      if (orgType == CorporateBody)
        messages("subscription-journey.how-confirm-identity.utr.para")
      else
        messages("subscription-journey.how-confirm-identity.utr.self.para")

    Map(
      "hintMessage"    -> hintMessage,
      "headingMessage" -> headingMessage,
      "message"        -> message,
      "subHeading"     -> subHeading,
      "linkText"       -> linkText
    )
  }

}
