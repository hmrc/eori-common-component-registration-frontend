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

package util.builders

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel

object SubscriptionContactDetailsFormBuilder {

  val FullName           = "Full Name"
  val Email              = "john.doe@example.com"
  val Telephone          = "01632961234"
  val Fax                = "01632961235"
  val Street             = "Line 1"
  val City               = "city name"
  val Postcode           = "SE28 1AA"
  val CountryCode        = "FR"
  val Country            = "France"
  val RevisedCountryCode = "AL"
  val RevisedCountry     = "Albania"

  val fullNameFieldName                 = "full-name"
  val emailFieldName                    = "email"
  val telephoneFieldName                = "telephone"
  val faxFieldName                      = "fax"
  val useRegisteredAddressFlagFieldName = "use-registered-address"
  val streetFieldName                   = "street"
  val cityFieldName                     = "city"
  val postcodeFieldName                 = "postcode"
  val countryCodeFieldName              = "countryCode"
  val countryFieldName                  = s"$countryCodeFieldName-auto-complete"

  val createFormMandatoryFieldsMap: Map[String, String] = Map(
    fullNameFieldName                 -> FullName,
    emailFieldName                    -> Email,
    telephoneFieldName                -> Telephone,
    useRegisteredAddressFlagFieldName -> "true"
  )

  val createFormMandatoryFieldsMapSubscribe: Map[String, String] =
    Map(fullNameFieldName -> FullName, emailFieldName -> Email, telephoneFieldName -> Telephone)

  val editFormFields: Map[String, String] = Map(
    fullNameFieldName    -> FullName,
    emailFieldName       -> Email,
    telephoneFieldName   -> Telephone,
    faxFieldName         -> Fax,
    streetFieldName      -> Street,
    cityFieldName        -> City,
    postcodeFieldName    -> Postcode,
    countryFieldName     -> Country,
    countryCodeFieldName -> CountryCode
  )

  val contactDetails = ContactDetails(
    fullName = FullName,
    emailAddress = Email,
    telephone = Telephone,
    fax = Some(Fax),
    street = Street,
    city = City,
    postcode = Some(Postcode),
    countryCode = CountryCode
  )

  val revisedContactDetailsModel = ContactDetailsModel(
    fullName = FullName,
    emailAddress = Email,
    telephone = Telephone,
    fax = Some(Fax),
    street = Some(Street),
    city = Some(City),
    postcode = Some(Postcode),
    countryCode = Some(RevisedCountryCode),
    useAddressFromRegistrationDetails = false
  )

  val contactDetailsModel = ContactDetailsModel(
    fullName = FullName,
    emailAddress = Email,
    telephone = Telephone,
    fax = Some(Fax),
    street = Some(Street),
    city = Some(City),
    postcode = Some(Postcode),
    countryCode = Some(CountryCode),
    useAddressFromRegistrationDetails = false
  )

  val contactDetailsModelWithRegisteredAddress: ContactDetailsModel =
    contactDetailsModel.copy(useAddressFromRegistrationDetails = true)

  val createFormMandatoryFieldsWhenNotUsingRegAddressMap: Map[String, String] = Map(
    fullNameFieldName                 -> FullName,
    emailFieldName                    -> Email,
    telephoneFieldName                -> Telephone,
    useRegisteredAddressFlagFieldName -> "false",
    streetFieldName                   -> Street,
    cityFieldName                     -> City,
    postcodeFieldName                 -> "",
    countryFieldName                  -> Country,
    countryCodeFieldName              -> CountryCode
  )

  val createFormAllFieldsWhenNotUsingRegAddressMap: Map[String, String] =
    createFormMandatoryFieldsWhenNotUsingRegAddressMap ++ Map(
      faxFieldName         -> Fax,
      streetFieldName      -> Street,
      cityFieldName        -> City,
      postcodeFieldName    -> Postcode,
      countryCodeFieldName -> CountryCode,
      countryFieldName     -> Country
    )

  val createContactDetailsViewModelWhenNotUsingRegAddress = ContactDetailsModel(
    fullName = FullName,
    emailAddress = Email,
    telephone = Telephone,
    fax = Some(Fax),
    street = Some(Street),
    city = Some(City),
    postcode = Some(Postcode),
    countryCode = Some(CountryCode),
    useAddressFromRegistrationDetails = false
  )

  val createFormAllFieldsWhenUseRegAddressMap: Map[String, String] = Map(
    fullNameFieldName                 -> FullName,
    emailFieldName                    -> Email,
    telephoneFieldName                -> Telephone,
    faxFieldName                      -> Fax,
    useRegisteredAddressFlagFieldName -> "true"
  )

  val createContactDetailsViewModelWhenUseRegAddress = ContactDetailsModel(
    fullName = FullName,
    emailAddress = Email,
    telephone = Telephone,
    fax = Some(Fax),
    street = None,
    city = None,
    postcode = None,
    countryCode = None,
    useAddressFromRegistrationDetails = true
  )

  val createFormAllFieldsEmptyMap: Map[String, String] = Map(
    fullNameFieldName                 -> "",
    emailFieldName                    -> "",
    telephoneFieldName                -> "",
    faxFieldName                      -> "",
    useRegisteredAddressFlagFieldName -> ""
  )

}
