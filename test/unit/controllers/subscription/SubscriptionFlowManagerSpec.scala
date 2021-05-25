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

package unit.controllers.subscription

import base.UnitSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.mvc.{AnyContent, Request, Session}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{IndividualSubscriptionFlow, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubscriptionFlowManagerSpec
    extends UnitSpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ControllerSpec {

  private val mockRequestSessionData   = mock[RequestSessionData]
  private val mockCdsFrontendDataCache = mock[SessionCache]

  val controller =
    new SubscriptionFlowManager(mockRequestSessionData, mockCdsFrontendDataCache)(global)

  private val mockOrgRegistrationDetails        = mock[RegistrationDetailsOrganisation]
  private val mockIndividualRegistrationDetails = mock[RegistrationDetailsIndividual]
  private val mockSession                       = mock[Session]

  private val mockHC      = mock[HeaderCarrier]
  private val mockRequest = mock[Request[AnyContent]]

  private val mockSubscriptionFlow = mock[SubscriptionFlow]

  val noSubscriptionFlowInSessionException = new IllegalStateException("No subscription flow in session.")

  override def beforeEach(): Unit = {
    reset(mockRequestSessionData, mockSession, mockCdsFrontendDataCache)
    when(mockRequestSessionData.storeUserSubscriptionFlow(any[SubscriptionFlow], any[String])(any[Request[AnyContent]]))
      .thenReturn(mockSession)
    when(mockCdsFrontendDataCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))
  }

  "Getting current subscription flow" should {
    "return value from session when stored there before" in {
      when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(mockSubscriptionFlow)

      controller.currentSubscriptionFlow(mockRequest) shouldBe mockSubscriptionFlow
    }

    "fail when there was no flow stored in session before" in {
      when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]]))
        .thenThrow(noSubscriptionFlowInSessionException)

      intercept[IllegalStateException](
        controller.currentSubscriptionFlow(mockRequest)
      ) shouldBe noSubscriptionFlowInSessionException
    }
  }

  "Flow already started" should {
    val values = Table(
      ("flow", "currentPage", "expectedStepNumber", "expectedTotalSteps", "expectedNextPage"),
      (
        OrganisationSubscriptionFlow,
        DateOfEstablishmentSubscriptionFlowPage,
        1,
        10,
        ContactDetailsSubscriptionFlowPageGetEori
      ),
      (
        OrganisationSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        2,
        10,
        BusinessShortNameSubscriptionFlowPage
      ),
      (OrganisationSubscriptionFlow, BusinessShortNameSubscriptionFlowPage, 3, 10, SicCodeSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, SicCodeSubscriptionFlowPage, 4, 10, VatRegisteredUkSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatRegisteredUkSubscriptionFlowPage, 5, 10, VatDetailsSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatDetailsSubscriptionFlowPage, 6, 10, VatRegisteredEuSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatRegisteredEuSubscriptionFlowPage, 7, 10, VatEUIdsSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatEUIdsSubscriptionFlowPage, 8, 10, VatEUConfirmSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, VatEUConfirmSubscriptionFlowPage, 9, 10, EoriConsentSubscriptionFlowPage),
      (OrganisationSubscriptionFlow, EoriConsentSubscriptionFlowPage, 10, 10, ReviewDetailsPageGetYourEORI),
      (
        PartnershipSubscriptionFlow,
        DateOfEstablishmentSubscriptionFlowPage,
        1,
        10,
        ContactDetailsSubscriptionFlowPageGetEori
      ),
      (
        PartnershipSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        2,
        10,
        BusinessShortNameSubscriptionFlowPage
      ),
      (PartnershipSubscriptionFlow, BusinessShortNameSubscriptionFlowPage, 3, 10, SicCodeSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, SicCodeSubscriptionFlowPage, 4, 10, VatRegisteredUkSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatRegisteredUkSubscriptionFlowPage, 5, 10, VatDetailsSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatDetailsSubscriptionFlowPage, 6, 10, VatRegisteredEuSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatRegisteredEuSubscriptionFlowPage, 7, 10, VatEUIdsSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatEUIdsSubscriptionFlowPage, 8, 10, VatEUConfirmSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, VatEUConfirmSubscriptionFlowPage, 9, 10, EoriConsentSubscriptionFlowPage),
      (PartnershipSubscriptionFlow, EoriConsentSubscriptionFlowPage, 10, 10, ReviewDetailsPageGetYourEORI),
      (SoleTraderSubscriptionFlow, ContactDetailsSubscriptionFlowPageGetEori, 1, 8, SicCodeSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, SicCodeSubscriptionFlowPage, 2, 8, VatRegisteredUkSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatRegisteredUkSubscriptionFlowPage, 3, 8, VatDetailsSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatDetailsSubscriptionFlowPage, 4, 8, VatRegisteredEuSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatRegisteredEuSubscriptionFlowPage, 5, 8, VatEUIdsSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatEUIdsSubscriptionFlowPage, 6, 8, VatEUConfirmSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, VatEUConfirmSubscriptionFlowPage, 7, 8, EoriConsentSubscriptionFlowPage),
      (SoleTraderSubscriptionFlow, EoriConsentSubscriptionFlowPage, 8, 8, ReviewDetailsPageGetYourEORI),
      (IndividualSubscriptionFlow, ContactDetailsSubscriptionFlowPageGetEori, 1, 2, EoriConsentSubscriptionFlowPage),
      (IndividualSubscriptionFlow, EoriConsentSubscriptionFlowPage, 2, 2, ReviewDetailsPageGetYourEORI),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        DateOfEstablishmentSubscriptionFlowPage,
        1,
        10,
        ContactDetailsSubscriptionFlowPageGetEori
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        2,
        10,
        BusinessShortNameSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        BusinessShortNameSubscriptionFlowPage,
        3,
        10,
        SicCodeSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        SicCodeSubscriptionFlowPage,
        4,
        10,
        VatRegisteredUkSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        VatRegisteredUkSubscriptionFlowPage,
        5,
        10,
        VatDetailsSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        VatDetailsSubscriptionFlowPage,
        6,
        10,
        VatRegisteredEuSubscriptionFlowPage
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        VatRegisteredEuSubscriptionFlowPage,
        7,
        10,
        VatEUIdsSubscriptionFlowPage
      ),
      (ThirdCountryOrganisationSubscriptionFlow, VatEUIdsSubscriptionFlowPage, 8, 10, VatEUConfirmSubscriptionFlowPage),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        VatEUConfirmSubscriptionFlowPage,
        9,
        10,
        EoriConsentSubscriptionFlowPage
      ),
      (ThirdCountryOrganisationSubscriptionFlow, EoriConsentSubscriptionFlowPage, 10, 10, ReviewDetailsPageGetYourEORI),
      (
        ThirdCountryIndividualSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        1,
        2,
        EoriConsentSubscriptionFlowPage
      ),
      (ThirdCountryIndividualSubscriptionFlow, EoriConsentSubscriptionFlowPage, 2, 2, ReviewDetailsPageGetYourEORI),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        ContactDetailsSubscriptionFlowPageGetEori,
        1,
        8,
        SicCodeSubscriptionFlowPage
      ),
      (ThirdCountrySoleTraderSubscriptionFlow, SicCodeSubscriptionFlowPage, 2, 8, VatRegisteredUkSubscriptionFlowPage),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        VatRegisteredUkSubscriptionFlowPage,
        3,
        8,
        VatDetailsSubscriptionFlowPage
      ),
      (
        ThirdCountrySoleTraderSubscriptionFlow,
        VatDetailsSubscriptionFlowPage,
        4,
        8,
        VatRegisteredEuSubscriptionFlowPage
      ),
      (ThirdCountrySoleTraderSubscriptionFlow, VatRegisteredEuSubscriptionFlowPage, 5, 8, VatEUIdsSubscriptionFlowPage),
      (ThirdCountrySoleTraderSubscriptionFlow, VatEUIdsSubscriptionFlowPage, 6, 8, VatEUConfirmSubscriptionFlowPage),
      (ThirdCountrySoleTraderSubscriptionFlow, VatEUConfirmSubscriptionFlowPage, 7, 8, EoriConsentSubscriptionFlowPage),
      (ThirdCountrySoleTraderSubscriptionFlow, EoriConsentSubscriptionFlowPage, 8, 8, ReviewDetailsPageGetYourEORI),
      (OrganisationFlow, NameUtrDetailsSubscriptionFlowPage, 1, 3, DateOfEstablishmentSubscriptionFlowPageMigrate),
      (OrganisationFlow, DateOfEstablishmentSubscriptionFlowPageMigrate, 2, 3, AddressDetailsSubscriptionFlowPage),
      (OrganisationFlow, AddressDetailsSubscriptionFlowPage, 3, 3, ReviewDetailsPageSubscription),
      (SoleTraderFlow, NameDobDetailsSubscriptionFlowPage, 1, 3, HowCanWeIdentifyYouSubscriptionFlowPage),
      (SoleTraderFlow, HowCanWeIdentifyYouSubscriptionFlowPage, 2, 3, AddressDetailsSubscriptionFlowPage),
      (SoleTraderFlow, AddressDetailsSubscriptionFlowPage, 3, 3, ReviewDetailsPageSubscription)
    )

    TableDrivenPropertyChecks.forAll(values) {
      (
        flow: SubscriptionFlow,
        currentPage: SubscriptionPage,
        expectedStepNumber: Int,
        expectedTotalSteps: Int,
        expectedNextPage: SubscriptionPage
      ) =>
        when(mockRequestSessionData.userSubscriptionFlow(mockRequest)).thenReturn(flow)
        when(mockRequestSessionData.uriBeforeSubscriptionFlow(mockRequest)).thenReturn(None)
        val actual = controller.stepInformation(currentPage)(mockRequest)

        s"${flow.name} flow: current step is $expectedStepNumber when currentPage is $currentPage" in {
          actual.stepNumber shouldBe expectedStepNumber
        }

        s"${flow.name} flow: total Number of steps are $expectedTotalSteps when currentPage is $currentPage" in {
          actual.totalSteps shouldBe expectedTotalSteps
        }

        s"${flow.name} flow: next page is $expectedNextPage when currentPage is $currentPage" in {
          actual.nextPage shouldBe expectedNextPage
        }
    }
  }

  "First Page" should {

    "start Individual Subscription Flow for individual" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)

      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(mockIndividualRegistrationDetails))
      val (subscriptionPage, session) =
        await(
          controller.startSubscriptionFlow(Some(ConfirmIndividualTypePage), atarService, Journey.Register)(
            mockHC,
            mockRequest
          )
        )

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(IndividualSubscriptionFlow, ConfirmIndividualTypePage.url(atarService))(mockRequest)
    }

    "start Corporate Subscription Flow when cached registration details are for an Organisation" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)

      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService, Journey.Register)(mockHC, mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(OrganisationSubscriptionFlow, RegistrationConfirmPage.url(atarService))(mockRequest)
    }

    "start Corporate Subscription Flow when selected organisation type is Sole Trader" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest))
        .thenReturn(Some(CdsOrganisationType.SoleTrader))

      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(mockIndividualRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService, Journey.Register)(mockHC, mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession
      verify(mockRequestSessionData).storeUserSubscriptionFlow(
        SoleTraderSubscriptionFlow,
        RegistrationConfirmPage.url(atarService)
      )(mockRequest)
    }

    "start Corporate Subscription Flow when cached registration details are for an Organisation Reg-existing (a.k.a migration)" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)
      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService, Journey.Subscribe)(mockHC, mockRequest))

      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession

      verify(mockRequestSessionData)
        .storeUserSubscriptionFlow(OrganisationFlow, RegistrationConfirmPage.url(atarService))(mockRequest)
    }
  }
}

