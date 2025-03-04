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

import common.pages.RegistrationCompletePage
import common.support.testdata.TestData
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result, Session}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{routes, Sub02Controller}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionCreateResponse._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class Sub02ControllerGetAnEoriSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSessionCache               = mock[SessionCache]
  private val mockCdsSubscriber              = mock[CdsSubscriber]
  private val mockCdsOrganisationType        = mock[CdsOrganisationType]
  private val mockRegDetails                 = mock[RegistrationDetails]
  private val mockSubscribeOutcome           = mock[Sub02Outcome]
  private val mockSubscribe01Outcome         = mock[Sub01Outcome]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionDetails        = mock[SubscriptionDetails]

  private val sub01OutcomeView                = inject[sub01_outcome_processing]
  private val sub02RequestNotProcessed        = inject[sub02_request_not_processed]
  private val sub02SubscriptionInProgressView = inject[sub02_subscription_in_progress]
  private val sub02EoriAlreadyAssociatedView  = inject[sub02_eori_already_associated]
  private val sub02EoriAlreadyExists          = inject[sub02_eori_already_exists]

  private val standAloneOutcomeView   = inject[standalone_subscription_outcome]
  private val subscriptionOutcomeView = inject[subscription_outcome]
  private val EORI                    = "ZZZ1ZZZZ23ZZZZZZZ"

  private val subscriptionController = new Sub02Controller(
    mockAuthAction,
    mockRequestSessionData,
    mockSessionCache,
    mockSubscriptionDetailsService,
    mcc,
    sub01OutcomeView,
    sub02RequestNotProcessed,
    sub02SubscriptionInProgressView,
    sub02EoriAlreadyAssociatedView,
    sub02EoriAlreadyExists,
    standAloneOutcomeView,
    subscriptionOutcomeView,
    mockCdsSubscriber
  )(global)

  val eoriNumberResponse: String                = "EORI-Number"
  val formBundleIdResponse: String              = "Form-Bundle-Id"
  private val processingDate                    = "12 May 2018"
  val emailVerificationTimestamp: LocalDateTime = TestData.emailVerificationTimestamp
  val emulatedFailure                           = new UnsupportedOperationException("Emulated service call failure.")

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsService.saveKeyIdentifiers(any[GroupId], any[InternalId], any[Service])(any(), any()))
      .thenReturn(Future.successful(()))
    when(mockSessionCache.journeyCompleted(any[Request[AnyContent]]))
      .thenReturn(Future.successful(true))
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockCdsSubscriber)
    reset(mockSessionCache)

    super.afterEach()
  }

  private def assertCleanedSession(result: Future[Result]): Unit = {
    val currentSession: Session = session(result)

    currentSession.data.get("selected-user-location") shouldBe None
    currentSession.data.get("subscription-flow") shouldBe None
    currentSession.data.get("selected-organisation-type") shouldBe None
    currentSession.data.get("uri-before-subscription-flow") shouldBe None
  }

  "Subscribe" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, subscriptionController.subscribe(atarService))
  }

  "End" should {
    assertNotLoggedInUserShouldBeRedirectedToLoginPage(
      mockAuthConnector,
      "Access the end page",
      subscriptionController.end(atarService)
    )
  }

  "clicking on the register button" should {

    "subscribe using selected organisation type when available" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(
        Future.successful(
          SubscriptionSuccessful(
            Eori(eoriNumberResponse),
            formBundleIdResponse,
            processingDate,
            Some(emailVerificationTimestamp)
          )
        )
      )
      subscribeForGetYourEORI(organisationTypeOption = Some(mockCdsOrganisationType)) { result =>
        await(result)
        assertCleanedSession(result)

        verify(mockCdsSubscriber).subscribeWithCachedDetails(meq(Some(mockCdsOrganisationType)), meq(atarService))(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      }
    }

    "subscribe without a selected organisation type when selection is not available (automatic matching case)" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(
        Future.successful(
          SubscriptionSuccessful(
            Eori(eoriNumberResponse),
            formBundleIdResponse,
            processingDate,
            Some(emailVerificationTimestamp)
          )
        )
      )
      subscribeForGetYourEORI(organisationTypeOption = None) { result =>
        await(result)
        assertCleanedSession(result)

        verify(mockCdsSubscriber).subscribeWithCachedDetails(meq(None), meq(atarService))(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      }
    }

    "redirect to 'Application complete' page with EORI number when subscription successful" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(
        Future.successful(
          SubscriptionSuccessful(
            Eori(eoriNumberResponse),
            formBundleIdResponse,
            processingDate,
            Some(emailVerificationTimestamp)
          )
        )
      )

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe routes.Sub02Controller.end(atarService).url
      }
    }

    "redirect to 'Registration in review' page when subscription returns pending status" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(
        Future.successful(SubscriptionPending(formBundleIdResponse, processingDate, Some(emailVerificationTimestamp)))
      )

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe routes.Sub02Controller.pending(atarService).url
      }
    }

    "redirect to 'eori already exists' page when subscription returns failed status with EORI already exists status text" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(Future.successful(SubscriptionFailed(EoriAlreadyExists, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe routes.Sub02Controller.eoriAlreadyExists(atarService).url
      }
    }

    "redirect to 'eori already associated' page when subscription returns failed status when the provided EORI already associated to Business partner record" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(Future.successful(SubscriptionFailed(EoriAlreadyAssociated, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe routes.Sub02Controller.eoriAlreadyAssociated(atarService).url
      }
    }

    "redirect to 'subscription in-progress' page when subscription returns failed status with Subscription is already in-progress status text" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(Future.successful(SubscriptionFailed(SubscriptionInProgress, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe routes.Sub02Controller.subscriptionInProgress(atarService).url
      }
    }

    "redirect to 'request not processed' page when subscription returns failed status when the request could not be processed" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(Future.successful(SubscriptionFailed(RequestNotProcessed, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        header(
          LOCATION,
          result
        ).value shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.Sub02Controller
          .requestNotProcessed(atarService)
          .url
      }
    }

    "fail when subscription fails unexpectedly" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier],
          any[Messages],
          any[Request[_]]
        )
      ).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[Exception] {
        subscribeForGetYourEORI() { result =>
          await(result)
        }
      }
      caught.getMessage shouldBe "Subscription Error. "
      caught.getCause shouldBe emulatedFailure
    }

    "allow authenticated users to access the end Subscription outcome page" in {
      invokeEndSubscriptionPageWithAuthenticatedUser() {
        mockSessionCacheForOutcomePage
        when(mockSubscribeOutcome.eori).thenReturn(Some(EORI))
        when(mockSubscriptionDetails.name).thenReturn(Some("orgName"))
        when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")
        when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(
          Future.successful(mockSubscriptionDetails)
        )
        when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribe01Outcome))
        when(mockSessionCache.sub02Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribeOutcome))
        when(mockSessionCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])).thenReturn(
          Future.successful(true)
        )
        when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")

        result =>
          status(result) shouldBe OK
          val page = CdsPage(contentAsString(result))
          verify(mockSessionCache).journeyCompleted(any[Request[_]])
          verify(mockSubscribe01Outcome, times(2)).processedDate
          verify(mockSubscribeOutcome, never()).processedDate
          page.title() should startWith("Application sent")
          page.getElementsText(RegistrationCompletePage.panelHeadingXpath) shouldBe "Application sent"
          page.getElementsText(RegistrationCompletePage.eoriXpath) shouldBe s"Your new EORI number is: $EORI"
          page.getElementsText(RegistrationCompletePage.issuedDateXpath) shouldBe "issued by HMRC on 22 May 2016"

          page.elementIsPresent(RegistrationCompletePage.LeaveFeedbackLinkXpath) shouldBe true
          page
            .getElementById("what-you-think")
            .text() shouldBe "Before you go Your feedback helps us make our service better. Take a short survey to share your feedback on this service."

          page.getElementsText(RegistrationCompletePage.LeaveFeedbackLinkXpath) should include("Take a short survey")
          page.getElementsHref(RegistrationCompletePage.LeaveFeedbackLinkXpath) should endWith(
            "/feedback/eori-common-component-register-atar"
          )
      }
    }

    "allow authenticated users to access the end standalone outcome page" in {
      invokeEndStandAloneWithAuthenticatedUser() {
        mockSessionCacheForOutcomePage
        when(mockSubscribeOutcome.eori).thenReturn(Some(EORI))
        when(mockSubscriptionDetails.name).thenReturn(Some("orgName"))
        when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")
        when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(
          Future.successful(mockSubscriptionDetails)
        )
        when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribe01Outcome))
        when(mockSessionCache.sub02Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribeOutcome))
        when(mockSessionCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]])).thenReturn(
          Future.successful(true)
        )
        when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")

        result =>
          status(result) shouldBe OK
          val page = CdsPage(contentAsString(result))
          verify(mockSessionCache).journeyCompleted(any[Request[_]])
          verify(mockSubscribe01Outcome, times(4)).processedDate
          verify(mockSubscribeOutcome, never()).processedDate
          page.title() should startWith("Application sent")
          page.getElementsText(RegistrationCompletePage.issuedDateXpath) shouldBe "issued by HMRC on 22 May 2016"

          page.elementIsPresent(RegistrationCompletePage.LeaveFeedbackLinkXpath) shouldBe true
          page.getElementsText(RegistrationCompletePage.LeaveFeedbackLinkXpath) should include("Take a short survey")
          page.getElementsHref(RegistrationCompletePage.LeaveFeedbackLinkXpath) should endWith(
            "feedback/eori-common-component-register-eori-only"
          )
      }
    }

  }

  "calling eoriAlreadyExists on Sub02Controller" should {
    "render eori already exists page" in {
      when(mockSubscriptionDetails.name).thenReturn(Some("orgName"))
      when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(mockSubscriptionDetails))
      when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribe01Outcome))
      when(mockSessionCache.sub02Outcome(any[Request[_]]))
        .thenReturn(Future.successful(Sub02Outcome("testDate", "testFullName", Some("EoriTest"))))
      when(mockSessionCache.journeyCompleted(any[Request[_]])).thenReturn(Future.successful(true))
      invokeEoriAlreadyExists { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  "calling subscriptionInProgress on Sub02Controller" should {
    "render subscription in-progress page" in {
      when(mockSessionCache.submissionCompleteDetails(any[Request[_]]))
        .thenReturn(Future.successful(SubmissionCompleteDetails("22 May 2016")))
      when(mockSessionCache.saveSubmissionCompleteDetails(any())(any[Request[_]])).thenReturn(Future.successful(true))
      when(mockSessionCache.journeyCompleted(any[Request[_]])).thenReturn(Future.successful(true))

      invokeSubscriptionInProgress { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  "calling eoriAlreadyAssociated on Sub02Controller" should {
    "render Already Associated page" in {
      when(mockSubscriptionDetails.name).thenReturn(Some("orgName"))
      when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(mockSubscriptionDetails))
      when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribe01Outcome))

      when(mockSessionCache.sub02Outcome(any[Request[_]]))
        .thenReturn(Future.successful(Sub02Outcome("testDate", "testFullName", Some("EoriTest"))))
      when(mockSessionCache.journeyCompleted(any[Request[_]])).thenReturn(Future.successful(true))
      invokeEoriAlreadyAssociated { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  "calling RequestNotProcessed on Sub02Controller" should {
    "render Request Not Processed page" in {
      when(mockSessionCache.journeyCompleted(any[Request[_]])).thenReturn(Future.successful(true))
      invokeRequestNotProcessed { result =>
        assertCleanedSession(result)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "calling pending on Sub02Controller" should {
    "render sub01 processing page" in {

      when(mockSubscriptionDetails.name).thenReturn(Some("testFullName"))
      when(mockSubscribe01Outcome.processedDate).thenReturn("22 May 2016")
      when(mockSessionCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.successful(mockSubscriptionDetails))
      when(mockSessionCache.sub01Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribe01Outcome))
      when(mockSessionCache.saveSub01Outcome(any[Sub01Outcome])(any[Request[_]])).thenReturn(Future.successful(true))
      when(mockSessionCache.saveSubscriptionDetails(any())(any[Request[_]])).thenReturn(Future.successful(true))

      when(mockSessionCache.journeyCompleted(any[Request[_]])).thenReturn(Future.successful(true))
      invokePending { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  private def mockSessionCacheForOutcomePage = {
    when(mockSessionCache.registrationDetails(any[Request[_]])).thenReturn(Future.successful(mockRegDetails))
    when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]])).thenReturn(Future.successful(true))
    when(mockSessionCache.saveSub01Outcome(any[Sub01Outcome])(any[Request[_]])).thenReturn(Future.successful(true))
    when(mockRegDetails.name).thenReturn("orgName")
    when(mockSessionCache.journeyCompleted(any[Request[_]])).thenReturn(Future.successful(true))
    when(mockSessionCache.sub02Outcome(any[Request[_]])).thenReturn(Future.successful(mockSubscribeOutcome))
    when(mockSubscribeOutcome.processedDate).thenReturn("22 May 2016")
  }

  def invokeEndStandAloneWithAuthenticatedUser(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    mockSessionCacheForOutcomePage
    test(
      subscriptionController.end(eoriOnlyService).apply(
        SessionBuilder.buildRequestWithSessionAndPath("/eori-only/subscribe", userId)
      )
    )
  }

  def invokeEndSubscriptionPageWithAuthenticatedUser(
    userId: String = defaultUserId
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    mockSessionCacheForOutcomePage
    test(
      subscriptionController.end(atarService).apply(
        SessionBuilder.buildRequestWithSessionAndPath("/atar/subscribe", userId)
      )
    )
  }

  def invokeEndPageWithUnAuthenticatedUser(test: Future[Result] => Any): Unit = {
    withNotLoggedInUser(mockAuthConnector)
    test(subscriptionController.end(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser))
  }

  private def subscribeForGetYourEORI(
    userId: String = defaultUserId,
    organisationTypeOption: Option[CdsOrganisationType] = None
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(organisationTypeOption)
    test(subscriptionController.subscribe(atarService)(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def invokeEoriAlreadyExists(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      subscriptionController.eoriAlreadyExists(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    )
  }

  private def invokeSubscriptionInProgress(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      subscriptionController.subscriptionInProgress(atarService).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )
    )
  }

  private def invokeEoriAlreadyAssociated(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      subscriptionController.eoriAlreadyAssociated(atarService).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )
    )
  }

  private def invokeRequestNotProcessed(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      subscriptionController.requestNotProcessed(atarService).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )
    )
  }

  private def invokePending(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(subscriptionController.pending(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

}
