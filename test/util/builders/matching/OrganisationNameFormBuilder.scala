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

import common.RegistrationOutcome.{PENDING, PROCESSING, RegistrationOutcome}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameMatch
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.Organisation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.organisationNameForm

object OrganisationNameFormBuilder {

  val ValidName        = "orgName"
  val ValidNameRequest = Map("name" -> ValidName)

  val companyNameBasedOnExpectedSubscriptionOutcome: Map[RegistrationOutcome, String] =
    Map(PENDING -> "reg01-Pending Reg Corporate Body").withDefaultValue("reg01-Corporate Body-default")

  def validIncorporatedNameUtrRequest(expectedSubOutcome: RegistrationOutcome): Map[String, String] = Map(
    "name" -> companyNameBasedOnExpectedSubscriptionOutcome(expectedSubOutcome)
  )

  val mandatoryNameFields: NameMatch = organisationNameForm.bind(ValidNameRequest).value.get

  def mandatoryIncorporatedNameFields(expectedRegOutcome: Option[RegistrationOutcome]): NameMatch =
    organisationNameForm.bind(validIncorporatedNameUtrRequest(expectedRegOutcome.getOrElse(PROCESSING))).value.get

  val CharityPublicBodyNotForProfitOrganisation = Organisation(ValidName, "Unincorporated Body")
  val ThirdCountryOrg                           = Organisation(ValidName, "Unincorporated Body")
}
