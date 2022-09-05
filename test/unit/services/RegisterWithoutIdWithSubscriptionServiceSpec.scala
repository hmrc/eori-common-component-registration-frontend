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

package unit.services

import base.UnitSpec
import java.time.{LocalDate, LocalDateTime, ZoneId}

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, Request, Results}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.Sub02Controller
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.ResponseCommon._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{
  RegisterWithoutIdService,
  RegisterWithoutIdWithSubscriptionService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RegisterWithoutIdWithSubscriptionServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {
  private val mockRegisterWithoutIdService = mock[RegisterWithoutIdService]
  private val mockSessionCache             = mock[SessionCache]
  private val mockRequestSessionData       = mock[RequestSessionData]
  private val mockSub02Controller          = mock[Sub02Controller]
  private val mockOrgTypeLookup            = mock[OrgTypeLookup]
  private val mockRegistrationDetails      = mock[RegistrationDetails]

  private implicit val hc: HeaderCarrier       = mock[HeaderCarrier]
  private implicit val rq: Request[AnyContent] = mock[Request[AnyContent]]

  private val loggedInUserId   = java.util.UUID.randomUUID.toString
  private val mockLoggedInUser = mock[LoggedInUserWithEnrolments]
  private val emulatedFailure  = new RuntimeException("something bad happened")

  private val okResponse = RegisterWithoutIDResponse(
    ResponseCommon(StatusOK, Some("All OK"), LocalDateTime.now(ZoneId.of("Europe/London"))),
    Some(RegisterWithoutIdResponseDetail("TestSafeId", None))
  )

  private val notOKResponse = RegisterWithoutIDResponse(
    ResponseCommon(StatusNotOK, Some("Something went wrong"), LocalDateTime.now(ZoneId.of("Europe/London"))),
    Some(RegisterWithoutIdResponseDetail("TestSafeId", None))
  )

  private val contactDetails =
    ContactDetailsModel("John Doe", "john@example.com", "441234987654private ", None, true, None, None, None, None)

  private val service = new RegisterWithoutIdWithSubscriptionService(
    mockRegisterWithoutIdService,
    mockSessionCache,
    mockRequestSessionData,
    mockOrgTypeLookup,
    mockSub02Controller
  )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockLoggedInUser.userId()).thenReturn(loggedInUserId)
    when(mockSessionCache.saveRegistrationDetails(any[RegistrationDetails])(any[Request[_]]))
      .thenReturn(Future.successful(true))
    mockSessionCacheRegistrationDetails()
    when(mockRegistrationDetails.safeId).thenReturn(SafeId(""))
  }

  override protected def afterEach(): Unit = {
    reset(
      mockRegisterWithoutIdService,
      mockSessionCache,
      mockRequestSessionData,
      mockOrgTypeLookup,
      mockSub02Controller,
      mockRegistrationDetails
    )

    super.afterEach()
  }

  private def mockRegisterWithoutIdOKResponse() = {
    when(
      mockRegisterWithoutIdService.registerOrganisation(
        anyString(),
        any[Address],
        any[Option[ContactDetailsModel]],
        any[LoggedInUserWithEnrolments],
        any[Option[CdsOrganisationType]]
      )(any[HeaderCarrier], any[Request[_]])
    ).thenReturn(Future.successful(okResponse), Nil: _*)
    when(
      mockRegisterWithoutIdService.registerIndividual(any(), any(), any(), any(), any())(
        any[Request[_]],
        any[HeaderCarrier]
      )
    )
      .thenReturn(Future.successful(okResponse), Nil: _*)
  }

  private def mockRegisterWithoutIdNotOKResponse() = {
    when(
      mockRegisterWithoutIdService.registerOrganisation(
        anyString(),
        any[Address],
        any[Option[ContactDetailsModel]],
        any[LoggedInUserWithEnrolments],
        any[Option[CdsOrganisationType]]
      )(any[HeaderCarrier], any[Request[_]])
    ).thenReturn(Future.successful(notOKResponse), Nil: _*)
    when(
      mockRegisterWithoutIdService.registerIndividual(any(), any(), any(), any(), any())(
        any[Request[_]],
        any[HeaderCarrier]
      )
    )
      .thenReturn(Future.successful(notOKResponse), Nil: _*)
  }

  private def mockRegisterWithoutIdFailure() = {
    when(
      mockRegisterWithoutIdService.registerOrganisation(
        anyString(),
        any[Address],
        any[Option[ContactDetailsModel]],
        any[LoggedInUserWithEnrolments],
        any[Option[CdsOrganisationType]]
      )(any[HeaderCarrier], any[Request[_]])
    ).thenReturn(Future.failed(emulatedFailure))
    when(
      mockRegisterWithoutIdService.registerIndividual(any(), any(), any(), any(), any())(
        any[Request[_]],
        any[HeaderCarrier]
      )
    )
      .thenReturn(Future.failed(emulatedFailure))
  }

  private def mockSessionCacheRegistrationDetails() = {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockRegistrationDetails))
    when(mockRegistrationDetails.name).thenReturn("orgName")
    when(mockRegistrationDetails.address)
      .thenReturn(Address("add1", Some("add2"), Some("add3"), Some("add4"), Some("postcode"), "country"))
  }

  private def mockSessionCacheSubscriptionDetails() =
    when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(
      Future.successful(
        SubscriptionDetails(
          nameDobDetails =
            Some(NameDobMatchModel("firstName", Some("middleName"), "lastName", LocalDate.of(1980, 3, 31))),
          contactDetails = Some(contactDetails)
        )
      )
    )

  "RegisterWithoutIdWithSubscriptionService" should {

    "when UK, call SUB02, do not call registerOrganisation or registerIndividual" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some(UserLocation.Uk))
      when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]])).thenReturn(Some(CorporateBody))
      mockRegisterWithoutIdOKResponse()
      mockSub02ControllerCall()

      await(service.rowRegisterWithoutIdWithSubscription(mockLoggedInUser, atarService)(hc, rq))

      verify(mockSub02Controller, times(1)).subscribe(any())
      verify(mockRegisterWithoutIdService, never).registerOrganisation(anyString(), any(), any(), any(), any())(
        any(),
        any()
      )
      verify(mockRegisterWithoutIdService, never).registerIndividual(any(), any(), any(), any(), any())(any(), any())
    }

    "when CorporateBody and ROW and GYE, call SUB02, do not call Register without id" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))
      when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]])).thenReturn(Some(CorporateBody))
      when(mockRegistrationDetails.safeId).thenReturn(SafeId("SAFEID"))
      mockRegisterWithoutIdOKResponse()
      mockSub02ControllerCall()

      await(service.rowRegisterWithoutIdWithSubscription(mockLoggedInUser, atarService)(hc, rq))

      verify(mockSub02Controller, times(1)).subscribe(any())
      verify(mockRegisterWithoutIdService, never).registerOrganisation(anyString(), any(), any(), any(), any())(
        any(),
        any()
      )
      verify(mockRegisterWithoutIdService, never).registerIndividual(any(), any(), any(), any(), any())(any(), any())
    }

    "when NA and ROW, call SUB02, call registerIndividual, do not call registerOrganisation" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))
      when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]])).thenReturn(Some(NA))
      mockRegisterWithoutIdOKResponse()
      mockSub02ControllerCall()
      mockSessionCacheRegistrationDetails()
      mockSessionCacheSubscriptionDetails()

      await(service.rowRegisterWithoutIdWithSubscription(mockLoggedInUser, atarService)(hc, rq))

      verify(mockRegisterWithoutIdService, times(1)).registerIndividual(any(), any(), any(), any(), any())(any(), any())
      verify(mockSub02Controller, times(1)).subscribe(any())
      verify(mockRegisterWithoutIdService, never).registerOrganisation(anyString(), any(), any(), any(), any())(
        any(),
        any()
      )
      verify(mockSessionCache, times(2)).registrationDetails(any())
      verify(mockSessionCache).subscriptionDetails(any())
    }

    "when CorporateBody and ROW, call Register without id Successfully, then call SUB02" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))
      when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]])).thenReturn(Some(CorporateBody))
      mockSessionCacheRegistrationDetails()
      mockSessionCacheSubscriptionDetails()
      mockRegisterWithoutIdOKResponse()
      mockSub02ControllerCall()

      await(service.rowRegisterWithoutIdWithSubscription(mockLoggedInUser, atarService)(hc, rq))

      verify(mockSub02Controller, times(1)).subscribe(any[Service])
      verify(mockRegisterWithoutIdService, times(1)).registerOrganisation(
        anyString(),
        any(),
        meq(Some(contactDetails)),
        any(),
        any()
      )(any(), any())
      verify(mockRegisterWithoutIdService, never).registerIndividual(any(), any(), any(), any(), any())(any(), any())
      verify(mockSessionCache, times(2)).registrationDetails(any())
      verify(mockSessionCache).subscriptionDetails(any())
    }

    "when CorporateBody and ROW, call Register without id which fails, do not call SUB02" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))
      when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]])).thenReturn(Some(CorporateBody))
      mockSessionCacheRegistrationDetails()
      mockSessionCacheSubscriptionDetails()

      mockRegisterWithoutIdFailure()

      val thrown = the[RuntimeException] thrownBy {
        await(service.rowRegisterWithoutIdWithSubscription(mockLoggedInUser, atarService)(hc, rq))
      }
      thrown shouldBe emulatedFailure
    }

    "when CorporateBody and ROW, call register without id, which returns NotOK status, do not call Sub02" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))
      when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]])).thenReturn(Some(CorporateBody))
      mockSessionCacheRegistrationDetails()
      mockSessionCacheSubscriptionDetails()

      mockRegisterWithoutIdNotOKResponse()

      the[RuntimeException] thrownBy {
        await(service.rowRegisterWithoutIdWithSubscription(mockLoggedInUser, atarService)(hc, rq))
      } should have message "Registration of organisation FAILED"
    }
  }

  private def mockSub02ControllerCall() {
    val mockAction = mock[Action[AnyContent]]
    when(mockAction.apply(any[Request[AnyContent]])).thenReturn(Future.successful(Results.Ok))
    when(mockSub02Controller.subscribe(any())).thenReturn(mockAction)
  }

}
