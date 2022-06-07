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

trait SubscriptionEUVATDetailsPage extends WebPage {

  override val title = "Is your organisation VAT registered in other EU member countries?"

  val fieldLevelErrorEUVATNumberInput = "//p[@id='vatNumber-error' and @class='govuk-error-message']"

}

object SubscriptionEUVATDetailsPage extends SubscriptionEUVATDetailsPage

object GYEEUVATNumber extends SubscriptionEUVATDetailsPage {
  override val title = "Are you VAT registered in other EU member countries?"

  val url: String = "/customs-registration-services/register/vat-details-eu"
}
