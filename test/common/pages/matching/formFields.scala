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

package common.pages.matching

trait IndividualNameDateFields {

  val GivenName      = "Given name"
  val givenNameField = "given-name"

  val MiddleName      = "Middle name"
  val middleNameField = "middle-name"

  val FamilyName      = "Family name"
  val familyNameField = "family-name"

  val DateOfBirth           = "Date of birth"
  val dateOfBirthDayField   = "date-of-birth.day"
  val dateOfBirthMonthField = "date-of-birth.month"
  val dateOfBirthYearField  = "date-of-birth.year"

  val dateOfBirthFields = Set(dateOfBirthDayField, dateOfBirthMonthField, dateOfBirthYearField)
}
