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

package common.pages.subscription

import common.pages.WebPage

trait SubscriptionContactDetailsPage extends WebPage {

  override val title = "Who can we contact?"

  val formId = "contactDetailsForm"

  val headingXPath = "//*[@id='contactDetailsForm']/fieldset/legend/h1"
  val introXPathRegister = "//*[@id='contact-details-intro']"
  val introXPathSubscribe = "//*[@id='contactDetailsForm']/fieldset/p"

  val fullNameFieldXPath = "//*[@id='full-name']"
  val fullNameFieldLevelErrorXPath = "//p[@id='full-name-error' and @class='govuk-error-message']"
  val fullNameFieldLabel = "Full name"
  val fullNameFieldId = "full-name"

  val emailFieldXPath = "//*[@id='email']"
  val emailAddressFieldLevelErrorXPath = "//p[@id='email-error' and @class='govuk-error-message']"
  val emailAddressFieldLabel = "Email address"
  val emailAddressFieldId = "email"

  val emailReadOnlyFieldXPath = "//*[@id='email-read-only']"
  val emailLabelXPath = "//*[@id='email-outer']/span"

  val telephoneFieldXPath = "//*[@id='telephone']"
  val telephoneFieldLevelErrorXPath = "//p[@id='telephone-error' and @class='govuk-error-message']"
  val telephoneFieldLabel = "Telephone"
  val telephoneHintText = "For international numbers include the country code."
  val telephoneFieldId = "telephone"

  val faxFieldXPath = "//*[@id='fax']"
  val faxFieldLevelErrorXPath = "//p[@id='fax-error' and @class='govuk-error-message']"
  val faxFieldLabel = "Fax (optional)"
  val faxHintText = "For international numbers include the country code."
  val faxFieldId = "fax"

  val useRegisteredAddressLabel = "Enter address"

  val streetFieldXPath = "//*[@id='street']"
  val streetFieldLevelErrorXPath = "//p[@id='street-error' and @class='govuk-error-message']"
  val streetFieldLabel = "Street"

  val cityFieldXPath = "//*[@id='city']"
  val cityFieldLevelErrorXPath = "//p[@id='city-error' and @class='govuk-error-message']"
  val cityFieldLabel = "Town or city"

  val countryFieldLevelErrorXPath = "//p[@id='countryCode-error' and @class='govuk-error-message']"
  val countryFieldLabel = "Country"

  val countryCodeSelectedOptionXPath = "//*[@id='countryCode']/option[@selected]"

  val postcodeFieldXPath = "//*[@id='postcode']"
  val postcodeFieldLevelErrorXPath = "//p[@id='postcode-error' and @class='govuk-error-message']"
  val postcodeFieldLabel = "Postcode"

  val registeredAddressQuestionXPath =
    "//fieldset[@class='govuk-fieldset' and @aria-describedby='use-registered-address-hint']/legend/h1"

  val registeredAddressParaXPath =
    "//fieldset[@class='govuk-fieldset' and @aria-describedby='use-registered-address-hint']/div[@id='use-registered-address-hint']"

  val useRegisteredAddressYesRadioButtonXPath = "//*[@id='use-registered-address-yes']"
  val useRegisteredAddressNoRadioButtonXPath = "//*[@id='use-registered-address-no']"

  val continueButtonXpath = "//*[@id='continue-button']"

  val hintTextFaxXpath = "//*[@id='fax-hint']"
  val stepsXpath = "//*[@id='steps-heading']"

}

object SubscriptionContactDetailsPage extends SubscriptionContactDetailsPage
