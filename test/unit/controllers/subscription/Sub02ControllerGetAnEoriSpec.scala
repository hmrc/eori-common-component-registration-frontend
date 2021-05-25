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

import common.pages.{RegistrationCompletePage, RegistrationRejectedPage}
import common.support.testdata.TestData
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result, Session}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.PdfGeneratorConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{routes, Sub02Controller}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.SubscriptionCreateResponse._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.migration_success
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.xi_eori_guidance
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription._
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class Sub02ControllerGetAnEoriSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSessionCache               = mock[SessionCache]
  private val mockCdsSubscriber              = mock[CdsSubscriber]
  private val mockCdsOrganisationType        = mock[CdsOrganisationType]
  private val mockPdfGeneratorService        = mock[PdfGeneratorConnector]
  private val mockRegDetails                 = mock[RegistrationDetails]
  private val mockSubscribeOutcome           = mock[Sub02Outcome]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  private val migrationSuccessView            = instanceOf[migration_success]
  private val sub01OutcomeView                = instanceOf[sub01_outcome_processing]
  private val sub02RequestNotProcessed        = instanceOf[sub02_request_not_processed]
  private val sub02SubscriptionInProgressView = instanceOf[sub02_subscription_in_progress]
  private val sub02EoriAlreadyAssociatedView  = instanceOf[sub02_eori_already_associated]
  private val sub02EoriAlreadyExists          = instanceOf[sub02_eori_already_exists]
  private val sub01OutcomeRejected            = instanceOf[sub01_outcome_rejected]
  private val subscriptionOutcomeView         = instanceOf[subscription_outcome]
  private val xiEoriGuidanceView              = instanceOf[xi_eori_guidance]
  private val EORI                            = "ZZZ1ZZZZ23ZZZZZZZ"

  private val subscriptionController = new Sub02Controller(
    mockAuthAction,
    mockRequestSessionData,
    mockSessionCache,
    mockSubscriptionDetailsService,
    mcc,
    migrationSuccessView,
    sub01OutcomeView,
    sub02RequestNotProcessed,
    sub02SubscriptionInProgressView,
    sub02EoriAlreadyAssociatedView,
    sub02EoriAlreadyExists,
    sub01OutcomeRejected,
    subscriptionOutcomeView,
    xiEoriGuidanceView,
    mockCdsSubscriber
  )(global)

  val eoriNumberResponse: String           = "EORI-Number"
  val formBundleIdResponse: String         = "Form-Bundle-Id"
  private val processingDate               = "12 May 2018"
  val emailVerificationTimestamp: DateTime = TestData.emailVerificationTimestamp
  val emulatedFailure                      = new UnsupportedOperationException("Emulated service call failure.")

  override def beforeEach: Unit = {
    reset(mockAuthConnector, mockCdsSubscriber, mockPdfGeneratorService, mockSessionCache)
    when(mockSubscriptionDetailsService.saveKeyIdentifiers(any[GroupId], any[InternalId], any[Service])(any()))
      .thenReturn(Future.successful(()))
  }

  private def assertCleanedSession(result: Future[Result]): Unit = {
    val currentSession: Session = session(result)

    currentSession.data.get("selected-user-location") shouldBe None
    currentSession.data.get("subscription-flow") shouldBe None
    currentSession.data.get("selected-organisation-type") shouldBe None
    currentSession.data.get("uri-before-subscription-flow") shouldBe None
  }

  "Subscribe" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      subscriptionController.subscribe(atarService, Journey.Register)
    )
  }

  "End" should {
    assertNotLoggedInUserShouldBeRedirectedToLoginPage(
      mockAuthConnector,
      "Access the end page",
      subscriptionController.end(atarService)
    )
  }

  "Rejected" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, subscriptionController.rejected(atarService))
  }

  "clicking on the register button" should {

    "subscribe using selected organisation type when available" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
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

        verify(mockCdsSubscriber).subscribeWithCachedDetails(
          meq(Some(mockCdsOrganisationType)),
          meq(atarService),
          meq(Journey.Register)
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      }
    }

    "subscribe without a selected organisation type when selection is not available (automatic matching case)" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
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

        verify(mockCdsSubscriber).subscribeWithCachedDetails(meq(None), meq(atarService), meq(Journey.Register))(
          any[HeaderCarrier],
          any[Request[AnyContent]],
          any[Messages]
        )
      }
    }

    "redirect to 'Application complete' page with EORI number when subscription successful" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
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
        result.header.headers(LOCATION) shouldBe routes.Sub02Controller.end(atarService).url
      }
    }

    "redirect to 'Registration in review' page when subscription returns pending status" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(
        Future.successful(SubscriptionPending(formBundleIdResponse, processingDate, Some(emailVerificationTimestamp)))
      )

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe routes.Sub02Controller.pending(atarService).url
      }
    }

    "redirect to 'Registration rejected' page when subscription returns failed status" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(Future.successful(SubscriptionFailed("Subscription application has been rejected", processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe routes.Sub02Controller.rejected(atarService).url
      }
    }

    "redirect to 'eori already exists' page when subscription returns failed status with EORI already exists status text" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(Future.successful(SubscriptionFailed(EoriAlreadyExists, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe routes.Sub02Controller.eoriAlreadyExists(atarService).url
      }
    }

    "redirect to 'eori already associated' page when subscription returns failed status when the provided EORI already associated to Business partner record" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(Future.successful(SubscriptionFailed(EoriAlreadyAssociated, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe routes.Sub02Controller.eoriAlreadyAssociated(atarService).url
      }
    }

    "redirect to 'subscription in-progress' page when subscription returns failed status with Subscription is already in-progress status text" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(Future.successful(SubscriptionFailed(SubscriptionInProgress, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe routes.Sub02Controller.subscriptionInProgress(atarService).url
      }
    }

    "redirect to 'request not processed' page when subscription returns failed status when the request could not be processed" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(Future.successful(SubscriptionFailed(RequestNotProcessed, processingDate)))

      subscribeForGetYourEORI() { result =>
        assertCleanedSession(result)

        status(result) shouldBe SEE_OTHER
        result.header.headers(
          LOCATION
        ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.Sub02Controller
          .requestNotProcessed(atarService)
          .url
      }
    }

    "fail when subscription fails unexpectedly" in {
      when(
        mockCdsSubscriber.subscribeWithCachedDetails(
          any[Option[CdsOrganisationType]],
          any[Service],
          any[Journey.Value]
        )(any[HeaderCarrier], any[Request[AnyContent]], any[Messages])
      ).thenReturn(Future.failed(emulatedFailure))

      val caught = intercept[Exception] {
        subscribeForGetYourEORI() { result =>
          await(result)
        }
      }
      caught.getMessage shouldBe "Subscription Error. "
      caught.getCause shouldBe emulatedFailure
    }

    "allow authenticated users to access the end page" in {
      invokeEndPageWithAuthenticatedUser() {
        mockSessionCacheForOutcomePage
        when(mockSubscribeOutcome.eori).thenReturn(Some(EORI))
        when(mockSubscribeOutcome.fullName).thenReturn("orgName")

        result =>
          status(result) shouldBe OK
          val page = CdsPage(contentAsString(result))
          verify(mockSessionCache).remove(any[HeaderCarrier])
          page.title should startWith("Application complete")
          page.getElementsText(
            RegistrationCompletePage.pageHeadingXpath
          ) shouldBe s"The EORI number for orgName is $EORI"
          page.getElementsText(RegistrationCompletePage.eoriXpath) shouldBe EORI
          page.getElementsText(RegistrationCompletePage.issuedDateXpath) shouldBe "issued by HMRC on 22 May 2016"

          page.getElementsText(RegistrationCompletePage.additionalInformationXpath) should include(
            messages("cds.subscription.outcomes.success.optional-paragraph")
          )
          page.getElementsText(RegistrationCompletePage.whatHappensNextXpath) shouldBe
            strim("""
                |Your EORI number will be ready to use within 48 hours. It will appear on the EORI validation checker within 5 days. It has no expiry date.
                |You should only share it with trusted business partners or approved customs representatives. For example, you should give it to your courier or freight forwarder. They will use it to make customs declarations on your behalf.
                | """)
          page.elementIsPresent(RegistrationCompletePage.LeaveFeedbackLinkXpath) shouldBe true
          page.getElementsText(RegistrationCompletePage.LeaveFeedbackLinkXpath) should include(
            "What did you think of this service?"
          )
          page.getElementsHref(RegistrationCompletePage.LeaveFeedbackLinkXpath) should endWith(
            "/feedback/eori-common-component-subscribe-atar"
          )
      }
    }

    "allow authenticated users to access the rejected page" in {
      invokeRejectedPageWithAuthenticatedUser() {
        mockSessionCacheForOutcomePage

        result =>
          status(result) shouldBe OK
          val page = CdsPage(contentAsString(result))
          page.title should startWith(RegistrationRejectedPage.title)
          page.getElementsText(RegistrationRejectedPage.pageHeadingXpath) shouldBe RegistrationRejectedPage.heading
          page.getElementsText(
            RegistrationRejectedPage.processedDateXpath
          ) shouldBe "Application received by HMRC on 22 May 2016"
      }
    }
  }

  "calling eoriAlreadyExists on Sub02Controller" should {
    "render eori already exists page" in {
      when(mockSessionCache.sub02Outcome(any[HeaderCarrier]))
        .thenReturn(Future.successful(Sub02Outcome("testDate", "testFullName", Some("EoriTest"))))
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      invokeEoriAlreadyExists { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  "calling subscriptionInProgress on Sub02Controller" should {
    "render subscription in-progress page" in {
      when(mockSessionCache.sub02Outcome(any[HeaderCarrier]))
        .thenReturn(Future.successful(Sub02Outcome("testDate", "testFullName", Some("EoriTest"))))
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      invokeSubscriptionInProgress { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  "calling pending on Sub02Controller" should {
    "render sub01 processing page" in {
      when(mockSessionCache.sub02Outcome(any[HeaderCarrier]))
        .thenReturn(Future.successful(Sub02Outcome("testDate", "testFullName", Some("EoriTest"))))
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      invokePending { result =>
        assertCleanedSession(result)

        status(result) shouldBe OK
      }
    }
  }

  "calling migrationEnd on Sub02Controller" should {
    val sub02Outcome = Sub02Outcome("testDate", "testFullName", Some("EoriTest"))

    "render page with name for UK location" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some("uk"))
      when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(Utr("someUtr"))))
      mockNameAndSub02OutcomeRetrieval
      when(mockSessionCache.sub02Outcome(any[HeaderCarrier])).thenReturn(Future.successful(sub02Outcome))
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier])).thenReturn(Future.successful(true))
      invokeMigrationEnd { result =>
        assertCleanedSession(result)
        status(result) shouldBe OK
      }
    }

    "render page with name for ROW location when customsId exists" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some("eu"))
      when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(Utr("someUtr"))))
      mockNameAndSub02OutcomeRetrieval
      when(mockSessionCache.sub02Outcome(any[HeaderCarrier])).thenReturn(Future.successful(sub02Outcome))
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier])).thenReturn(Future.successful(true))
      invokeMigrationEnd { result =>
        assertCleanedSession(result)
        status(result) shouldBe OK
      }
    }

    "render page with name for ROW location when no customsId exists" in {
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(Some("eu"))
      when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier])).thenReturn(Future.successful(None))
      when(mockSessionCache.sub02Outcome(any[HeaderCarrier])).thenReturn(Future.successful(sub02Outcome))
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier])).thenReturn(Future.successful(true))
      invokeMigrationEnd { result =>
        assertCleanedSession(result)
        status(result) shouldBe OK
      }
    }
  }

  private def mockSessionCacheForOutcomePage = {
    when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(Future.successful(mockRegDetails))
    when(mockSessionCache.saveSub02Outcome(any[Sub02Outcome])(any[HeaderCarrier])).thenReturn(Future.successful(true))
    when(mockRegDetails.name).thenReturn("orgName")
    when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
    when(mockSessionCache.sub02Outcome(any[HeaderCarrier])).thenReturn(Future.successful(mockSubscribeOutcome))
    when(mockSubscribeOutcome.processedDate).thenReturn("22 May 2016")
  }

  def invokeEndPageWithAuthenticatedUser(userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    mockSessionCacheForOutcomePage
    test(
      subscriptionController.end(atarService).apply(
        SessionBuilder.buildRequestWithSessionAndPath("/atar/subscribe", userId)
      )
    )
  }

  def invokeRejectedPageWithAuthenticatedUser(userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    mockSessionCacheForOutcomePage
    test(
      subscriptionController.rejected(atarService).apply(
        SessionBuilder.buildRequestWithSessionAndPath("/atar/subscribe", userId)
      )
    )
  }

  def invokeEndPageWithUnAuthenticatedUser(test: Future[Result] => Any) {
    withNotLoggedInUser(mockAuthConnector)
    test(subscriptionController.end(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser))
  }

  private def subscribeForGetYourEORI(
    userId: String = defaultUserId,
    organisationTypeOption: Option[CdsOrganisationType] = None
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(organisationTypeOption)
    test(
      subscriptionController.subscribe(atarService, Journey.Register)(SessionBuilder.buildRequestWithSession(userId))
    )
  }

  private def invokeMigrationEnd(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(subscriptionController.migrationEnd(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
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

  private def invokePending(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(subscriptionController.pending(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def mockNameAndSub02OutcomeRetrieval = {
    val mrweair  = mock[RegisterWithEoriAndIdResponse]
    val mrweaird = mock[RegisterWithEoriAndIdResponseDetail]
    val rd       = mock[ResponseData]
    val trader   = mock[Trader]
    when(mockSessionCache.registerWithEoriAndIdResponse(any[HeaderCarrier])).thenReturn(Future.successful(mrweair))
    when(mrweair.responseDetail).thenReturn(Some(mrweaird))
    when(mrweaird.responseData).thenReturn(Some(rd))
    when(rd.trader).thenReturn(trader)
    when(trader.fullName).thenReturn("testName")
  }

}
