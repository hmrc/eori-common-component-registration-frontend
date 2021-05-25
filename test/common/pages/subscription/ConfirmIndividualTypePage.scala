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

object ConfirmIndividualTypePage extends WebPage {

  override val title = "Sole trader or individual"

  val fieldLevelErrorIndividualTypeXPath = "//*[@id='individual-type-fieldset']//span[@class='error-message']"

  val formId = "confirm-individual-type-form"

  val soleTraderLabelXpath  = "//*label[@for='individual-type-sole-trader']"
  val optionSoleTraderXpath = "//*[@id='individual-type-sole-trader']"

  val individualLabelXpath  = "//*label[@for='individual-type-individual']"
  val optionIndividualXpath = "//*[@id='individual-type-individual']"

}
