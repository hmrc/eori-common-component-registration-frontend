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

package common.pages.subscription

import common.pages.WebPage

trait SubscriptionContactDetailsPage extends WebPage {

  override val title = "Who can we contact?"

  val formId = "contactDetailsForm"

  val headingXPath        = "//*[@id='contactDetailsForm']/fieldset/legend/h1"
  val introXPathRegister  = "//*[@id='contactDetailsForm']/fieldset/span"
  val introXPathSubscribe = "//*[@id='contactDetailsForm']/fieldset/p"

  val fullNameFieldXPath           = "//*[@id='full-name']"
  val fullNameFieldLevelErrorXPath = "//span[@id='full-name-error' and @class='govuk-error-message']"
  val fullNameFieldLabel           = "Full name"
  val fullNameFieldId              = "full-name"

  val emailFieldXPath                  = "//*[@id='email']"
  val emailAddressFieldLevelErrorXPath = "//span[@id='email-error' and @class='govuk-error-message']"
  val emailAddressFieldLabel           = "Email address"
  val emailAddressFieldId              = "email"

  val emailReadOnlyFieldXPath = "//*[@id='email-read-only']"
  val emailLabelXPath         = "//*[@id='email-outer']/label"

  val telephoneFieldXPath           = "//*[@id='telephone']"
  val telephoneFieldLevelErrorXPath = "//span[@id='telephone-error' and @class='govuk-error-message']"
  val telephoneFieldLabel           = "Telephone"
  val telephoneHintText             = "Only enter numbers, for example 01632 960 001"
  val telephoneFieldId              = "telephone"

  val faxFieldXPath           = "//*[@id='fax']"
  val faxFieldLevelErrorXPath = "//span[@id='fax-error' and @class='govuk-error-message']"
  val faxFieldLabel           = "Fax (optional)"
  val faxHintText             = "Only enter numbers, for example 01632 960 001"
  val faxFieldId              = "fax"

  val useRegisteredAddressLabel = "Enter address"

  val streetFieldXPath           = "//*[@id='street']"
  val streetFieldLevelErrorXPath = "//span[@id='street-error' and @class='govuk-error-message']"
  val streetFieldLabel           = "Street"

  val cityFieldXPath           = "//*[@id='city']"
  val cityFieldLevelErrorXPath = "//span[@id='city-error' and @class='govuk-error-message']"
  val cityFieldLabel           = "Town or city"

  val countryFieldLevelErrorXPath = "//span[@id='countryCode-error' and @class='govuk-error-message']"
  val countryFieldLabel           = "Country"

  val countryCodeSelectedOptionXPath = "//*[@id='countryCode']/option[@selected]"

  val postcodeFieldXPath           = "//*[@id='postcode']"
  val postcodeFieldLevelErrorXPath = "//span[@id='postcode-error' and @class='govuk-error-message']"
  val postcodeFieldLabel           = "Postcode"

  val registeredAddressQuestionXPath =
    "//fieldset[@class='govuk-fieldset' and @aria-describedby='use-registered-address-hint']/legend/h1"

  val registeredAddressParaXPath =
    "//fieldset[@class='govuk-fieldset' and @aria-describedby='use-registered-address-hint']/div[@id='use-registered-address-hint']"

  val useRegisteredAddressYesRadioButtonXPath = "//*[@id='use-registered-address-yes']"
  val useRegisteredAddressNoRadioButtonXPath  = "//*[@id='use-registered-address-no']"

  val continueButtonXpath = "//*[@class='govuk-button']"

  val hintTextTelephonXpath = "//*[@id='telephone-hint']"
  val hintTextFaxXpath      = "//*[@id='fax-hint']"
  val stepsXpath            = "//*[@id='steps-heading']"

}

object SubscriptionContactDetailsPage extends SubscriptionContactDetailsPage
