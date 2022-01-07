/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._

sealed trait EtmpOrganisationType {
  def etmpOrgTypeCode: String
}

case object Partnership extends EtmpOrganisationType {
  override def etmpOrgTypeCode: String = "0001"

  override def toString: String = "Partnership"
}

case object LLP extends EtmpOrganisationType {
  override def etmpOrgTypeCode: String = "0002"

  override def toString: String = "LLP"
}

case object CorporateBody extends EtmpOrganisationType {
  override def etmpOrgTypeCode: String = "0003"

  override def toString: String = "Corporate Body"
}

case object UnincorporatedBody extends EtmpOrganisationType {
  override def etmpOrgTypeCode: String = "0004"

  override def toString: String = "Unincorporated Body"
}

case object NA extends EtmpOrganisationType {
  override def etmpOrgTypeCode: String = "N/A"

  override def toString: String = "N/A"
}

object EtmpOrganisationType {

  private val cdsToEtmpOrgType = Map(
    CompanyId                       -> CorporateBody,
    PartnershipId                   -> Partnership,
    LimitedLiabilityPartnershipId   -> LLP,
    CharityPublicBodyNotForProfitId -> UnincorporatedBody,
    EUOrganisationId                -> CorporateBody,
    ThirdCountryOrganisationId      -> CorporateBody
  )

  def apply(cdsOrgType: CdsOrganisationType): EtmpOrganisationType = cdsToEtmpOrgType.getOrElse(cdsOrgType.id, NA)

  def apply(id: String): EtmpOrganisationType = id match {
    case "Partnership"         => Partnership
    case "LLP"                 => LLP
    case "Corporate Body"      => CorporateBody
    case "Unincorporated Body" => UnincorporatedBody
    case invalidId =>
      throw new IllegalArgumentException(
        s"""I got an $invalidId as an ETMP Organisation Type but I wanted one of "Partnership", "LLP", "Corporate Body", "Unincorporated Body""""
      )
  }

  private def unapply(id: EtmpOrganisationType): String = id match {
    case Partnership        => "Partnership"
    case LLP                => "LLP"
    case CorporateBody      => "Corporate Body"
    case UnincorporatedBody => "Unincorporated Body"
    case _                  => "N/A"
  }

  def orgTypeToEtmpOrgCode(id: String): String = apply(id).etmpOrgTypeCode

  implicit val etmpOrgReads: Reads[EtmpOrganisationType] = new Reads[EtmpOrganisationType] {
    def reads(value: JsValue): JsResult[EtmpOrganisationType] = JsSuccess(apply((value \ "id").as[String]))
  }

  implicit val etmpOrgWrites: Writes[EtmpOrganisationType] = new Writes[EtmpOrganisationType] {
    def writes(org: EtmpOrganisationType): JsValue = Json.toJson(CdsOrganisationType(unapply(org)))
  }

}
