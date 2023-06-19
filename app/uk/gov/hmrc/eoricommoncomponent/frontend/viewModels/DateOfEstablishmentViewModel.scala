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

import play.api.data.Form
import java.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CorporateBody,
  EtmpOrganisationType,
  LLP,
  Partnership,
  UnincorporatedBody
}

object DateOfEstablishmentViewModel {

  def introText(orgType: EtmpOrganisationType)(implicit messages: Messages): String =
    orgType match {
      case LLP                => messages("cds.subscription.llp.date-established.label")
      case UnincorporatedBody => messages("cds.subscription.llp.date-established.charity-label")
      case Partnership        => messages("cds.subscription.business.date-established.label.partnership")
      case _                  => messages("cds.subscription.business.date-established.label")
    }

  def updateFormErrors(dateForm: Form[LocalDate]): Form[LocalDate] =
    dateForm.copy(errors = DateConverter.updateDateOfEstablishmentErrors(dateForm.errors))

  def headerAndTitle(orgType: EtmpOrganisationType, isRestOfWorldJourney: Boolean): String =
    (orgType, isRestOfWorldJourney) match {
      case (Partnership | LLP, _) => "cds.subscription.partnership.date-of-establishment.title-and-heading"
      case (CorporateBody, false) => "cds.subscription.date-of-establishment.company.title-and-heading"
      case (_, _)                 => "cds.subscription.date-of-establishment.title-and-heading"
    }

}
