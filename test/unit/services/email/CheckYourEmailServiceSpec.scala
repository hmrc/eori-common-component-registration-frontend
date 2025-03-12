/*
 * Copyright 2025 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import play.api.test.Helpers.{await, contentAsString}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.confirmEmailYesNoAnswerForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailJourneyService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{CheckYourEmailService, Save4LaterService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.{check_your_email, email_confirmed, verify_your_email}
import uk.gov.hmrc.http.HeaderCarrier
import util.ViewSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckYourEmailServiceSpec extends ViewSpec with MockitoSugar with Injecting {

  implicit val hc: HeaderCarrier       = HeaderCarrier()
  implicit val rq: Request[AnyContent] = withFakeCSRF(FakeRequest())

  private val mockSave4LaterService        = mock[Save4LaterService]
  private val mockEmailJourneyService      = mock[EmailJourneyService]
  private val messagesControllerComponents = inject[MessagesControllerComponents]

  private val checkYourEmailView = inject[check_your_email]
  private val emailConfirmedView = inject[email_confirmed]
  private val verifyYourEmail    = inject[verify_your_email]

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  implicit val loggedInUser: LoggedInUserWithEnrolments =
    LoggedInUserWithEnrolments(
      None,
      None,
      Enrolments(Set.empty),
      Some("test@example.com"),
      Some("groupId"),
      None,
      "credId"
    )

  private def emailStatus(isConfirmed: Boolean = true) =
    EmailStatus(Some("test@example.com"), isConfirmed = Some(isConfirmed))

  val service = new CheckYourEmailService(
    mockSave4LaterService,
    messagesControllerComponents,
    checkYourEmailView,
    verifyYourEmail,
    emailConfirmedView,
    mockEmailJourneyService
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

    "fetch email successfully and display confirmed page" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emailStatus())))

      val result = await(service.emailConfirmed(loggedInUser, subscription))
      result.header.status mustBe OK
      doc().body().getElementsByTag("h1").text() mustBe messages("cds.email-confirmed.title-and-heading")

    }

    "redirect to SecuritySignOutController when email not in cache" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = await(service.emailConfirmed(loggedInUser, subscription))
      result.header.status mustBe SEE_OTHER
      result.header.headers("Location") mustBe s"/customs-registration-services/${subscription.code}/register/sign-out"
    }

    "fetch and save email successfully then populate emailConfirmedView when not confirmed" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emailStatus(false))))

      when(mockSave4LaterService.saveEmail(any[GroupId], any())(any[HeaderCarrier]))
        .thenReturn(Future.successful((): Unit))

      val result = await(service.emailConfirmed(loggedInUser, subscription))
      result.header.status mustBe OK
    }
  }

  "handleFormWithErrors" should {

    "fetch email successfully and populate checkYourEmailView and throw bad request" in servicesToTest.foreach { subscription =>
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

    "populate checkYourEmailView without email when not cached and throw bad request" in servicesToTest.foreach { subscription =>
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

    "redirect to CheckYourEmailController when answer is yes and email is cached and email not previously verified" in servicesToTest.foreach { subscription =>
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emailStatus())))

      when(mockEmailJourneyService.continue(any())(any(), any(), any(), any()))
        .thenReturn(
          Future.successful(
            Redirect(s"/customs-registration-services/${subscription.code}/register/matching/verify-your-email")
          )
        )

      val result = await(service.locationByAnswer(YesNo.apply(isYes = true), subscription))
      result.header.status mustBe SEE_OTHER
      result.header.headers(
        "Location"
      ) mustBe s"/customs-registration-services/${subscription.code}/register/matching/verify-your-email"
    }

    "redirect to WhatIsYourEmailController when answer is no" in servicesToTest.foreach { subscription =>
      val result = await(service.locationByAnswer(YesNo.apply(isYes = false), subscription))
      result.header.status mustBe SEE_OTHER
      result.header.headers(
        "Location"
      ) mustBe s"/customs-registration-services/${subscription.code}/register/matching/what-is-your-email"

    }
  }

  def doc(service: Service = atarService): Document =
    Jsoup.parse(contentAsString(emailConfirmedView(service)))

}