class SubscriptionFlowManagerNinoUtrEnabledSpec
    extends UnitSpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ControllerSpec {

  private val mockRequestSessionData   = mock[RequestSessionData]
  private val mockCdsFrontendDataCache = mock[SessionCache]

  val controller =
    new SubscriptionFlowManager(mockRequestSessionData, mockCdsFrontendDataCache)(global)

  private val mockSession = mock[Session]

  private val mockHC      = mock[HeaderCarrier]
  private val mockRequest = mock[Request[AnyContent]]

  val noSubscriptionFlowInSessionException = new IllegalStateException("No subscription flow in session.")

  override def beforeEach(): Unit = {
    reset(mockRequestSessionData, mockSession, mockCdsFrontendDataCache)
    when(mockRequestSessionData.storeUserSubscriptionFlow(any[SubscriptionFlow], any[String])(any[Request[AnyContent]]))
      .thenReturn(mockSession)
    when(mockCdsFrontendDataCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))
  }

  "First Page" should {

    "start Corporate Subscription Flow when selected organisation type is Sole Trader" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest))
        .thenReturn(Some(CdsOrganisationType.SoleTrader))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some(UserLocation.Eu))
      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(RegistrationDetailsIndividual()))

      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService, Journey.Subscribe)(mockHC, mockRequest))
      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession
      verify(mockRequestSessionData).storeUserSubscriptionFlow(
        RowIndividualFlow,
        RegistrationConfirmPage.url(atarService)
      )(mockRequest)
    }

    "start Corporate Subscription Flow when selected organisation type is Individual" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest))
        .thenReturn(Some(CdsOrganisationType.Individual))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some(UserLocation.Eu))
      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(RegistrationDetailsIndividual()))

      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService, Journey.Subscribe)(mockHC, mockRequest))
      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession
      verify(mockRequestSessionData).storeUserSubscriptionFlow(
        RowIndividualFlow,
        RegistrationConfirmPage.url(atarService)
      )(mockRequest)
    }

    "start Corporate Subscription Flow when cached registration details are for an Organisation" in {
      when(mockRequestSessionData.userSelectedOrganisationType(mockRequest)).thenReturn(None)
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some(UserLocation.Eu))
      when(mockCdsFrontendDataCache.registrationDetails(mockHC))
        .thenReturn(Future.successful(RegistrationDetailsOrganisation()))

      val (subscriptionPage, session) =
        await(controller.startSubscriptionFlow(atarService, Journey.Subscribe)(mockHC, mockRequest))
      subscriptionPage.isInstanceOf[SubscriptionPage] shouldBe true
      session shouldBe mockSession
      verify(mockRequestSessionData).storeUserSubscriptionFlow(
        RowOrganisationFlow,
        RegistrationConfirmPage.url(atarService)
      )(mockRequest)
    }
  }
}
