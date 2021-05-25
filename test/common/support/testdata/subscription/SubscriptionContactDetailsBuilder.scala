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

package common.support.testdata.subscription

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ContactDetails

object SubscriptionContactDetailsBuilder {

  val contactDetailsWithMandatoryValuesOnly = ContactDetails(
    fullName = "John Doe",
    emailAddress = "john.doe@example.com",
    telephone = "01632961234",
    fax = None,
    street = "Line 1",
    city = "city name",
    postcode = None,
    countryCode = "ZZ"
  )

  val contactDetailsWithAllValues = ContactDetails(
    fullName = "John Doe",
    emailAddress = "john.doe@example.com",
    telephone = "01632961234",
    fax = Some("01632961234"),
    street = "Line 1",
    city = "city name",
    postcode = Some("SE28 1AA"),
    countryCode = "ZZ"
  )

}
