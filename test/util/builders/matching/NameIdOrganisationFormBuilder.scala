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

package util.builders.matching

import common.RegistrationOutcome.{PENDING, RegistrationOutcome}
import java.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.Organisation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, NameIdOrganisationMatch, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.nameUtrOrganisationForm

object NameIdOrganisationFormBuilder {

  val ValidUtrId: String = "1111111111"

  val ValidUtr               = Utr(ValidUtrId)
  val ValidName              = "SA Partnership 3 For Digital"
  val ValidNameUtrRequest    = Map("name" -> ValidName, "utr" -> ValidUtrId)
  val ValidIncorporatedUtrId = "9160817001"

  val companyNameBasedOnExpectedSubscriptionOutcome: Map[RegistrationOutcome, String] =
    Map(PENDING -> "reg01-Pending Reg Corporate Body").withDefaultValue("reg01-Corporate Body-default")

  val ValidEoriId                      = "GB1234567890"
  val ValidEori                        = Eori(ValidEoriId)
  val ValidDateEstabilished: LocalDate = LocalDate.parse("2015-10-15")

  val ValidNameEoriRequest = Map(
    "name"                   -> ValidName,
    "eori"                   -> ValidEoriId,
    "date-established.day"   -> "15",
    "date-established.month" -> "10",
    "date-established.year"  -> "2015"
  )

  val mandatoryNameUtrFields: NameIdOrganisationMatch = nameUtrOrganisationForm
    .bind(ValidNameUtrRequest)
    .value
    .getOrElse(throw new IllegalArgumentException("Invalid request : " + ValidNameUtrRequest))

  val CompanyOrganisation                       = Organisation(ValidName, "Corporate Body")
  val LimitedLiabilityPartnershipOrganisation   = Organisation(ValidName, "LLP")
  val PartnershipOrganisation                   = Organisation(ValidName, "Partnership")
  val CharityPublicBodyNotForProfitOrganisation = Organisation(ValidName, "Unincorporated Body")
}
