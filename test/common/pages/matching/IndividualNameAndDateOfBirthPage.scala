/*
 * Copyright 2021 HM Revenue & Customs
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

import common.pages.WebPage

trait IndividualNameAndDateOfBirthXPath extends AddressPageFactoring {
  val fieldLevelErrorGivenName   = "//*[@id='given-name-outer']//span[@class='error-message']"
  val fieldLevelErrorMiddleName  = "//*[@id='middle-name-outer']//span[@class='error-message']"
  val fieldLevelErrorFamilyName  = "//*[@id='family-name-outer']//span[@class='error-message']"
  val fieldLevelErrorDateOfBirth = "//*[@id='date-of-birth-fieldset']//span[@class='error-message']"

  val givenNameElement   = "//*[@id='given-name']"
  val middleNameElement  = "//*[@id='middle-name']"
  val familyNameElement  = "//*[@id='family-name']"
  val dateOfBirthElement = "//*[@id='date-of-birth-fieldset']"
  val dobDayElement      = "//*[@id='date-of-birth.day']"
  val dobMonthElement    = "//*[@id='date-of-birth.month']"
  val dobYearElement     = "//*[@id='date-of-birth.year']"
}

abstract class IndividualNameAndDateOfBirthPage(val organisationType: String)
    extends WebPage with IndividualNameAndDateOfBirthXPath with IndividualNameDateFields {

  override val pageLevelErrorSummaryListXPath: String = AddressPageFactoring.PageLevelErrorSummaryListXPath

  def formElement: String

  override val title = "Enter your details"

}
