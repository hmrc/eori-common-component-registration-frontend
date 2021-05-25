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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping

import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{RegistrationDetailsIndividual, _}

object EtmpTypeOfPerson {
  val NaturalPerson       = "1"
  val LegalPerson         = "2"
  val AssociationOfPerson = "3"
}

object EtmpLegalStatus {
  val CorporateBody      = "Corporate Body"
  val UnincorporatedBody = "Unincorporated Body"
  val Llp                = "LLP"
  val Partnership        = "Partnership"
}

case class OrganisationTypeConfiguration(typeOfPerson: String, legalStatus: String)

object OrganisationTypeConfiguration {

  val Company: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.CorporateBody)

  val SoleTrader: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.NaturalPerson, EtmpLegalStatus.UnincorporatedBody)

  val Individual: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.NaturalPerson, EtmpLegalStatus.UnincorporatedBody)

  val Partnership: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.Partnership)

  val LimitedLiabilityPartnership: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.Llp)

  val CharityPublicBodyNotForProfit: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.AssociationOfPerson, EtmpLegalStatus.UnincorporatedBody)

  val EUOrganisation: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.CorporateBody)

  val EUIndividual: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.NaturalPerson, EtmpLegalStatus.UnincorporatedBody)

  val ThirdCountryOrganisation: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.CorporateBody)

  val ThirdCountrySoleTrader: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.NaturalPerson, EtmpLegalStatus.UnincorporatedBody)

  val ThirdCountryIndividual: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.NaturalPerson, EtmpLegalStatus.UnincorporatedBody)

  val EtmpPartnership: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.Partnership)

  val EtmpLlp: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.Llp)

  val EtmpCorporateBody: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.LegalPerson, EtmpLegalStatus.CorporateBody)

  val EtmpUnincorporatedBody: OrganisationTypeConfiguration =
    OrganisationTypeConfiguration(EtmpTypeOfPerson.AssociationOfPerson, EtmpLegalStatus.UnincorporatedBody)

}

object CdsToEtmpOrganisationType {

  private val cdsTypeOfPersonMap = Map(
    CdsOrganisationType.Company                       -> OrganisationTypeConfiguration.Company,
    CdsOrganisationType.SoleTrader                    -> OrganisationTypeConfiguration.SoleTrader,
    CdsOrganisationType.Individual                    -> OrganisationTypeConfiguration.Individual,
    CdsOrganisationType.Partnership                   -> OrganisationTypeConfiguration.Partnership,
    CdsOrganisationType.LimitedLiabilityPartnership   -> OrganisationTypeConfiguration.LimitedLiabilityPartnership,
    CdsOrganisationType.CharityPublicBodyNotForProfit -> OrganisationTypeConfiguration.CharityPublicBodyNotForProfit,
    CdsOrganisationType.EUOrganisation                -> OrganisationTypeConfiguration.EUOrganisation,
    CdsOrganisationType.EUIndividual                  -> OrganisationTypeConfiguration.EUIndividual,
    CdsOrganisationType.ThirdCountryOrganisation      -> OrganisationTypeConfiguration.ThirdCountryOrganisation,
    CdsOrganisationType.ThirdCountrySoleTrader        -> OrganisationTypeConfiguration.ThirdCountrySoleTrader,
    CdsOrganisationType.ThirdCountryIndividual        -> OrganisationTypeConfiguration.ThirdCountryIndividual
  )

  private def etmpTypeOfPersonMap(orgType: EtmpOrganisationType) = orgType match {
    case Partnership        => OrganisationTypeConfiguration.EtmpPartnership
    case LLP                => OrganisationTypeConfiguration.EtmpLlp
    case CorporateBody      => OrganisationTypeConfiguration.EtmpCorporateBody
    case UnincorporatedBody => OrganisationTypeConfiguration.EtmpUnincorporatedBody
    case invalid            => throw new IllegalArgumentException(s"Invalid ETMP orgType: $invalid")
  }

  def apply(cdsOrganisationType: Option[CdsOrganisationType]): Option[OrganisationTypeConfiguration] =
    cdsOrganisationType.flatMap(cdsTypeOfPersonMap.get)

  def apply(matchingDetails: RegistrationDetails): Option[OrganisationTypeConfiguration] = matchingDetails match {
    case org: RegistrationDetailsOrganisation => org.etmpOrganisationType.map(etmpTypeOfPersonMap)
    case _: RegistrationDetailsIndividual =>
      Some(OrganisationTypeConfiguration(EtmpTypeOfPerson.NaturalPerson, EtmpLegalStatus.UnincorporatedBody))
    case _ => throw new IllegalStateException("Incomplete cache cannot complete journey")
  }

}
