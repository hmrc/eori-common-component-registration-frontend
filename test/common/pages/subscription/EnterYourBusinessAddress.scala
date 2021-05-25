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
import org.openqa.selenium.By

trait EnterYourBusinessAddress extends WebPage {
  override val title = "Enter your organisation address"

  val url: String = "/customs-enrolment-services/subscribe/address"

  val suggestionXpath: By = By.xpath("//*[@id='suggestions-list']")

}

object EnterYourBusinessAddress extends EnterYourBusinessAddress
