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

import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import org.joda.time.LocalDate

object RegistrationDetailsBuilder {

  val defaultCountryName = "United Kingdom"
  val defaultEmail       = "john.doe@example.com"
  val defaultAddress     = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("SE28 1AA"), "GB")

  val defaultAddressWithMandatoryValuesOnly = Address("Line 1", None, None, None, None, "GB")

  private val DefaultOrganisationRegistrationDetails =
    RegistrationDetails.organisation(
      sapNumber = "0123456789",
      safeId = SafeId("safe-id"),
      name = "orgName",
      address = defaultAddress,
      customsId = Some(Utr("123UTRNO"))
    )

  private val DefaultIndividualRegistrationDetails =
    RegistrationDetails.individual(
      sapNumber = "0123456789",
      safeId = SafeId("safe-id"),
      name = "John Doe",
      address = defaultAddress,
      dateOfBirth = LocalDate.parse("1980-07-23"),
      customsId = Some(Utr("123UTRNO"))
    )

  val organisationRegistrationDetails: RegistrationDetailsOrganisation =
    DefaultOrganisationRegistrationDetails.copy(etmpOrganisationType = Some(CorporateBody))

  val limitedLiabilityPartnershipRegistrationDetails: RegistrationDetailsOrganisation =
    DefaultOrganisationRegistrationDetails.copy(etmpOrganisationType = Some(LLP))

  val partnershipRegistrationDetails: RegistrationDetailsOrganisation = DefaultOrganisationRegistrationDetails.copy(
    etmpOrganisationType = Some(Partnership),
    customsId = Some(Utr("123456ABC"))
  )

  val emptyETMPOrgTypeRegistrationDetails: RegistrationDetailsOrganisation =
    DefaultOrganisationRegistrationDetails.copy(etmpOrganisationType = None)

  val incorporatedRegistrationDetails =
    DefaultOrganisationRegistrationDetails.copy(etmpOrganisationType = Some(CorporateBody))

  val individualRegistrationDetails                     = DefaultIndividualRegistrationDetails
  val individualRegistrationDetailsNotIdentifiedByReg01 = DefaultIndividualRegistrationDetails.copy(safeId = SafeId(""))

  val existingOrganisationRegistrationDetails = DefaultOrganisationRegistrationDetails
    .copy(etmpOrganisationType = Some(Partnership))
    .withEstablishmentDate("2001-06-09")

  val PartneshipRegistrationDetails: RegistrationDetailsOrganisation =
    DefaultOrganisationRegistrationDetails.copy(etmpOrganisationType = Some(Partnership))

  val soleTraderRegistrationDetails = DefaultIndividualRegistrationDetails.copy(name = "John Doe Sole Trader")

  val sub01Outcome: Sub01Outcome = Sub01Outcome("2016-08-18T14:01:05")

  def withBusinessAddress(businessAddress: Address): RegistrationDetails =
    DefaultOrganisationRegistrationDetails.withBusinessAddress(businessAddress)

  def withSapNumber(sapNumber: String): RegistrationDetails =
    DefaultOrganisationRegistrationDetails.withSapNumber(sapNumber)

  def withEstablishmentDate(localDateStr: String): RegistrationDetails =
    DefaultOrganisationRegistrationDetails.withEstablishmentDate(localDateStr)

  implicit class OrganisationDetailsOps(val o: RegistrationDetailsOrganisation) extends AnyVal {

    def withCustomsId(maybeCustomsId: Option[CustomsId]): RegistrationDetailsOrganisation =
      o.copy(customsId = maybeCustomsId)

    def withBusinessAddress(businessAddress: Address): RegistrationDetailsOrganisation =
      o.copy(address = businessAddress)

    def withSapNumber(sapNumber: String): RegistrationDetailsOrganisation = o.copy(sapNumber = TaxPayerId(sapNumber))

    def withEstablishmentDate(localDateStr: String): RegistrationDetailsOrganisation =
      o.copy(dateOfEstablishment = Some(LocalDate.parse(localDateStr)))

    def withOrganisationType(organisationType: EtmpOrganisationType): RegistrationDetailsOrganisation =
      o.copy(etmpOrganisationType = Option(organisationType))

  }

  implicit class IndividualDetailsOps(val i: RegistrationDetailsIndividual) extends AnyVal {

    def withCustomsId(maybeCustomsId: Option[CustomsId]): RegistrationDetailsIndividual =
      i.copy(customsId = maybeCustomsId)

  }

}
