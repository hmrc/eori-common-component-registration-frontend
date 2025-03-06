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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  CorporateBody,
  EtmpOrganisationType,
  UnincorporatedBody
}

object HowCanWeIdentifyYouUtrViewModel {

  def getPageContent(orgType: EtmpOrganisationType, cdsOrgType: String = "")(implicit
    messages: Messages
  ): Map[String, String] =
    Map(
      "hintMessage"    -> hintMessage(orgType),
      "headingMessage" -> headingMessage(cdsOrgType),
      "message"        -> mainMessage(orgType),
      "subHeading"     -> subheading(orgType, cdsOrgType),
      "linkText"       -> linkText(orgType)
    )

  private def hintMessage(orgType: EtmpOrganisationType)(implicit messages: Messages) = {
    if (orgType == CorporateBody)
      messages("subscription-journey.how-confirm-identity.utr.hint")
    else if (orgType == UnincorporatedBody)
      messages("cds.matching.charity-public-body.utr.hint")
    else
      messages("cds.matching.partnership.utr.hint")
  }

  private def headingMessage(cdsOrgType: String)(implicit messages: Messages) = {
    cdsOrgType match {
      case CdsOrganisationType.ThirdCountryOrganisationId =>
        messages("subscription-journey.how-confirm-identity.utr.row.org.heading")
      case CdsOrganisationType.CharityPublicBodyNotForProfitId =>
        messages("subscription-journey.how-confirm-identity.utr.row.charity-public-body.heading")
      case _ => messages("subscription-journey.how-confirm-identity.utr.heading")
    }
  }

  private def mainMessage(orgType: EtmpOrganisationType)(implicit messages: Messages) = {
    if (orgType == CorporateBody)
      messages("subscription-journey.how-confirm-identity.utr.row.org.message")
    else if (orgType == UnincorporatedBody)
      messages("subscription-journey.how-confirm-identity.utr.row.charity-public-body.message")
    else
      messages("subscription-journey.how-confirm-identity.utr.row.message")
  }

  private def subheading(orgType: EtmpOrganisationType, cdsOrgType: String)(implicit messages: Messages) = {
    if (orgType == CorporateBody)
      messages("subscription-journey.how-confirm-identity.utr.row.org.subheading")
    else if (cdsOrgType == CdsOrganisationType.CharityPublicBodyNotForProfitId)
      messages("subscription-journey.how-confirm-identity.utr.row.charity-public-body.subheading")
    else
      messages("subscription-journey.how-confirm-identity.utr.row.subheading")
  }

  private def linkText(orgType: EtmpOrganisationType)(implicit messages: Messages) = {
    if (orgType == CorporateBody)
      messages("subscription-journey.how-confirm-identity.utr.para")
    else
      messages("subscription-journey.how-confirm-identity.utr.self.para")
  }

}
