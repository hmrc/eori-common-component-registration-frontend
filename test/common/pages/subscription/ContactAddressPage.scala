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

package common.pages.subscription

import common.pages.WebPage

trait ContactAddressPage extends WebPage {

  override val title = "Do you want us to use this address to send you any information about your application?"

  val fieldLevelErrorYesNoAnswer = "//*[@id='yes-no-answer-error']"

  val addressXpath        = "//*[@id='yes-no-answer-hint']"
  val addressNoXpath      = "//*[@id='yes-no-answer-false']"
  val addressYesXpath     = "//*[@id='yes-no-answer-true']"
  val continueButtonXpath = "//*[@id='continue-button']"
}

object ContactAddressPage extends ContactAddressPage
