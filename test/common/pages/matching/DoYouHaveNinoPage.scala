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

trait DoYouHaveNinoPage extends WebPage {

  val fieldLevelErrorNino = "//span[@id='nino-error' and @class='govuk-error-message']"

  override val title = "Do you have a National Insurance number"

  val yesRadioButton = "//*[@id='have-nino-true']"
  val yesLabel       = "//label[@for='have-nino-true']"

  val noRadioButton = "//*[@id='have-nino-false']"
  val noLabel       = "//label[@for='have-nino-false']"

  val ninoLabelBold = "//label[@class='form-label-bold']"
  val ninoHint      = "//*[@id='nino-hint']"
  val ninoInput     = "//*[@id='nino']"

}

object DoYouHaveNinoPage extends DoYouHaveNinoPage

object SubscriptionNinoPage extends DoYouHaveNinoPage {
  val formId = "SubscriptionUtrForm"
}
