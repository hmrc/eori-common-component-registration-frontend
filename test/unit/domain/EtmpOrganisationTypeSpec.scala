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

package unit.domain

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._

class EtmpOrganisationTypeSpec extends UnitSpec {

  "ETMP Organisation Type" should {

    "convert from CDS Organisation Type: LimitedCompany to CorporateBody" in {
      EtmpOrganisationType(Company) shouldBe CorporateBody
    }

    "convert from CDS Organisation Type: LimitedLiabilityPartnership to LLP" in {
      EtmpOrganisationType(LimitedLiabilityPartnership) shouldBe LLP
    }

    "convert from CDS Organisation Type: Partnership to Partnership" in {
      EtmpOrganisationType(CdsOrganisationType.Partnership) shouldBe domain.Partnership
    }

    "convert from CDS Organisation Type: CharitableTrust to UnincorporatedBody" in {
      EtmpOrganisationType(CharityPublicBodyNotForProfit) shouldBe UnincorporatedBody
    }

    "convert from CDS Organisation Type: EUOrganisation to CorporateBody" in {
      EtmpOrganisationType(EUOrganisation) shouldBe CorporateBody
    }

    "convert from CDS Organisation Type: ThirdCountryOrganisation to CorporateBody" in {
      EtmpOrganisationType(ThirdCountryOrganisation) shouldBe CorporateBody
    }

    "throw an exception when invalid id is applied to EtmpOrganisationType" in {
      intercept[IllegalArgumentException](
        EtmpOrganisationType("invalidId")
      ).getMessage shouldBe "I got an invalidId as an ETMP Organisation Type but I wanted one of \"Partnership\", \"LLP\", \"Corporate Body\", \"Unincorporated Body\""
    }

    "toString needs to be defined for each ETMP Organisation Type" in {
      domain.Partnership.toString shouldBe "Partnership"
      LLP.toString shouldBe "LLP"
      CorporateBody.toString shouldBe "Corporate Body"
      UnincorporatedBody.toString shouldBe "Unincorporated Body"
      NA.toString shouldBe "N/A"
    }

    "etmpOrgTypeCode needs to be defined for each ETMP Organisation Type" in {
      domain.Partnership.etmpOrgTypeCode shouldBe "0001"
      LLP.etmpOrgTypeCode shouldBe "0002"
      CorporateBody.etmpOrgTypeCode shouldBe "0003"
      UnincorporatedBody.etmpOrgTypeCode shouldBe "0004"
      NA.etmpOrgTypeCode shouldBe "N/A"
    }
  }
}
