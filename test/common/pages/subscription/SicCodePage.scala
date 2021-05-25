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

package common.pages.subscription

import common.pages.WebPage

sealed trait SicCodePageCommon extends WebPage {

  val shortNameInputXpath = "//*[@id='short-name']"

  override val title = "What is the Standard Industrial Classification (SIC) code for your organisation?"

  val formId: String = "sicCodeform"

  val continueButtonXpath = "//*[@class='button']"

  val headingXpath: String = "//*[@id='page-heading']"

  val sicDescriptionLabelXpath = "//*[@id='description']"

  val sicLabelXpath = "//*label[@for='sic']"
}

trait SicCodePage extends SicCodePageCommon {}

object SicCodePage extends SicCodePage
