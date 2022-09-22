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

package common.pages

import org.openqa.selenium.By
import org.scalatest.matchers.must.Matchers

trait WebPage extends Matchers {

  val backLinkXPath: String          = "//*[@id='back-link']"
  val pageLevelErrorSummaryListXPath = "//ul[@class='govuk-list govuk-error-summary__list']"
  val countryCodeCss                 = By.xpath("//*[@id='countryCode']")
  val countrySuggestion              = By.xpath("//*[@id='countryCode__option--0']")

  protected def fieldLevelErrorXpath(fieldName: String) =
    s"//p[contains(@id, '$fieldName-error') and contains(@class, 'error-message')]"

  val title: String

}
