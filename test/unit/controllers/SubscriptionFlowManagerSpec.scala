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

package unit.controllers

import base.UnitSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Session}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{Company, Embassy}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  RegistrationDetailsEmbassy,
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation
}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError.FlowNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError.DataNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubscriptionFlowManagerSpec
    extends UnitSpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ControllerSpec {

  private val mockRequestSessionData     = mock[RequestSessionData]
  private val mockCdsFrontendDataCache   = mock[SessionCache]
  private val mockAppConfig              = mock[AppConfig]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val controller =
    new SubscriptionFlowManager(mockRequestSessionData, mockCdsFrontendDataCache, mockAppConfig)(global)

  private val mockOrgRegistrationDetails        = mock[RegistrationDetailsOrganisation]
  private val mockIndividualRegistrationDetails = mock[RegistrationDetailsIndividual]
  private val mockEmbassyRegistrationDetails    = mock[RegistrationDetailsEmbassy]
  private val mockSession                       = mock[Session]

  private val mockRequest = mock[Request[AnyContent]]

  private val mockSubscriptionFlow = mock[SubscriptionFlow]

  val noSubscriptionFlowInSessionException = new IllegalStateException("No subscription flow in session.")

  override def beforeEach(): Unit = {
    reset(mockRequestSessionData)
    reset(mockSession)
    reset(mockCdsFrontendDataCache)
    when(mockRequestSessionData.storeUserSubscriptionFlow(any[SubscriptionFlow], any[String])(any[Request[AnyContent]]))
      .thenReturn(mockSession)
    when(mockCdsFrontendDataCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]]))
      .thenReturn(Future.successful(true))
    when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
  }

  "Getting current subscription flow" should {
    "return value from session when stored there before" in {
      when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )

      controller.currentSubscriptionFlow(mockRequest, hc) shouldBe Right(mockSubscriptionFlow)
    }

    "return FlowNotFound when no data stored there" in {
      when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Left(DataNotFound("key"))
      )

      controller.currentSubscriptionFlow(mockRequest, hc) shouldBe Left(FlowNotFound())
    }

    "fail when there was no flow stored in session before" in {
      when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenThrow(noSubscriptionFlowInSessionException)

      intercept[IllegalStateException](
        controller.currentSubscriptionFlow(mockRequest, hc)
      ) shouldBe noSubscriptionFlowInSessionException
    }
  }

  "Flow already started" should {
    val values = Table(
      ("flow", "currentPage", "expectedStepNumber", "expectedTotalSteps", "expectedNextPage"),
      (OrganisationSubscriptionFlow, DateOfEstablishmentSubscriptionFlowPage, 1, 7, SicCodeSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, SicCodeSubscriptionFlowPage, 2, 7, EoriConsentSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, EoriConsentSubscriptionFlowPage, 3, 7, VatRegisteredUkSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatRegisteredUkSubscriptionFlowPage, 4, 7, VatDetailsSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatDetailsSubscriptionFlowPage, 5, 7, ContactDetailsSubscriptionFlowPageGetEori),
      (
        OrganisationSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        6,
        7,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (OrganisationSubscriptionFlow, ContactAddressSubscriptionFlowPageGetEori, 7, 7, ReviewDetailsPageGetYourEORI),
      (PartnershipSubscriptionFlow, DateOfEstablishmentSubscriptionFlowPage, 1, 7, SicCodeSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, SicCodeSubscriptionFlowPage, 2, 7, EoriConsentSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, EoriConsentSubscriptionFlowPage, 3, 7, VatRegisteredUkSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatRegisteredUkSubscriptionFlowPage, 4, 7, VatDetailsSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatDetailsSubscriptionFlowPage, 5, 7, ContactDetailsSubscriptionFlowPageGetEori),
      (
        PartnershipSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        6,
        7,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (PartnershipSubscriptionFlow, ContactAddressSubscriptionFlowPageGetEori, 7, 7, ReviewDetailsPageGetYourEORI),
      (SoleTraderSubscriptionFlow, SicCodeSubscriptionFlowPage, 1, 6, EoriConsentSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, EoriConsentSubscriptionFlowPage, 2, 6, VatRegisteredUkSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatRegisteredUkSubscriptionFlowPage, 3, 6, VatDetailsSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatDetailsSubscriptionFlowPage, 4, 6, ContactDetailsSubscriptionFlowPageGetEori),
      (
        SoleTraderSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        5,
        6,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (SoleTraderSubscriptionFlow, ContactAddressSubscriptionFlowPageGetEori, 6, 6, ReviewDetailsPageGetYourEORI),
      (IndividualSubscriptionFlow, EoriConsentSubscriptionFlowPage, 1, 3, ContactDetailsSubscriptionFlowPageGetEori),
      (
        IndividualSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        2,
        3,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (IndividualSubscriptionFlow, ContactAddressSubscriptionFlowPageGetEori, 3, 3, ReviewDetailsPageGetYourEORI),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        DateOfEstablishmentSubscriptionFlowPage,
        1,
        7,
        SicCodeSubscriptionFlowPage
      ),
      (ThirdCountryOrganisationSubscriptionFlow, SicCodeSubscriptionFlowPage, 2, 7, EoriConsentSubscriptionFlowPage),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        EoriConsentSubscriptionFlowPage,
        3,
        7,
        VatRegisteredUkSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        VatRegisteredUkSubscriptionFlowPage,
        4,
        7,
        VatDetailsSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        VatDetailsSubscriptionFlowPage,
        5,
        7,
        ContactDetailsSubscriptionFlowPageGetEori
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        6,
        7,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        ContactAddressSubscriptionFlowPageGetEori,
        7,
        7,
        ReviewDetailsPageGetYourEORI
      ),
      (
        ThirdCountryIndividualSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        2,
        3,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (
        ThirdCountryIndividualSubscriptionFlow,
        ContactAddressSubscriptionFlowPageGetEori,
        3,
        3,
        ReviewDetailsPageGetYourEORI
      ),
      (
        ThirdCountryIndividualSubscriptionFlow,
        EoriConsentSubscriptionFlowPage,
        1,
        3,
        ContactDetailsSubscriptionFlowPageGetEori
      ),
      (ThirdCountrySoleTraderSubscriptionFlow, SicCodeSubscriptionFlowPage, 1, 6, EoriConsentSubscriptionFlowPage),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        EoriConsentSubscriptionFlowPage,
        2,
        6,
        VatRegisteredUkSubscriptionFlowPage
      ),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        VatRegisteredUkSubscriptionFlowPage,
        3,
        6,
        VatDetailsSubscriptionFlowPage
      ),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        VatDetailsSubscriptionFlowPage,
        4,
        6,
        ContactDetailsSubscriptionFlowPageGetEori
      ),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        5,
        6,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        ContactAddressSubscriptionFlowPageGetEori,
        6,
        6,
        ReviewDetailsPageGetYourEORI
      ),
      (EmbassySubscriptionFlow, EoriConsentSubscriptionFlowPage, 1, 3, ContactDetailsSubscriptionFlowPageGetEori),
      (
        EmbassySubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        2,
        3,
        ContactAddressSubscriptionFlowPageGetEori
      ),
      (EmbassySubscriptionFlow, ContactAddressSubscriptionFlowPageGetEori, 3, 3, ReviewDetailsPageGetYourEORI)
    )

    TableDrivenPropertyChecks.forAll(values) {
      (
        flow: SubscriptionFlow,
        currentPage: SubscriptionPage,
        expectedStepNumber: Int,
        expectedTotalSteps: Int,
        expectedNextPage: SubscriptionPage
      ) =>
        when(mockRequestSessionData.userSubscriptionFlow(mockRequest, hc)).thenReturn(Right(flow))
        val actual = controller.stepInformation(currentPage)(mockRequest, hc)

        s"${flow.name} flow: current step is $expectedStepNumber when currentPage is $currentPage" in {
          actual.map(subFlowInfo => subFlowInfo.stepNumber shouldBe expectedStepNumber)
        }

        s"${flow.name} flow: total Number of steps are $expectedTotalSteps when currentPage is $currentPage" in {
          actual.map(subFlowInfo => subFlowInfo.totalSteps shouldBe expectedTotalSteps)
        }

        s"${flow.name} flow: next page is $expectedNextPage when currentPage is $currentPage" in {
          actual.map(subFlowInfo => subFlowInfo.nextPage shouldBe expectedNextPage)
        }
    }
  }

  "First Page" should {

    "start Individual Subscription Flow for individual" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)

      when(mockCdsFrontendDataCache.registrationDetails(mockRequest))
        .thenReturn(Future.successful(mockIndividualRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(Some(ConfirmIndividualTypePage), atarService)(mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(IndividualSubscriptionFlow, ConfirmIndividualTypePage.url(atarService))(mockRequest)
    }

    "start Corporate Subscription Flow when cached registration details are for an Organisation" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)

      when(mockCdsFrontendDataCache.registrationDetails(mockRequest))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService)(mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(OrganisationSubscriptionFlow, RegistrationConfirmPage.url(atarService))(mockRequest)
    }

    "start Corporate Subscription Flow when selected organisation type is Sole Trader" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest))
        .thenReturn(Some(CdsOrganisationType.SoleTrader))

      when(mockCdsFrontendDataCache.registrationDetails(mockRequest))
        .thenReturn(Future.successful(mockIndividualRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService)(mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession
      verify(mockRequestSessionData).storeUserSubscriptionFlow(
        SoleTraderSubscriptionFlow,
        RegistrationConfirmPage.url(atarService)
      )(mockRequest)
    }

    "start Corporate Subscription Flow when cached registration details are for an Organisation and pass organisationType as input" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)

      when(mockCdsFrontendDataCache.registrationDetails(mockRequest))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(Some(RegistrationConfirmPage), Company, atarService)(mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(OrganisationSubscriptionFlow, RegistrationConfirmPage.url(atarService))(mockRequest)
    }

    "start Embassy flow when cached registration details are for an Embassy and feature switch is on" in {
      when(mockAppConfig.allowNoIdJourney).thenReturn(true)
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(Some(Embassy))

      when(mockCdsFrontendDataCache.registrationDetails(mockRequest))
        .thenReturn(Future.successful(mockEmbassyRegistrationDetails))

      val (subscriptionPage, session) = await(controller.startSubscriptionFlow(eoriOnlyService)(mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(EmbassySubscriptionFlow, RegistrationConfirmPage.url(eoriOnlyService))(mockRequest)
    }
  }
}
