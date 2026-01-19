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

import play.api.data.{Form, Mapping}
import play.api.data.Forms.{mapping, text}
import uk.gov.hmrc.eoricommoncomponent.frontend.DateConverter
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameDobMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.mappings.Mappings

import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class NameDobFormProvider() extends Mappings {

  val enterNameDobForm: Form[NameDobMatchModel] =
    Form(
      mapping(
        "first-name" -> text.verifying(validName("first-name")),
        "last-name"  -> text.verifying(validName("last-name")),
        validateDateOfBirth
      )(NameDobMatchModel.apply)(nameDOBMatchModel => Some(nameDOBMatchModel.firstName, nameDOBMatchModel.lastName, nameDOBMatchModel.dateOfBirth))
    )

  private def validateDateOfBirth: (String, Mapping[LocalDate]) = {

    val minimumDate = LocalDate.of(DateConverter.earliestYearDateOfBirth, 1, 1)
    val today = LocalDate.now()

    "date-of-birth" -> localDate(emptyKey = "dob.error.empty-date", invalidKey = "dob.error.invalid-date")
      .verifying(
        minDate(minimumDate, "dob.error.minMax", DateConverter.earliestYearDateOfBirth.toString)
      )
      .verifying(maxDate(today, "dob.error.minMax", DateConverter.earliestYearDateOfBirth.toString))
  }
}
