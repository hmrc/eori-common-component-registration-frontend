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

package common.pages

object RegistrationProcessingPage extends WebPage {
  val processedDateXpath = "//*[@id='processed-date']"
  override val title     = "The ATaR application is being processed"
  val heading            = "The ATaR application for orgName is being processed"
  val individualHeading  = "The ATaR application for Name is being processed"
  val pageHeadingXpath   = "//*[@id='page-heading']"
}
