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

package common.support.testdata.subscription

import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel

object SubscriptionContactDetailsModelBuilder {

  val contactUkDetailsModelWithMandatoryValuesOnly = ContactDetailsModel(
    fullName = "John Doe",
    emailAddress = "john.doe@example.com",
    telephone = "01632961234",
    fax = None,
    street = Some("Line 1"),
    city = Some("city name"),
    postcode = None,
    countryCode = Some("GB")
  )

  val contactDetailsModelWithAllValues = ContactDetailsModel(
    fullName = "John Doe",
    emailAddress = "john.doe@example.com",
    telephone = "01632961234",
    fax = Some("01632961234"),
    street = Some("Line 1"),
    city = Some("city name"),
    postcode = Some("SE28 1AA"),
    countryCode = Some("FR")
  )

}
