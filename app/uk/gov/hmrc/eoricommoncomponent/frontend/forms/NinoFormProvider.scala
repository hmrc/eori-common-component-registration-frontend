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
import play.api.data.Forms.{mapping, text}
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter.earliestYearDateOfBirth
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NinoMatch
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class NinoFormProvider() extends Mappings {

  val ninoForm: Form[NinoMatch] =
    Form(
      mapping(
        "first-name"    -> text.verifying(validName("first-name")),
        "last-name"     -> text.verifying(validName("last-name")),
        "date-of-birth" ->
          localDate(emptyKey = "dob.error.empty-date", invalidKey = "dob.error.invalid-date")
            .verifying(minDate(LocalDate.of(earliestYearDateOfBirth, 1, 1), "dob.error.minMax", earliestYearDateOfBirth.toString))
            .verifying(maxDate(LocalDate.now(), "dob.error.minMax", earliestYearDateOfBirth.toString)),
        "nino"          -> text.verifying(validNino)
      )(NinoMatch.apply)(NinoMatch.unapply)
    )
}
