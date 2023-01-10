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

package common.pages.subscription

import common.pages.WebPage

trait SubscriptionVatDetailsPage extends WebPage {

  override val title = "Your UK VAT details"

  val vatPostcodeFieldLevelError = "//p[@id='postcode-error' and @class='govuk-error-message']"
  val vatNumberFieldLevelError   = "//p[@id='vat-number-error' and @class='govuk-error-message']"

  val vatEffectiveDateFieldLevelError =
    "//p[contains(@id, 'vat-effective-date-error') and contains(@class, 'govuk-error-message']"

}

object SubscriptionVatDetailsPage extends SubscriptionVatDetailsPage
