/*
 * Copyright 2025 HM Revenue & Customs
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
  val fieldLevelErrorName                  = "//p[@id='name-error' and @class='govuk-error-message']"
  val fieldLevelErrorUtr                   = "//p[@id='utr-error' and @class='govuk-error-message']"
  val labelForNameXpath                    = "//div[@class='govuk-form-group']//label[@for='name']"
  val labelForUtrXpath                     = "//div[@class='govuk-form-group']//label[@for='utr']"
  val hintForUtrHintTextXpath              = "//*[@id='utr-hint']"
  val linkInUtrHintTextXpath               = "//a[@id='utrLink']"

  override val title = "What are your business details?"
}
