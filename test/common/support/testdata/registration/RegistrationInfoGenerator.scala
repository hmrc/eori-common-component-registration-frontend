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

package common.support.testdata.registration

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.NonUKIdentification
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{IndividualRegistrationInfo, OrgRegistrationInfo, TaxPayerId}

import java.time.LocalDate

object RegistrationInfoGenerator {

  val individualRegistrationInfoWithAllOptionalValues = IndividualRegistrationInfo(
    firstName = "John",
    lastName = "Doe",
    dateOfBirth = Some(LocalDate.of(1981, 2, 28)),
    taxPayerId = TaxPayerId("someTaxPayerId"),
    lineOne = "Line 1",
    lineTwo = Some("line 2"),
    lineThree = Some("line 3"),
    lineFour = Some("line 4"),
    postcode = Some("SE28 1AA"),
    country = "ZZ",
    phoneNumber = Some("01632961234"),
    email = Some("john.doe@example.com"),
    nonUKIdentification = Some(NonUKIdentification("id-number", "issuing-Institution", "issuing-CountryCode")),
    isAnAgent = false
  )

  val individualRegistrationInfoWithOnlyMandatoryValues = IndividualRegistrationInfo(
    firstName = "John",
    lastName = "Doe",
    dateOfBirth = None,
    taxPayerId = TaxPayerId("someTaxPayerId"),
    lineOne = "Line 1",
    lineTwo = None,
    lineThree = None,
    lineFour = None,
    postcode = None,
    country = "ZZ",
    phoneNumber = None,
    email = None,
    nonUKIdentification = None,
    isAnAgent = false
  )

  val organisationRegistrationInfoWithAllOptionalValues = OrgRegistrationInfo(
    name = "Test Business Name not really a Ltd",
    taxPayerId = TaxPayerId("someTaxPayerId"),
    lineOne = "Line 1",
    lineTwo = Some("line 2"),
    lineThree = Some("line 3"),
    lineFour = Some("line 4"),
    postcode = Some("SE28 1AA"),
    country = "ZZ",
    phoneNumber = Some("01632961234"),
    email = Some("john.doe@example.com"),
    organisationType = Some("company"),
    nonUKIdentification = Some(NonUKIdentification("id-number", "issuing-Institution", "issuing-CountryCode")),
    isAnAgent = false,
    isAGroup = false
  )

  val organisationRegistrationInfoWithOnlyMandatoryValues = OrgRegistrationInfo(
    name = "Test Business Name not really a Ltd",
    taxPayerId = TaxPayerId("someTaxPayerId"),
    lineOne = "Line 1",
    lineTwo = None,
    lineThree = None,
    lineFour = None,
    postcode = None,
    country = "ZZ",
    phoneNumber = None,
    email = None,
    organisationType = None,
    nonUKIdentification = None,
    isAGroup = false,
    isAnAgent = false
  )

}
