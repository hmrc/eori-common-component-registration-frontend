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

package uk.gov.hmrc.eoricommoncomponent.frontend.domain

import play.api.libs.json.{JsValue, Json, OFormat}

case class CdsOrganisationType(id: String)

object CdsOrganisationType {
  implicit val formats: OFormat[CdsOrganisationType] = Json.format[CdsOrganisationType]
  implicit def toJsonFormat(mayBeOrgType: Option[CdsOrganisationType]): JsValue = Json.toJson(mayBeOrgType)

  val CompanyId = "company"
  val SoleTraderId = "sole-trader"
  val IndividualId = "individual"
  val PartnershipId = "partnership"
  val LimitedLiabilityPartnershipId = "limited-liability-partnership"
  val CharityPublicBodyNotForProfitId = "charity-public-body-not-for-profit"
  val EmbassyId = "embassy"

  val EUOrganisationId = "eu-organisation"
  val EUIndividualId = "eu-individual"

  val ThirdCountryOrganisationId = "third-country-organisation"
  val ThirdCountrySoleTraderId = "third-country-sole-trader"
  val ThirdCountryIndividualId = "third-country-individual"

  val Company: CdsOrganisationType = CdsOrganisationType(CompanyId)
  val SoleTrader: CdsOrganisationType = CdsOrganisationType(SoleTraderId)
  val Individual: CdsOrganisationType = CdsOrganisationType(IndividualId)
  val Partnership: CdsOrganisationType = CdsOrganisationType(PartnershipId)
  val LimitedLiabilityPartnership: CdsOrganisationType = CdsOrganisationType(LimitedLiabilityPartnershipId)
  val CharityPublicBodyNotForProfit: CdsOrganisationType = CdsOrganisationType(CharityPublicBodyNotForProfitId)
  val Embassy: CdsOrganisationType = CdsOrganisationType(EmbassyId)

  val EUOrganisation: CdsOrganisationType = CdsOrganisationType(EUOrganisationId)
  val EUIndividual: CdsOrganisationType = CdsOrganisationType(EUIndividualId)

  val ThirdCountryOrganisation: CdsOrganisationType = CdsOrganisationType(ThirdCountryOrganisationId)
  val ThirdCountrySoleTrader: CdsOrganisationType = CdsOrganisationType(ThirdCountrySoleTraderId)
  val ThirdCountryIndividual: CdsOrganisationType = CdsOrganisationType(ThirdCountryIndividualId)

  val validOrganisationTypes: Map[String, CdsOrganisationType] = Map(
    CompanyId                       -> Company,
    SoleTraderId                    -> SoleTrader,
    IndividualId                    -> Individual,
    PartnershipId                   -> Partnership,
    LimitedLiabilityPartnershipId   -> LimitedLiabilityPartnership,
    CharityPublicBodyNotForProfitId -> CharityPublicBodyNotForProfit,
    EUOrganisationId                -> EUOrganisation,
    EUIndividualId                  -> EUIndividual,
    ThirdCountryOrganisationId      -> ThirdCountryOrganisation,
    ThirdCountrySoleTraderId        -> ThirdCountrySoleTrader,
    ThirdCountryIndividualId        -> ThirdCountryIndividual,
    EmbassyId                       -> Embassy
  )

  val RestOfTheWorld: Seq[CdsOrganisationType] =
    Seq(ThirdCountryOrganisation, ThirdCountrySoleTrader, ThirdCountryIndividual)

  val IndividualOrganisations: Seq[CdsOrganisationType] =
    Seq(SoleTrader, Individual, ThirdCountryIndividual, ThirdCountrySoleTrader)

  lazy val individualOrganisationIds: Seq[String] =
    Seq(SoleTraderId, IndividualId, ThirdCountryIndividualId, ThirdCountrySoleTraderId)

  lazy val rowIndividualOrganisationIds: Seq[String] =
    Seq(ThirdCountryIndividualId, ThirdCountrySoleTraderId)

  def forId(organisationTypeId: String): CdsOrganisationType = validOrganisationTypes(organisationTypeId)

}
