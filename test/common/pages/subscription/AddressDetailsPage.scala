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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel

sealed trait AddressDetailsPage extends WebPage {

  override val title     = "Your details"
  val manualAddressTitle = "Enter your address manually"

  val formId: String = "addressDetailsForm"

  val continueButtonXpath = "//*[@id='continue-button']"

  val streetFieldXPath           = "//*[@id='street']"
  val streetFieldLevelErrorXPath = "//p[@id='street-error' and @class='govuk-error-message']"

  val cityFieldXPath           = "//*[@id='city']"
  val cityFieldLevelErrorXPath = "//p[@id='city-error' and @class='govuk-error-message']"

  val countryFieldLevelErrorXPath = "//p[@id='countryCode-error' and @class='govuk-error-message']"

  val countryCodeFieldXPath = "//*[@id='countryCode']"

  val postcodeFieldXPath           = "//*[@id='postcode']"
  val postcodeFieldLevelErrorXPath = "//p[@id='postcode-error' and @class='govuk-error-message']"

  val filledValues =
    AddressViewModel(street = "Line 1", city = "city name", postcode = Some("SE28 1AA"), countryCode = "ZZ")

}

trait AddressPage extends AddressDetailsPage {}

object AddressPage extends AddressPage
