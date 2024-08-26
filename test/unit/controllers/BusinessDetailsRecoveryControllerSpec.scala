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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import play.api.mvc.{AnyContent, Request, Result, Session}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  ContactDetailsController,
  DateOfEstablishmentController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{BusinessDetailsRecoveryController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  BusinessDetailsRecoveryPage,
  ContactDetailsSubscriptionFlowPageGetEori,
  DateOfEstablishmentSubscriptionFlowPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  DataUnavailableException,
  RequestSessionData,
  SessionCache,
  SessionCacheService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.business_details_recovery
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder.{organisationRegistrationDetails, soleTraderRegistrationDetails}
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BusinessDetailsRecoveryControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector           = mock[AuthConnector]
  private val mockAuthAction              = authAction(mockAuthConnector)
  private val mockRequestSessionData      = mock[RequestSessionData]
  private val mockSessionCache            = mock[SessionCache]
  private val mockOrgTypeLookup           = mock[OrgTypeLookup]
  private val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  private val mockSave4LaterService       = mock[Save4LaterService]
  private val mockSessionCacheService     = instanceOf[SessionCacheService]

  private val businessDetailsRecoveryView = instanceOf[business_details_recovery]

  private val organisationDetails = RegistrationDetailsOrganisation(
    customsId = Some(Eori("ZZZ1ZZZZ23ZZZZZZZ")),
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    dateOfEstablishment = None,
    etmpOrganisationType = Some(CorporateBody)
  )

  private val individualDetails = RegistrationDetailsIndividual(
    customsId = None,
    TaxPayerId("sapNumber"),
    safeId = SafeId("safe-id"),
    "name",
    Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "GB"),
    LocalDate.parse("2011-11-11")
  )

  private val controller = new BusinessDetailsRecoveryController(
    mockAuthAction,
    mockRequestSessionData,
    mockSessionCache,
    mcc,
    businessDetailsRecoveryView,
    mockSave4LaterService,
    mockSubscriptionFlowManager,
    mockSessionCacheService
  )(global)

  "Recovery details" should {
    "display registered name when entityType Organisation found in cache with safeId" in {
      mockCacheWithRegistrationDetails(organisationRegistrationDetails)
      when(
        mockOrgTypeLookup
          .etmpOrgType(any[Request[AnyContent]])
      ).thenReturn(Future.successful(CorporateBody))
      invokeConfirm() { result =>
        status(result) shouldBe OK
      }
    }

    "display registered name when entityType Individual is found in cache with safeId" in {
      mockCacheWithRegistrationDetails(soleTraderRegistrationDetails)
      invokeConfirm() { result =>
        status(result) shouldBe OK
      }
    }

    "display registered name when entityType Individual is found in cache with safeId1" in {
      mockCacheWithRegistrationDetails(RegistrationDetails.rdSafeId(SafeId("safeId")))
      intercept[IllegalArgumentException](invokeConfirm() { result =>
        status(result) shouldBe OK
      })
    }

    val locations = Seq(UserLocation.ThirdCountry, UserLocation.Iom, UserLocation.Islands)

    locations foreach { location =>
      assertAndTestBasedOnTheLocationForIndividual(location)
      assertAndTestBasedOnTheLocationForOrganisation(location)
    }
    assertAndTestBasedOnTheLocationForSafeIdDetails()
    assertAndTestThrowsExceptionForInvalidLocationOrganisation()
  }

  private def assertAndTestBasedOnTheLocationForIndividual(location: UserLocation): Unit =
    s"redirect to contactDetailsPage when orgType is found in cache for Individual and location is selected to $location" in {
      val mockSession = mock[Session]
      val mockFlowStart =
        (ContactDetailsSubscriptionFlowPageGetEori, mockSession)

      when(
        mockSubscriptionFlowManager.startSubscriptionFlow(
          meq(Some(BusinessDetailsRecoveryPage)),
          meq(CdsOrganisationType.ThirdCountryIndividual),
          meq(atarService)
        )(any[Request[AnyContent]])
      ).thenReturn(Future.successful(mockFlowStart))
      mockCacheWithRegistrationDetails(individualDetails)
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(
        UserLocation.enumerable.withName(location)
      )
      when(mockSave4LaterService.fetchOrgType(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(CdsOrganisationType("third-country-individual"))))

      invokeContinue() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith(ContactDetailsController.createForm(atarService).url)
      }
    }

  private def assertAndTestBasedOnTheLocationForSafeIdDetails(): Unit =
    s"throw an exception" in {
      val mockSession = mock[Session]
      val mockFlowStart =
        (ContactDetailsSubscriptionFlowPageGetEori, mockSession)

      when(
        mockSubscriptionFlowManager.startSubscriptionFlow(
          meq(Some(BusinessDetailsRecoveryPage)),
          meq(CdsOrganisationType.ThirdCountryIndividual),
          meq(atarService)
        )(any[Request[AnyContent]])
      ).thenReturn(Future.successful(mockFlowStart))
      mockCacheWithRegistrationDetails(RegistrationDetails.rdSafeId(SafeId("safeId")))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(
        UserLocation.enumerable.withName(UserLocation.Islands)
      )
      when(mockSave4LaterService.fetchOrgType(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(CdsOrganisationType("third-country-individual"))))

      intercept[IllegalArgumentException](invokeContinue() { result =>
        status(result) shouldBe SEE_OTHER
      })
    }

  private def assertAndTestBasedOnTheLocationForOrganisation(location: UserLocation): Unit =
    s"redirect to dateOfEstablishment when orgType is found in cache for Organisation and location is selected to $location" in {
      val mockSession   = mock[Session]
      val mockFlowStart = (DateOfEstablishmentSubscriptionFlowPage, mockSession)

      when(
        mockSubscriptionFlowManager.startSubscriptionFlow(
          meq(Some(BusinessDetailsRecoveryPage)),
          meq(CdsOrganisationType.ThirdCountryOrganisation),
          meq(atarService)
        )(any[Request[AnyContent]])
      ).thenReturn(Future.successful(mockFlowStart))
      mockCacheWithRegistrationDetails(organisationDetails)
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(
        UserLocation.enumerable.withName(location)
      )
      when(mockSave4LaterService.fetchOrgType(any[GroupId])(any[HeaderCarrier])).thenReturn(
        Future
          .successful(Some(CdsOrganisationType("third-country-organisation")))
      )

      invokeContinue() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith(DateOfEstablishmentController.createForm(atarService).url)
      }
    }

  private def assertAndTestThrowsExceptionForInvalidLocationOrganisation(): Unit =
    "throw IllegalStateException when passing invalid location" in {
      val location: String = "invalid-location"
      val mockSession      = mock[Session]
      val mockFlowStart    = (DateOfEstablishmentSubscriptionFlowPage, mockSession)

      when(
        mockSubscriptionFlowManager.startSubscriptionFlow(
          meq(Some(BusinessDetailsRecoveryPage)),
          meq(CdsOrganisationType.ThirdCountryOrganisation),
          meq(atarService)
        )(any[Request[AnyContent]])
      ).thenReturn(Future.successful(mockFlowStart))
      mockCacheWithRegistrationDetails(organisationDetails)
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(
        UserLocation.enumerable.withName(location)
      )
      when(mockSave4LaterService.fetchOrgType(any[GroupId])(any[HeaderCarrier])).thenReturn(
        Future
          .successful(Some(CdsOrganisationType("third-country-organisation")))
      )

      invokeContinue() { result =>
        val thrown = intercept[DataUnavailableException] {
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
        thrown.getMessage shouldBe s"User Location not set"
      }
    }

  private def mockCacheWithRegistrationDetails(details: RegistrationDetails): Unit =
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(details))

  private def invokeConfirm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.form(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def invokeContinue(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.continue(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
