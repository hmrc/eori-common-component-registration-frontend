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

import org.mockito.ArgumentMatchers.{any, anyString, contains, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  SUB09SubscriptionDisplayConnector,
  ServiceUnavailableResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionRecoveryController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{RecipientDetails, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{
  HandleSubscriptionService,
  RandomUUIDGenerator,
  TaxEnrolmentsService,
  UpdateVerifiedEmailService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, recovery_registration_exists}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SubscriptionInfoBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.{LocalDate, LocalDateTime}
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
  private val mockTaxEnrolmentService               = mock[TaxEnrolmentsService]
  private val mockOrgRegistrationDetails            = mock[RegistrationDetailsOrganisation]
  private val mockSubscriptionDetailsHolder         = mock[SubscriptionDetails]
  private val mockRandomUUIDGenerator               = mock[RandomUUIDGenerator]
  private val contactDetails                        = mock[ContactDetailsModel]
  private val mockRequestSessionData                = mock[RequestSessionData]
  private val mockUpdateVerifiedEmailService        = mock[UpdateVerifiedEmailService]
  private val errorTemplateView                     = instanceOf[error_template]
  private val alreadyHaveEori                       = instanceOf[recovery_registration_exists]

  private val controller = new SubscriptionRecoveryController(
    mockAuthAction,
    mockHandleSubscriptionService,
    mockTaxEnrolmentService,
    mockUpdateVerifiedEmailService,
    mockSessionCache,
    mockSUB09SubscriptionDisplayConnector,
    mcc,
    errorTemplateView,
    mockRandomUUIDGenerator,
    mockRequestSessionData,
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

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionCache)
    reset(mockOrgRegistrationDetails)
    reset(mockRequestSessionData)
    reset(mockHandleSubscriptionService)
    reset(mockTaxEnrolmentService)

    when(mockRandomUUIDGenerator.generateUUIDAsString).thenReturn("MOCKUUID12345")
  }

  override protected def afterEach(): Unit = {
    reset(mockSessionCache)
    reset(mockOrgRegistrationDetails)
    reset(mockRequestSessionData)

    super.afterEach()
  }

  "Viewing the Organisation Name Matching form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.complete(atarService))
    def setupMockCommon() = {
      when(mockSessionCache.subscriptionDetails(any[Request[_]]))
        .thenReturn(Future.successful(mockSubscriptionDetailsHolder))
      when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(fullyPopulatedResponse)))
      when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(Some(contactDetails))
      when(contactDetails.emailAddress).thenReturn("test@example.com")
      when(mockSubscriptionDetailsHolder.email).thenReturn(Some("test@example.com"))
      when(mockSessionCache.email(any[Request[_]])).thenReturn(Future.successful("test@example.com"))

      when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))
      when(mockSub01Outcome.processedDate).thenReturn("01 May 2016")

      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      when(
        mockHandleSubscriptionService.handleSubscription(
          anyString,
          any[RecipientDetails],
          any[TaxPayerId],
          any[Option[Eori]],
          any[Option[LocalDateTime]],
          any[SafeId]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(result = ()))
      when(mockSubscriptionDetailsHolder.nameDobDetails)
        .thenReturn(Some(NameDobMatchModel("fname", "lname", LocalDate.parse("2019-01-01"))))
    }

    "call Enrolment Complete with successful SUB09 call for Get Your EORI journey" in {

      setupMockCommon()
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testsafeId"))
      when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      when(
        mockTaxEnrolmentService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))

      val expectedFormBundleId = fullyPopulatedResponse.responseCommon.returnParameters
        .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue))
        .get

      callEnrolmentComplete() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/complete")
      }
      verify(mockTaxEnrolmentService).issuerCall(
        contains(expectedFormBundleId),
        meq(Eori("12345")),
        any[Option[LocalDate]],
        meq(atarService)
      )(any[HeaderCarrier])

    }

    "call Enrolment Complete with successful SUB09 call for Get Your EORI journey using CDS formBundle enrichment when service is CDS" in {

      setupMockCommon()
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testsafeId"))
      when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      when(
        mockTaxEnrolmentService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))

      val expectedFormBundleId = fullyPopulatedResponse.responseCommon.returnParameters
        .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue))
        .get

      callEnrolmentComplete() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/complete")
      }
      verify(mockTaxEnrolmentService).issuerCall(
        contains(expectedFormBundleId),
        meq(Eori("12345")),
        any[Option[LocalDate]],
        meq(atarService)
      )(any[HeaderCarrier])

    }

    "call Enrolment Complete with no email verification( SUB22) with successful SUB09 call for Get Your EORI  for non cds services " in {

      setupMockCommon()
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testsafeId"))
      when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      when(
        mockTaxEnrolmentService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))
      callEnrolmentComplete() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/complete")
      }

      verify(mockUpdateVerifiedEmailService, never()).updateVerifiedEmail(any(), any())(any[HeaderCarrier])
    }
    "call Enrolment Complete with email verification( SUB22) triggered with successful SUB09 call for Get Your EORI  for cds journey " in {

      setupMockCommon()
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testsafeId"))
      when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      when(mockUpdateVerifiedEmailService.updateVerifiedEmail(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))
      when(
        mockTaxEnrolmentService
          .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))
      callEnrolmentCDSComplete() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result) shouldBe Some("/customs-registration-services/cds/register/complete")
      }

      verify(mockUpdateVerifiedEmailService, times(1)).updateVerifiedEmail(any(), any())(any[HeaderCarrier])
    }

    "call Enrolment Complete with unsuccessful SUB09 call" in {
      when(mockSessionCache.registrationDetails(any[Request[_]]))
        .thenReturn(Future.successful(mockOrgRegistrationDetails))
      when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))

      when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ServiceUnavailableResponse)))

      when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))
      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      when(
        mockHandleSubscriptionService.handleSubscription(
          anyString,
          any[RecipientDetails],
          any[TaxPayerId],
          any[Option[Eori]],
          any[Option[LocalDateTime]],
          any[SafeId]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(result = ()))

      callEnrolmentComplete() { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "call Enrolment Complete with successful SUB09 call with empty ResponseCommon should throw IllegalArgumentException" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(fullyPopulatedResponseWithBlankReturnParameters)))
    when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))

    the[IllegalStateException] thrownBy {
      callEnrolmentComplete() { result =>
        await(result)
      }
    } should have message "NO ETMPFORMBUNDLENUMBER specified"
  }

  "call Enrolment Complete with successful SUB09 call with ResponseCommon with no ETMPFORMBUNDLENUMBER should throw IllegalStateException" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(fullyPopulatedResponseWithNoETMPFORMBUNDLENUMBER)))
    when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))

    the[IllegalStateException] thrownBy {
      callEnrolmentComplete() { result =>
        await(result)
      }
    } should have message "NO ETMPFORMBUNDLENUMBER specified"
  }

  "call Enrolment Complete with successful SUB09 call with empty ContactDetails should show existing EORI" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithoutContactDetails)))
    when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))

    callEnrolmentComplete() { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/eori-exist")
    }
  }

  "call Enrolment Complete with successful SUB09 call without EmailAddress should show existing EORI" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithoutEmailAddress)))
    when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))

    callEnrolmentComplete() { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/eori-exist")
    }
  }

  "call Enrolment Complete with successful SUB09 call with un-verified EmailAddress should show existing EORI" in {
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithUnverifiedEmailAddress)))
    when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))

    callEnrolmentComplete() { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/eori-exist")
    }
  }

  "should show existing EORI" in {
    when(mockSessionCache.eori(any[Request[_]]))
      .thenReturn(Future.successful(Some("GB132123231223")))

    callExistingEori() { result =>
      status(result) shouldBe OK
      val page = CdsPage(contentAsString(result))
      page.getElementsText("//*[@id='page-heading']") shouldBe "You already have an EORI"
      page.getElementsText("//*[@id='eori-number']") shouldBe "EORI number: GB132123231223"
    }
  }

  "call Enrolment Complete with successful SUB09 call without personOfContact should not throw exception" in {

    when(mockSessionCache.subscriptionDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockSubscriptionDetailsHolder))

    when(mockSubscriptionDetailsHolder.contactDetails).thenReturn(Some(contactDetails))
    when(contactDetails.emailAddress).thenReturn("test@example.com")
    when(mockSubscriptionDetailsHolder.email).thenReturn(Some("test@example.com"))
    when(mockSessionCache.email(any[Request[_]])).thenReturn(Future.successful("test@example.com"))

    when(mockSub01Outcome.processedDate).thenReturn("01 May 2016")

    when(mockSubscriptionDetailsHolder.nameDobDetails)
      .thenReturn(Some(NameDobMatchModel("fname", "lname", LocalDate.parse("2019-01-01"))))
    when(mockSessionCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockOrgRegistrationDetails))
    when(mockOrgRegistrationDetails.safeId).thenReturn(SafeId("testSapNumber"))
    when(mockSessionCache.saveEori(any[Eori])(any[Request[_]]))
      .thenReturn(Future.successful(true))

    when(mockSUB09SubscriptionDisplayConnector.subscriptionDisplay(any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(responseWithoutPersonOfContact)))
    when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSub01Outcome))
    when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
      .thenReturn(Future.successful(true))
    when(
      mockHandleSubscriptionService.handleSubscription(
        anyString,
        any[RecipientDetails],
        any[TaxPayerId],
        any[Option[Eori]],
        any[Option[LocalDateTime]],
        any[SafeId]
      )(any[HeaderCarrier])
    ).thenReturn(Future.successful(result = ()))
    val expectedFormBundleId = fullyPopulatedResponse.responseCommon.returnParameters
      .flatMap(_.find(_.paramName.equals("ETMPFORMBUNDLENUMBER")).map(_.paramValue))
      .get
    when(
      mockTaxEnrolmentService
        .issuerCall(anyString, any[Eori], any[Option[LocalDate]], any[Service])(any[HeaderCarrier])
    ).thenReturn(Future.successful(NO_CONTENT))

    callEnrolmentComplete() { result =>
      status(result) shouldBe SEE_OTHER
      header(LOCATION, result) shouldBe Some("/customs-registration-services/atar/register/complete")
    }

    verify(mockTaxEnrolmentService).issuerCall(
      contains(expectedFormBundleId),
      meq(Eori("12345")),
      any[Option[LocalDate]],
      meq(atarService)
    )(any[HeaderCarrier])

  }

  def callEnrolmentComplete(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {

    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.complete(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  def callEnrolmentCDSComplete(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {

    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.complete(cdsService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  def callExistingEori(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {

    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.eoriExist(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
