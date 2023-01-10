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

package util.builders.matching

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, Utr}

object IndividualIdFormBuilder {

  val ValidUtrId            = "2108834503"
  val ValidEoriId           = "GB012345678912345"
  val validUtr              = Utr(ValidUtrId)
  val ValidEori             = Eori(ValidEoriId)
  val ValidFirstName        = "John"
  val ValidMiddleName       = "Middle"
  val ValidLastName         = "Doe"
  val ValidDateOfBirthDay   = "23"
  val ValidDateOfBirthMonth = "07"
  val ValidDateOfBirthYear  = "1980"
  val validDateOfBirth      = s"$ValidDateOfBirthYear-$ValidDateOfBirthMonth-$ValidDateOfBirthDay"
  val ValidAddressLine1     = "Address Line 1"
  val ValidAddressLine2     = "Address line 2"
  val ValidPostcode         = "SE28 1AA"
  val ValidCountry          = "GB"
  val ThirdCountry          = "Algeria"

  val ValidRequest = Map(
    "first-name"          -> ValidFirstName,
    "last-name"           -> ValidLastName,
    "date-of-birth.day"   -> ValidDateOfBirthDay,
    "date-of-birth.month" -> ValidDateOfBirthMonth,
    "date-of-birth.year"  -> ValidDateOfBirthYear
  )

  val ValidRequestEori = ValidRequest + ("middle-name" -> ValidMiddleName, "matching-id" -> ValidEoriId)

  val validIndividualUtr = Individual.noMiddle(ValidFirstName, ValidLastName, validDateOfBirth)

  val ValidIndividualEori = Individual(ValidFirstName, Some(ValidMiddleName), ValidLastName, validDateOfBirth)
}
