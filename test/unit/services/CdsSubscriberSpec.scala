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

package unit.services

import base.{Injector, UnitSpec}
import common.support.testdata.TestData
import common.support.testdata.subscription.SubscriptionContactDetailsBuilder

import java.time.LocalDateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{when, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Lang.defaultLang
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{RecipientDetails, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.http.HeaderCarrier

import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import play.api.Application
import play.api.inject.bind

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class CdsSubscriberSpec extends UnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach with Injector {

  implicit lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser]
    )
    .build()

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(15, Millis)))

  private val mockSubscriptionService                      = mock[SubscriptionService]
  private val mockCdsFrontendDataCache                     = mock[SessionCache]
  private val mockRegistrationConfirmService               = mock[RegistrationConfirmService]
  private val mockSubscriptionFlowManager                  = mock[SubscriptionFlowManager]
  private val mockHandleSubscriptionService                = mock[HandleSubscriptionService]
  private val mockRegistrationDetails: RegistrationDetails = mock[RegistrationDetails]

  implicit private val hc: HeaderCarrier = mock[HeaderCarrier]

  implicit val messages: Messages = MessagesImpl(defaultLang, instanceOf[MessagesApi])

  private val eori                       = "EORI-Number"
  private val formBundleId               = "Form-Bundle-Id"
  private val processingDate             = "19 April 2018"
  private val emailVerificationTimestamp = TestData.emailVerificationTimestamp
  private val mockCdsOrganisationType    = mock[Option[CdsOrganisationType]]
  private val mockContactDetailsModel    = mock[ContactDetailsModel]
  private val contactDetails             = SubscriptionContactDetailsBuilder.contactDetailsWithMandatoryValuesOnly

  private val subscriptionDetails = SubscriptionDetails(
    contactDetails = Some(mockContactDetailsModel),
    eoriNumber = Some(eori),
    email = Some("test@example.com"),
    nameIdOrganisationDetails = Some(NameIdOrganisationMatchModel("orgname", "orgid"))
  )

  private val emulatedFailure = new UnsupportedOperationException("Emulation of service call failure")

  private val cdsSubscriber =
    new CdsSubscriber(mockSubscriptionService, mockCdsFrontendDataCache, mockHandleSubscriptionService)(global)

  implicit val request: Request[Any] = mock[Request[Any]]

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockRegistrationDetails.sapNumber).thenReturn(TaxPayerId("some-SAP-number"))
    when(mockContactDetailsModel.contactDetails).thenReturn(contactDetails)
  }

  override protected def afterEach(): Unit = {
    reset(mockCdsFrontendDataCache)
    reset(mockSubscriptionService)
    reset(mockCdsFrontendDataCache)
    reset(mockRegistrationConfirmService)
    reset(mockSubscriptionFlowManager)
    reset(mockHandleSubscriptionService)
    reset(mockRegistrationDetails)

    super.afterEach()
  }

  "CdsSubscriber" should {

    "call SubscriptionService when there is a cache hit" in {
      mockSuccessfulSubscribeGYEJourney(mockRegistrationDetails, subscriptionDetails)
      val inOrder =
        org.mockito.Mockito.inOrder(mockCdsFrontendDataCache, mockSubscriptionService, mockRegistrationConfirmService)

      whenReady(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService)) {
        subscriptionResult =>
          subscriptionResult shouldBe SubscriptionSuccessful(
            Eori(eori),
            formBundleId,
            processingDate,
            Some(emailVerificationTimestamp)
          )
          inOrder.verify(mockCdsFrontendDataCache).registrationDetails(any[Request[_]])
          inOrder
            .verify(mockSubscriptionService)
            .subscribe(
              meq(mockRegistrationDetails),
              meq(subscriptionDetails),
              meq(mockCdsOrganisationType),
              any[Service]
            )(any[HeaderCarrier])
      }
    }

    "call SubscriptionService when there is only registration details in cache to get a pending subscription" in {
      mockPendingSubscribe(mockRegistrationDetails)
      when(mockCdsFrontendDataCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      val inOrder = org.mockito.Mockito.inOrder(mockCdsFrontendDataCache, mockSubscriptionService)

      whenReady(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService)) {
        result =>
          result shouldBe SubscriptionPending(formBundleId, processingDate, Some(emailVerificationTimestamp))
          inOrder.verify(mockCdsFrontendDataCache).registrationDetails(any[Request[_]])
          inOrder
            .verify(mockSubscriptionService)
            .subscribe(
              meq(mockRegistrationDetails),
              meq(subscriptionDetails),
              any[Option[CdsOrganisationType]],
              any[Service]
            )(any[HeaderCarrier])
          verify(mockCdsFrontendDataCache, never).remove(any[Request[_]])
      }
    }

    "propagate a failure when registrationDetails cache fails to be accessed" in {
      when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]])).thenReturn(Future.failed(emulatedFailure))

      val caught = the[UnsupportedOperationException] thrownBy {
        await(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService))
      }
      caught shouldBe emulatedFailure
      verifyNoInteractions(mockSubscriptionService)
      verify(mockCdsFrontendDataCache, never).remove(any[Request[_]])
    }

    "propagate a failure when subscriptionDetailsHolder cache fails to be accessed" in {
      when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]])).thenReturn(mockRegistrationDetails)
      when(mockCdsFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(Future.failed(emulatedFailure))

      val caught = the[UnsupportedOperationException] thrownBy {
        await(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService))
      }
      caught shouldBe emulatedFailure
      verifyNoInteractions(mockSubscriptionService)
      verify(mockCdsFrontendDataCache, never).remove(any[Request[_]])
    }

    "call handle-subscription service when subscription successful" in {
      val expectedOrgName = "My Successful Org"
      val expectedRecipient = RecipientDetails(
        atarService,
        "john.doe@example.com",
        "John Doe",
        Some("My Successful Org"),
        Some("19 April 2018")
      )

      mockSuccessfulSubscribeGYEJourney(mockRegistrationDetails, subscriptionDetails, expectedOrgName)
      whenReady(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService)) {
        result =>
          result shouldBe SubscriptionSuccessful(
            Eori(eori),
            formBundleId,
            processingDate,
            Some(emailVerificationTimestamp)
          )
          val inOrder = org.mockito.Mockito.inOrder(mockCdsFrontendDataCache, mockHandleSubscriptionService)
          inOrder
            .verify(mockCdsFrontendDataCache)
            .saveSub02Outcome(meq(Sub02Outcome(processingDate, expectedOrgName, Some(eori))))(meq(request))
          inOrder
            .verify(mockHandleSubscriptionService)
            .handleSubscription(
              meq(formBundleId),
              meq(expectedRecipient),
              any[TaxPayerId],
              meq(Some(Eori(eori))),
              meq(Some(emailVerificationTimestamp)),
              any[SafeId]
            )(any[HeaderCarrier])
      }
    }

    "call handle-subscription service when subscription returns pending status" in {
      val expectedOrgName = "My Pending Org"
      val expectedRecipient =
        RecipientDetails(atarService, "john.doe@example.com", "John Doe", Some("My Pending Org"), Some("19 April 2018"))
      mockPendingSubscribe(mockRegistrationDetails, expectedOrgName)
      when(mockCdsFrontendDataCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
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
      ).thenReturn(Future.successful(()))

      whenReady(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService)) {
        result =>
          result shouldBe SubscriptionPending(formBundleId, processingDate, Some(emailVerificationTimestamp))
          val inOrder = org.mockito.Mockito.inOrder(mockCdsFrontendDataCache, mockHandleSubscriptionService)
          inOrder
            .verify(mockCdsFrontendDataCache)
            .saveSub02Outcome(meq(Sub02Outcome(processingDate, expectedOrgName)))(meq(request))
          inOrder
            .verify(mockHandleSubscriptionService)
            .handleSubscription(
              meq(formBundleId),
              meq(expectedRecipient),
              any[TaxPayerId],
              meq(None),
              meq(Some(emailVerificationTimestamp)),
              any[SafeId]
            )(any[HeaderCarrier])
      }
    }

    "not call handle-subscription service when subscription returns a failure status" in {
      val expectedName = "Org Already Has EORI in PDS"
      mockFailedSubscribe(mockRegistrationDetails, subscriptionDetails, expectedName)
      when(mockCdsFrontendDataCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
        .thenReturn(Future.successful(true))
      whenReady(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService)) {
        result =>
          result shouldBe SubscriptionFailed("EORI already exists", processingDate)
          verify(mockCdsFrontendDataCache).saveSub02Outcome(meq(Sub02Outcome(processingDate, expectedName)))(
            meq(request)
          )
          verifyNoInteractions(mockHandleSubscriptionService)
      }
    }

    "fail when subscription succeeds but handle-subscription call fails" in {
      mockSuccessfulSubscribeGYEJourney(mockRegistrationDetails, subscriptionDetails)
      when(
        mockHandleSubscriptionService.handleSubscription(
          anyString,
          any[RecipientDetails],
          any[TaxPayerId],
          any[Option[Eori]],
          any[Option[LocalDateTime]],
          any[SafeId]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(emulatedFailure))

      val caught = the[UnsupportedOperationException] thrownBy {
        await(cdsSubscriber.subscribeWithCachedDetails(mockCdsOrganisationType, atarService))
      }
      caught shouldBe emulatedFailure
    }
  }

  private def mockSuccessfulSubscribeGYEJourney(
    mockRegistrationDetails: RegistrationDetails,
    cachedSubscriptionDetailsHolder: SubscriptionDetails,
    registeredName: String = "orgName"
  ) = {
    when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]]))
      .thenReturn(Future.successful(mockRegistrationDetails))
    when(mockCdsFrontendDataCache.subscriptionDetails(any[Request[_]]))
      .thenReturn(Future.successful(cachedSubscriptionDetailsHolder))
    when(
      mockSubscriptionService.subscribe(
        any[RegistrationDetails],
        any[SubscriptionDetails],
        any[Option[CdsOrganisationType]],
        any[Service]
      )(any[HeaderCarrier])
    ).thenReturn(
      Future
        .successful(SubscriptionSuccessful(Eori(eori), formBundleId, processingDate, Some(emailVerificationTimestamp)))
    )
    when(
      mockHandleSubscriptionService.handleSubscription(
        anyString,
        any[RecipientDetails],
        any[TaxPayerId],
        any[Option[Eori]],
        any[Option[LocalDateTime]],
        any[SafeId]
      )(any[HeaderCarrier])
    ).thenReturn(Future.successful(()))
    when(mockCdsFrontendDataCache.saveSub02Outcome(any[Sub02Outcome])(any[Request[_]]))
      .thenReturn(Future.successful(true))
    when(mockRegistrationDetails.name).thenReturn(registeredName)
    when(mockRegistrationDetails.safeId).thenReturn(SafeId("safeId"))
  }

  private def mockPendingSubscribe(
    cachedRegistrationDetails: RegistrationDetails,
    registeredName: String = "orgName"
  ) = {

    when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]])).thenReturn(cachedRegistrationDetails)
    when(mockCdsFrontendDataCache.subscriptionDetails).thenReturn(subscriptionDetails)
    when(
      mockSubscriptionService
        .subscribe(any[RegistrationDetails], meq(subscriptionDetails), any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier]
        )
    ).thenReturn(Future.successful(SubscriptionPending(formBundleId, processingDate, Some(emailVerificationTimestamp))))
    when(
      mockHandleSubscriptionService.handleSubscription(
        anyString,
        any[RecipientDetails],
        any[TaxPayerId],
        any[Option[Eori]],
        any[Option[LocalDateTime]],
        any[SafeId]
      )(any[HeaderCarrier])
    ).thenReturn(Future.successful(()))
    when(mockRegistrationDetails.name).thenReturn(registeredName)
    when(mockRegistrationDetails.safeId).thenReturn(SafeId("safeId"))
  }

  private def mockFailedSubscribe(
    registrationDetails: RegistrationDetails,
    subscriptionDetails: SubscriptionDetails,
    registeredName: String
  ) = {
    when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]])).thenReturn(registrationDetails)
    when(mockCdsFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(subscriptionDetails)
    when(
      mockSubscriptionService
        .subscribe(any[RegistrationDetails], any[SubscriptionDetails], any[Option[CdsOrganisationType]], any[Service])(
          any[HeaderCarrier]
        )
    ).thenReturn(Future.successful(SubscriptionFailed("EORI already exists", processingDate)))
    when(mockRegistrationDetails.name).thenReturn(registeredName)
    when(mockRegistrationDetails.safeId).thenReturn(SafeId("safeId"))
  }

}
