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

package unit.controllers.registration

import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  SUB09SubscriptionDisplayConnector,
  ServiceUnavailableResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.SubscriptionRecoveryController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{RecipientDetails, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RandomUUIDGenerator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  HandleSubscriptionService,
  SubscriptionDetailsService,
  TaxEnrolmentsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.recovery_registration_exists
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SubscriptionInfoBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class SubscriptionRecoveryControllerSpec
    extends ControllerSpec with MockitoSugar with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                     = mock[AuthConnector]
  private val mockAuthAction                        = authAction(mockAuthConnector)
  private val mockSessionCache: SessionCache        = mock[SessionCache]
  private val mockSUB09SubscriptionDisplayConnector = mock[SUB09SubscriptionDisplayConnector]
  private val mockSub01Outcome                      = mock[Sub01Outcome]
  private val mockHandleSubscriptionService         = mock[HandleSubscriptionService]
  private val mockOrgRegistrationDetails            = mock[RegistrationDetailsOrganisation]
  private val mockSubscriptionDetailsHolder         = mock[SubscriptionDetails]
  private val mockRegisterWithEoriAndIdResponse     = mock[RegisterWithEoriAndIdResponse]
  private val mockRandomUUIDGenerator               = mock[RandomUUIDGenerator]
  private val contactDetails                        = mock[ContactDetailsModel]
  private val mockTaxEnrolmentsService              = mock[TaxEnrolmentsService]
  private val mockSubscriptionDetailsService        = mock[SubscriptionDetailsService]
  private val mockRequestSessionData                = mock[RequestSessionData]

  private val errorTemplateView = instanceOf[error_template]
  private val alreadyHaveEori   = instanceOf[recovery_registration_exists]

  private val controller = new SubscriptionRecoveryController(
    mockAuthAction,
    mockHandleSubscriptionService,
    mockTaxEnrolmentsService,
    mockSessionCache,
    mockSUB09SubscriptionDisplayConnector,
    mcc,
    errorTemplateView,
    mockRandomUUIDGenerator,
    mockRequestSessionData,
    mockSubscriptionDetailsService,
    alreadyHaveEori
  )(global)

  def registerWithEoriAndIdResponseDetail: Option[RegisterWithEoriAndIdResponseDetail] = {
    val trader               = Trader(fullName = "New trading", shortName = "nt")
    val establishmentAddress = EstablishmentAddress(streetAndNumber = "new street", city = "leeds", countryCode = "GB")
    val responseData: ResponseData = ResponseData(
      SAFEID = "SomeSafeId",
      trader = trader,
      establishmentAddress = establishmentAddress,
      hasInternetPublication = true,
      startDate = "2018-01-01"
    )
    Some(
      RegisterWithEoriAndIdResponseDetail(
        outcome = Some("PASS"),
        caseNumber = Some("case no 1"),
        responseData = Some(responseData)
      )
    )
  }

  override def beforeEach: Unit = {
    reset(
      mockSessionCache,
      mockOrgRegistrationDetails,
      mockRequestSessionData,
      mockSubscriptionDetailsService,
      mockTaxEnrolmentsService
    )
    when(mockRandomUUIDGenerator.generateUUIDAsString).thenReturn("MOCKUUID12345")
  }

  "Viewing the Organisation Name Matching form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.complete(atarService, Journey.Register)
    )
    def setupMockCommon() = {
      when(mockSessionCache.subscriptionDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(mockSubscriptionDetailsHolder))
      when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(fullyPopulatedResponse)))
      when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(Some(contactDetails))
      when(contactDetails.emailAddress).thenReturn("test@example.com")
      when(mockSubscriptionDetailsHolder.email).thenReturn(Some("test@example.com"))
      when(mockSessionCache.email(any[HeaderCarrier])).thenReturn(Future.successful("test@example.com"))

      when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))
      when(mockSub01Outcome.processedDate).thenReturn("01 May 2016")

      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))
      when(
        mockHandleSubscriptionService.handleSubscription(
          anyString,
          any[RecipientDetails],
          any[TaxPayerId],
          any[Option[Eori]],
          any[Option[DateTime]],
          any[SafeId]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(result = ()))
      when(mockSubscriptionDetailsHolder.nameDobDetails)
        .thenReturn(Some(NameDobMatchModel("fname", Some("mName"), "lname", LocalDate.parse("2019-01-01"))))
    }

    "call Enrolment Complete with successful SUB09 call for Get Your EORI journey" in {

      setupMockCommon()
      when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testsafeId"))
      when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      callEnrolmentComplete(journey = Journey.Register) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/register/complete")
      }
      verify(mockTaxEnrolmentsService, times(0))
        .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])

    }

    "call Enrolment Complete with successful SUB09 call for Subscription UK journey" in {
      setupMockCommon()

      when(mockSubscriptionDetailsHolder.eoriNumber).thenReturn(Some("testEORInumber"))

      when(mockSessionCache.registerWithEoriAndIdResponse(any[HeaderCarrier]))
        .thenReturn(Future.successful(mockRegisterWithEoriAndIdResponse))
      when(mockRegisterWithEoriAndIdResponse.responseDetail).thenReturn(registerWithEoriAndIdResponseDetail)

      when(
        mockTaxEnrolmentsService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))

      val expectedFormBundleId = fullyPopulatedResponse.responseCommon.returnParameters
        .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue)).get + "atar"

      callEnrolmentComplete(journey = Journey.Subscribe) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/subscribe/complete")
      }

      verify(mockTaxEnrolmentsService).issuerCall(
        meq(expectedFormBundleId),
        meq(Eori("testEORInumber")),
        any[Option[LocalDate]],
        meq(atarService)
      )(any[HeaderCarrier])

      verify(mockHandleSubscriptionService).handleSubscription(
        meq(expectedFormBundleId),
        any(),
        any(),
        meq(Some(Eori("testEORInumber"))),
        any(),
        any()
      )(any())
    }

    "call Enrolment Complete with successful SUB09 call for Subscription ROW journey" in {
      setupMockCommon()

      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some("eu"))
      when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(Utr("someUtr"))))
      when(mockSubscriptionDetailsHolder.eoriNumber).thenReturn(Some("testEORInumber2"))
      when(mockSessionCache.registerWithEoriAndIdResponse(any[HeaderCarrier]))
        .thenReturn(Future.successful(mockRegisterWithEoriAndIdResponse))
      when(mockRegisterWithEoriAndIdResponse.responseDetail).thenReturn(registerWithEoriAndIdResponseDetail)

      when(
        mockTaxEnrolmentsService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))

      val expectedFormBundleId = fullyPopulatedResponse.responseCommon.returnParameters
        .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue)).get + "atar"

      callEnrolmentComplete(journey = Journey.Subscribe) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/subscribe/complete")
      }
      verify(mockTaxEnrolmentsService).issuerCall(
        meq(expectedFormBundleId),
        meq(Eori("testEORInumber2")),
        any[Option[LocalDate]],
        meq(atarService)
      )(any[HeaderCarrier])

      verify(mockHandleSubscriptionService).handleSubscription(
        meq(expectedFormBundleId),
        any(),
        any(),
        meq(Some(Eori("testEORInumber"))),
        any(),
        any()
      )(any())
    }
    "call Enrolment Complete with successful SUB09 call for Subscription ROW journey without Identifier" in {
      setupMockCommon()

      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some("eu"))
      when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsHolder.eoriNumber).thenReturn(Some("testEORInumber3"))
      when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testsafeId"))

      when(
        mockTaxEnrolmentsService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))

      val expectedFormBundleId = fullyPopulatedResponse.responseCommon.returnParameters
        .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue)).get + "atar"

      callEnrolmentComplete(journey = Journey.Subscribe) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/subscribe/complete")
      }
      verify(mockTaxEnrolmentsService).issuerCall(
        meq(expectedFormBundleId),
        meq(Eori("testEORInumber3")),
        any[Option[LocalDate]],
        meq(atarService)
      )(any[HeaderCarrier])

      verify(mockHandleSubscriptionService).handleSubscription(
        meq(expectedFormBundleId),
        any(),
        any(),
        meq(Some(Eori("testEORInumber"))),
        any(),
        any()
      )(any())
    }

    "call Enrolment Complete with unsuccessful SUB09 call" in {
      when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))

      when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ServiceUnavailableResponse)))

      when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))
      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))
      when(
        mockHandleSubscriptionService.handleSubscription(
          anyString,
          any[RecipientDetails],
          any[TaxPayerId],
          any[Option[Eori]],
          any[Option[DateTime]],
          any[SafeId]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(result = ()))

      callEnrolmentComplete(journey = Journey.Register) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "call Enrolment Complete with successful SUB09 call with empty ResponseCommon should throw IllegalArgumentException" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(fullyPopulatedResponseWithBlankReturnParameters)))
    when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))

    the[IllegalStateException] thrownBy {
      callEnrolmentComplete(journey = Journey.Register) { result =>
        await(result)
      }
    } should have message "NO ETMPFORMBUNDLENUMBER specified"
  }

  "call Enrolment Complete with successful SUB09 call with ResponseCommon with no ETMPFORMBUNDLENUMBER should throw IllegalStateException" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(fullyPopulatedResponseWithNoETMPFORMBUNDLENUMBER)))
    when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))

    the[IllegalStateException] thrownBy {
      callEnrolmentComplete(journey = Journey.Register) { result =>
        await(result)
      }
    } should have message "NO ETMPFORMBUNDLENUMBER specified"
  }

  "call Enrolment Complete with successful SUB09 call with empty ContactDetails should show existing EORI" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithoutContactDetails)))
    when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))

    callEnrolmentComplete(journey = Journey.Register) { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/register/eori-exist")
    }
  }

  "call Enrolment Complete with successful SUB09 call without EmailAddress should show existing EORI" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithoutEmailAddress)))
    when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))

    callEnrolmentComplete(journey = Journey.Register) { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/register/eori-exist")
    }
  }

  "call Enrolment Complete with successful SUB09 call with un-verified EmailAddress should show existing EORI" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithUnverifiedEmailAddress)))
    when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))

    callEnrolmentComplete(journey = Journey.Register) { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/register/eori-exist")
    }
  }

  "should show existing EORI" in {
    when(mockSessionCache.eori(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some("GB132123231223")))

    callExistingEori(journey = Journey.Register) { result =>
      status(result) shouldBe OK
      val page = CdsPage(contentAsString(result))
      page.getElementsText("//*[@id='page-heading']") shouldBe "You already have an EORI"
      page.getElementsText("//*[@id='eori-number']") shouldBe "EORI number: GB132123231223"
    }
  }

  "call Enrolment Complete with successful SUB09 call without personOfContact should not throw exception" in {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithoutPersonOfContact)))
    when(mockSessionCache.sub01Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSub01Outcome))
    when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier]))
      .thenReturn(Future.successful(true))
    when(
      mockHandleSubscriptionService.handleSubscription(
        anyString,
        any[RecipientDetails],
        any[TaxPayerId],
        any[Option[Eori]],
        any[Option[DateTime]],
        any[SafeId]
      )(any[HeaderCarrier])
    ).thenReturn(Future.successful(result = ()))

    callEnrolmentComplete(journey = Journey.Register) { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-enrolment-services/atar/register/complete")
    }
  }

  def callEnrolmentComplete(userId: String = defaultUserId, journey: Journey.Value)(test: Future[Result] => Any) {

    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.complete(atarService, journey).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  def callExistingEori(userId: String = defaultUserId, journey: Journey.Value)(test: Future[Result] => Any) {

    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.eoriExist(atarService, journey).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
