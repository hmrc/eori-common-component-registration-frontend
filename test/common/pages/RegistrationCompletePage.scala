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

package common.pages

class RegistrationCompletePage extends RegistrationOutcomePage("complete") with XPathRegistrationOutcomePage {

  val eoriXpath: String = "//*[@id='eori-number']"

  override val title = "Application complete"

  val LeaveFeedbackLinkXpath = "//a[@id='feedback_link']"
  val DownloadEoriLinkXpath  = "//a[@id='download-eori']"
}

object RegistrationCompletePage extends RegistrationCompletePage
