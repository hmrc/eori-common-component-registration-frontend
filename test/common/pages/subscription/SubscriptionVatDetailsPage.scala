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

trait SubscriptionVatDetailsPage extends WebPage {

  override val title = "What are your UK VAT details?"

  val vatPostcodeFieldLevelError      = "//*[@id='postcode-outer']//span[@class='error-message']"
  val vatNumberFieldLevelError        = "//*[@id='vat-number-outer']//span[@class='error-message']"
  val vatEffectiveDateFieldLevelError = "//*[@id='vat-effective-date-fieldset']//span[@class='error-message']"
}

object SubscriptionVatDetailsPage extends SubscriptionVatDetailsPage
