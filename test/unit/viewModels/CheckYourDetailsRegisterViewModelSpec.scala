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

package unit.viewModels

import base.UnitSpec
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, Eori, Nino, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.CheckYourDetailsRegisterConstructor
import uk.gov.hmrc.eoricommoncomponent.frontend.views.helpers.DateFormatter
import unit.services.SubscriptionServiceTestData
import util.ControllerSpec
import util.builders.RegistrationDetailsBuilder.{limitedLiabilityPartnershipRegistrationDetails, partnershipRegistrationDetails}

class CheckYourDetailsRegisterViewModelSpec extends UnitSpec with ControllerSpec with SubscriptionServiceTestData {

  val mockDateFormatter: DateFormatter         = mock[DateFormatter]
  val mockSessionCache: SessionCache           = mock[SessionCache]
  val mockRegistrationData: RequestSessionData = mock[RequestSessionData]

  private val organisationToTest =
    Seq(
      Option(CdsOrganisationType.Company),
      Option(CdsOrganisationType.EUOrganisation),
      Option(CdsOrganisationType.ThirdCountryOrganisation)
    )

  private val organisationWithCharityToTest =
    Seq(Option(CdsOrganisationType.CharityPublicBodyNotForProfit), Option(CdsOrganisationType.ThirdCountryOrganisation))

  private val partnershipToTest =
    Seq(Option(CdsOrganisationType.Partnership), Option(CdsOrganisationType.LimitedLiabilityPartnership))

  private val individualToTest =
    Seq(
      Option(CdsOrganisationType.Individual),
      Option(CdsOrganisationType.EUIndividual),
      Option(CdsOrganisationType.ThirdCountryIndividual)
    )

  private val soleTraderToTest =
    Seq(Some(CdsOrganisationType.SoleTrader), Some(CdsOrganisationType.ThirdCountrySoleTrader))

  private val soleAndIndividualToTest = individualToTest ++ soleTraderToTest

  val constructorInstance =
    new CheckYourDetailsRegisterConstructor(mockDateFormatter, mockSessionCache, mockRegistrationData)

  "getDateOfEstablishmentLabel" should {
    "return correct messages for SoleTrader is true" in soleTraderToTest.foreach { test =>
      val result = constructorInstance.getDateOfEstablishmentLabel(test)
      result shouldBe "Date of birth"
    }
    "return correct messages for SoleTrader is false" in organisationToTest.foreach { test =>
      val result = constructorInstance.getDateOfEstablishmentLabel(test)
      result shouldBe "Organisation establish date"
    }
  }
  "orgNameLabel" should {
    "return correct messages for partnership" in partnershipToTest.foreach { test =>
      val result = constructorInstance.orgNameLabel(test, isPartnership = true)
      result shouldBe "Registered partnership name"
    }
    "return correct messages for orgType" ignore organisationWithCharityToTest.foreach { test => // todo unicode u2019 comparison not working
      val result = constructorInstance.orgNameLabel(test, isPartnership = false)
      result shouldBe "Organisation’s name"
    }
    "return correct messages for any other " in individualToTest.foreach { test =>
      val result = constructorInstance.orgNameLabel(test, isPartnership = false)
      result shouldBe "Registered company name"
    }
  }

  "ninoOrUtrLabel" should {
    "return correct messages for partnership with UTR" in {
      val result = constructorInstance.ninoOrUtrLabel(
        limitedLiabilityPartnershipRegistrationDetails,
        Option(CdsOrganisationType.LimitedLiabilityPartnership),
        isPartnership = false
      )
      result shouldBe "Corporation Tax Unique Taxpayer Reference (UTR)"
    }

    "return correct messages for partnership non LLP with UTR" in {
      val result = constructorInstance.ninoOrUtrLabel(
        partnershipRegistrationDetails,
        Option(CdsOrganisationType.Partnership),
        isPartnership = true
      )
      result shouldBe "Partnership Self Assessment UTR"
    }

    "return correct messages for orgType with UTR" in organisationToTest.foreach { test =>
      val result = constructorInstance.ninoOrUtrLabel(
        organisationRegistrationDetails.copy(customsId = Some(Utr("12345679"))),
        test,
        isPartnership = false
      )
      result shouldBe "Corporation Tax UTR"
    }
    "return correct messages for Sole and Individual with UTR " in soleAndIndividualToTest.foreach { test =>
      val result = constructorInstance.ninoOrUtrLabel(
        individualRegistrationDetails.copy(customsId = Some(Utr("12345679"))),
        test,
        isPartnership = false
      )
      result shouldBe "Self Assessment Unique Taxpayer Reference (UTR)"
    }
    "return correct messages for Sole and Individual with NINO " in soleAndIndividualToTest.foreach { test =>
      val result = constructorInstance.ninoOrUtrLabel(
        individualRegistrationDetails.copy(customsId = Some(Nino("12345679"))),
        test,
        isPartnership = false
      )
      result shouldBe "National Insurance number"
    }
    "return correct messages for orgType with Eori " in organisationToTest.foreach { test =>
      val result = constructorInstance.ninoOrUtrLabel(
        organisationRegistrationDetails.copy(customsId = Some(Eori("12345679"))),
        test,
        isPartnership = false
      )
      result shouldBe "EORI number"
    }
    "return correct messages for wilde case " in soleAndIndividualToTest.foreach { test =>
      val result = constructorInstance.ninoOrUtrLabel(individualRegistrationDetails, test, isPartnership = false)
      result shouldBe "National Insurance number"
    }
  }

}
