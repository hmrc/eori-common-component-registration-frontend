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

import play.api.data.Form
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, EtmpOrganisationType, LLP, Partnership}

import java.time.LocalDate

object DateOfEstablishmentViewModel {

  def updateFormErrors(dateForm: Form[LocalDate]): Form[LocalDate] =
    dateForm.copy(errors = DateConverter.updateDateOfEstablishmentErrors(dateForm.errors))

  def headerAndTitle(orgType: EtmpOrganisationType, isRestOfWorldJourney: Boolean): String =
    (orgType, isRestOfWorldJourney) match {
      case (Partnership, _) => "cds.subscription.partnership.date-of-establishment.title-and-heading"
      case (CorporateBody | LLP, false) => "cds.subscription.date-of-establishment.company.title-and-heading"
      case (_, _) => "cds.subscription.date-of-establishment.title-and-heading"
    }

}
