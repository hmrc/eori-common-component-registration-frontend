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

package uk.gov.hmrc.eoricommoncomponent.frontend.forms

import play.api.data.Form
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.FormUtils.{mandatoryString, oneOf}

import javax.inject.Singleton

@Singleton
class ConfirmIndividualTypeForm() {

  private val validConfirmIndividualTypes = Set(CdsOrganisationType.SoleTraderId, CdsOrganisationType.IndividualId)

  private val confirmIndividualTypeError = "cds.confirm-individual-type.error.individual-type"

  def form(): Form[CdsOrganisationType] = {
    Form(
      "individual-type" -> mandatoryString(confirmIndividualTypeError)(oneOf(validConfirmIndividualTypes))
        .transform[CdsOrganisationType](CdsOrganisationType.forId, _.id)
    )
  }
}
