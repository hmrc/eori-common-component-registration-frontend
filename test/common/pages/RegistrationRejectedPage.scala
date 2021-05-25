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

abstract class RegistrationOutcomeRejectedPage extends WebPage

object RegistrationRejectedPage extends RegistrationOutcomeRejectedPage {
  override val title                = "The Advance Tariff Rulings subscription request has been unsuccessful"
  val titleRegistration             = "The Advance Tariff Rulings registration request has been unsuccessful"
  val processedDateXpath            = "//*[@id='processed-date']"
  val heading                       = "The Advance Tariff Rulings subscription request for orgName has been unsuccessful"
  val individualHeadingSubscription = "The Advance Tariff Rulings subscription request for Name has been unsuccessful"
  val individualHeadingRegistration = "The Advance Tariff Rulings registration request for Name has been unsuccessful"
  val pageHeadingXpath              = "//*[@id='page-heading']"
}
