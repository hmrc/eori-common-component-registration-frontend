/*
 * Copyright 2022 HM Revenue & Customs
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

abstract class NameDateOfBirthPage extends WebPage {

  val formId: String = "nameDobForm"

  val firstName = "//*[@id='first-name']"

  val lastName = "//*[@id='last-name']"

  val middleName = "//*[@id='middle-name']"

  val fieldLevelErrorFirstName = "//span[@id='first-name-error' and @class='govuk-error-message']"

  val fieldLevelErrorLastName = "//span[@id='last-name-error' and @class='govuk-error-message']"

  val fieldLevelErrorDateOfBirth = "//span[@id='date-of-birth-error' and @class='govuk-error-message']"

  override val title = "Enter your details"

}

object NameDateOfBirthPage extends NameDateOfBirthPage {}
