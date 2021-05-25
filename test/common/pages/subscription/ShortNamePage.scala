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

sealed trait ShortNamePageCommon extends WebPage {

  override val title = "Does your organisation use a shortened name?"

  val formId: String = "shortNameForm"

  val shortNameLabelXpath = "//*label[@for='short-name']"

  val headingXpath = "//h1"

  val continueButtonXpath = "//*[@class='button']"
}

trait ShortNamePage extends ShortNamePageCommon {}

object ShortNamePage extends ShortNamePage
