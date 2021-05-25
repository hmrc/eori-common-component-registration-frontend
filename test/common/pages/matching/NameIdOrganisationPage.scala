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

object NameIdOrganisationPage extends WebPage {
  val registerWithNameAndAddressLink       = "//*[@id='address-link']"
  val registerWithNameAndAddressLinkAnchor = s"$registerWithNameAndAddressLink/a"
  val fieldLevelErrorName                  = "//*[@id='name-outer']//span[@class='error-message']"
  val fieldLevelErrorUtr                   = "//*[@id='utr-outer']//span[@class='error-message']"
  val labelForNameXpath                    = "//*[@id='name-outer']//label"
  val labelForUtrXpath                     = "//*[@id='utr-outer']/label"
  val hintForUtrHintTextXpath              = "//*[@id='utr-hint']"
  val linkInUtrHintTextXpath               = "//*[@id='utr-hint']/a"

  override val title = "What are your business details?"
}
