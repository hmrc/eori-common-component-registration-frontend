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

package unit.services.email

import org.mockito.Mockito._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.CheckYourEmailService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import base.Injector
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailVerificationService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.{check_your_email, email_confirmed, verify_your_email}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.confirmEmailYesNoAnswerForm
import util.ViewSpec

class CheckYourEmailServiceSpec extends ViewSpec with MockitoSugar with Injector {

  implicit val hc: HeaderCarrier       = HeaderCarrier()
  implicit val rq: Request[AnyContent] = withFakeCSRF(FakeRequest())

  private val mockSave4LaterService        = mock[Save4LaterService]
  private val mockEmailVerificationService = mock[EmailVerificationService]
  private val mockSessionCache             = mock[SessionCache]
  private val messagesControllerComponents = instanceOf[MessagesControllerComponents]

  private val checkYourEmailView = instanceOf[check_your_email]
  private val emailConfirmedView = instanceOf[email_confirmed]
  private val verifyYourEmail    = instanceOf[verify_your_email]

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val loggedInUser =
    LoggedInUserWithEnrolments(None, None, Enrolments(Set.empty), Some("test@example.com"), Some("groupId"), None)

  private def emailStatus(isConfirmed: Boolean = true) =
    EmailStatus(Some("test@example.com"), isConfirmed = Some(isConfirmed))

  val service = new CheckYourEmailService(
    mockSave4LaterService,
    mockEmailVerificationService,
    mockSessionCache,
    messagesControllerComponents,
    checkYourEmailView,
    verifyYourEmail,
    emailConfirmedView
  )

  "fetchEmailAndPopulateView" should {
    "fetch email successfully and populate checkYourEmailView" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emailStatus())))

      val result = await(service.fetchEmailAndPopulateView(loggedInUser, subscription))
      result.header.status mustBe OK
    }

    "fetch email successfully and populate verifyYourEmail" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emailStatus())))

      val result = await(service.fetchEmailAndPopulateView(loggedInUser, subscription, emailVerificationView = true))
      result.header.status mustBe OK
    }

    "populate checkYourEmailView without cached email" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = await(service.fetchEmailAndPopulateView(loggedInUser, subscription))
      result.header.status mustBe OK
    }

    "populate verifyYourEmail without cached email" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = await(service.fetchEmailAndPopulateView(loggedInUser, subscription, emailVerificationView = true))
      result.header.status mustBe OK
    }
  }

  "emailConfirmed" should {

    "fetch email successfully and redirect to MatchingIdController when confirmed" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(emailStatus())))

        val result = await(service.emailConfirmed(loggedInUser, subscription))
        result.header.status mustBe SEE_OTHER
        result.header.headers(
          "Location"
        ) mustBe (s"/customs-registration-services/${subscription.code}/register/matching/user-location")
    }

    "redirect to SecuritySignOutController when email not in cache" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = await(service.emailConfirmed(loggedInUser, subscription))
      result.header.status mustBe SEE_OTHER
      result.header.headers(
        "Location"
      ) mustBe (s"/customs-registration-services/${subscription.code}/register/sign-out")
    }

    "fetch and save email successfully then populate emailConfirmedView when not confirmed" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(emailStatus(false))))

        when(mockSave4LaterService.saveEmail(any[GroupId], any())(any[HeaderCarrier]))
          .thenReturn(Future.successful((): Unit))

        val result = await(service.emailConfirmed(loggedInUser, subscription))
        result.header.status mustBe OK
    }
  }

  "handleFormWithErrors" should {

    "fetch email successfully and populate checkYourEmailView and throw bad request" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(emailStatus())))

        val result = await(
          service.handleFormWithErrors(
            loggedInUser,
            confirmEmailYesNoAnswerForm(),
            isInReviewMode = false,
            subscription
          )
        )
        result.header.status mustBe BAD_REQUEST
    }

    "populate checkYourEmailView without email when not cached and throw bad request" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val result = await(
          service.handleFormWithErrors(
            loggedInUser,
            confirmEmailYesNoAnswerForm(),
            isInReviewMode = false,
            subscription
          )
        )
        result.header.status mustBe BAD_REQUEST
    }

  }

  "locationByAnswer" should {

    "redirect to CheckYourEmailController when answer is yes and email is cached and email not previously verified" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(emailStatus())))

        when(mockEmailVerificationService.createEmailVerificationRequest(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(true)))

        val result = await(service.locationByAnswer(GroupId("groupId"), YesNo.apply(true), subscription))
        result.header.status mustBe SEE_OTHER
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${subscription.code}/register/matching/verify-your-email"
    }

    "save email and redirect to EmailController when answer is yes and email already verified" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(emailStatus())))

        when(mockSave4LaterService.saveEmail(any[GroupId], any())(any[HeaderCarrier]))
          .thenReturn(Future.successful((): Unit))

        when(mockSessionCache.saveEmail(any())(any[Request[_]]))
          .thenReturn(Future.successful(true))

        when(mockEmailVerificationService.createEmailVerificationRequest(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(false)))

        val result = await(service.locationByAnswer(GroupId("groupId"), YesNo.apply(true), subscription))
        result.header.status mustBe SEE_OTHER
        result.header.headers(
          "Location"
        ) mustBe s"/customs-registration-services/${subscription.code}/register/check-user"
    }

    "throw IllegalStateException when email not in cache" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      intercept[IllegalStateException] {
        val result = await(service.locationByAnswer(GroupId("groupId"), YesNo.apply(true), subscription))
        result.header.status mustBe INTERNAL_SERVER_ERROR
      }
    }

    "throw IllegalStateException when email is cached and createEmailVerificationRequest fails" in servicesToTest.foreach {
      subscription =>
        when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        when(mockEmailVerificationService.createEmailVerificationRequest(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        intercept[IllegalStateException] {
          val result = await(service.locationByAnswer(GroupId("groupId"), YesNo.apply(true), subscription))
          result.header.status mustBe INTERNAL_SERVER_ERROR
        }
    }

    "redirect to WhatIsYourEmailController when answer is no" in servicesToTest.foreach { subscription =>
      val result = await(service.locationByAnswer(GroupId("groupId"), YesNo.apply(false), subscription))
      result.header.status mustBe SEE_OTHER
      result.header.headers(
        "Location"
      ) mustBe s"/customs-registration-services/${subscription.code}/register/matching/what-is-your-email"

    }
  }

}
