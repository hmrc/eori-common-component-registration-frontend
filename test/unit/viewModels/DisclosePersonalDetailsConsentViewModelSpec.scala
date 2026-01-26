/*
 * Copyright 2026 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.DisclosePersonalDetailsConsentViewModel
import util.ControllerSpec

class DisclosePersonalDetailsConsentViewModelSpec extends UnitSpec with ControllerSpec {

  private val viewModel = DisclosePersonalDetailsConsentViewModel

  private val mockRequestSessionData = mock[RequestSessionData]

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val organisationsToTest = Seq[(CdsOrganisationType, String, String)](
    (
      CdsOrganisationType.Company,
      messages("ecc.subscription.organisation-disclose-personal-details-consent.org.question"),
      messages("ecc.subscription.organisation-disclose-personal-details-consent.org.para2")
    ),
    (
      CdsOrganisationType.Partnership,
      messages("ecc.subscription.organisation-disclose-personal-details-consent.partnership.question"),
      messages("ecc.subscription.organisation-disclose-personal-details-consent.partnership.para2")
    ),
    (
      CdsOrganisationType.LimitedLiabilityPartnership,
      messages("ecc.subscription.organisation-disclose-personal-details-consent.org.question"),
      messages("ecc.subscription.organisation-disclose-personal-details-consent.org.para2")
    ),
    (
      CdsOrganisationType.CharityPublicBodyNotForProfit,
      messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.question"),
      messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.para2")
    ),
    (
      CdsOrganisationType.ThirdCountryOrganisation,
      messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.question"),
      messages("ecc.subscription.organisation-disclose-personal-details-consent.charity.para2")
    ),
    (
      CdsOrganisationType.Individual,
      messages("ecc.subscription.organisation-disclose-personal-details-consent.individual.question"),
      messages("ecc.subscription.organisation-disclose-personal-details-consent.individual.para2")
    )
  )

  "questionLabel" should organisationsToTest.foreach { orgType =>
    s"display correct message for ${orgType._1}" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(orgType._1))
      viewModel(mockRequestSessionData).questionLabel() shouldBe orgType._2
    }
  }

  "textPara2" should organisationsToTest.foreach { orgType =>
    s"display correct message for ${orgType._1}" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(orgType._1))
      viewModel(mockRequestSessionData).textPara2() shouldBe orgType._3
    }
  }
}
