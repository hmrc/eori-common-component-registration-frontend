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

package common.pages.migration

import common.pages.WebPage
import java.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameDobMatchModel

trait NameDobSoleTraderPage extends WebPage {

  override val title = "Enter your details"

  val formId = "subscriptionNameDobForm"

  val pageTitleXPath = "//*[@id=\"content\"]/div/div/h1"

  val firstNameFieldXPath           = "//*[@id='first-name']"
  val firstNameFieldLevelErrorXPath = "//*[@id='first-name-outer']//span[@class='error-message']"
  val firstNameFieldLabel           = "First name"
  val firstNameFieldId              = "first-name"
  val firstNameFieldName            = "first-name"

  val lastNameFieldXPath           = "//*[@id='last-name']"
  val lastNameFieldLevelErrorXPath = "//*[@id='last-name-outer']//span[@class='error-message']"
  val lastNameFieldLabel           = "Last name"
  val lastNameFieldId              = "last-name"
  val lastNameFieldName            = "last-name"

  val middleNameFieldName = "middle-name"

  val dobFieldLevelErrorXPath    = "//*[@id='date-of-birth-fieldset']//span[@class='error-message']"
  val dateOfBirthDayFieldXPath   = "//*[@id='date-of-birth.day']"
  val dateOfBirthMonthFieldXPath = "//*[@id='date-of-birth.month']"
  val dateOfBirthYearFieldXPath  = "//*[@id='date-of-birth.year']"

  val dobFieldName      = "date-of-birth"
  val dobDayFieldName   = "date-of-birth.day"
  val dobMonthFieldName = "date-of-birth.month"
  val dobYearFieldName  = "date-of-birth.year"

  val continueButtonXpath = "//*[@class='button']"

  val filledValues = NameDobMatchModel(
    firstName = "Test First Name",
    middleName = Some("Test Middle Name"),
    lastName = "Test Last Name",
    dateOfBirth = LocalDate.of(1983, 9, 3)
  )

}

object NameDobSoleTraderPage extends NameDobSoleTraderPage
