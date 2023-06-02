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

package unit.controllers.email

import common.pages.emailvericationprocess.CheckYourEmailPage
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.CheckYourEmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.GroupId
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailVerificationService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.{check_your_email, email_confirmed, verify_your_email}
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.YesNoFormBuilder.ValidRequest
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckYourEmailControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val yesNoInputName = "yes-no-answer"
  private val answerYes      = true.toString
  private val answerNo       = false.toString

  private val problemWithSelectionError =
    "Select yes if this is the correct email address"

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val mockSave4LaterService = mock[Save4LaterService]
  private val mockSessionCache      = mock[SessionCache]

  private val checkYourEmailView = instanceOf[check_your_email]
  private val emailConfirmedView = instanceOf[email_confirmed]
  private val verifyYourEmail    = instanceOf[verify_your_email]

  private val controller = new CheckYourEmailController(
    mockAuthAction,
    mockSave4LaterService,
    mockSessionCache,
    mcc,
    checkYourEmailView,
    emailConfirmedView,
    verifyYourEmail,
    mockEmailVerificationService
  )

  val email       = "test@example.com"
  val emailStatus = EmailStatus(Some(email))

  val internalId = "InternalID"
  val jsonValue  = Json.toJson(emailStatus)
  val data       = Map(internalId -> jsonValue)
  val unit       = ()

  override def beforeEach(): Unit = {
    when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(emailStatus)))

    when(mockEmailVerificationService.createEmailVerificationRequest(any[String], any[String])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(true)))
  }

  "Displaying the Check Your Email Page" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display title as 'Check your email address'" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Is this the email address you want to use?")
      }
    }

    "display title as 'Check your email address' when no email saved in session" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Is this the email address you want to use?")
      }
    }
  }

  "Submitting the Check Your Email Page" should {

    "redirect to Verify Your Email Address page for unverified email address" in {
      when(mockEmailVerificationService.createEmailVerificationRequest(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(true)))
      submitForm(ValidRequest + (yesNoInputName -> answerYes), service = atarService) {
        result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers("Location") should endWith(
            "/customs-registration-services/atar/register/matching/verify-your-email"
          )
      }
    }

    "redirect to Are You based in UK for Already verified email" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(emailStatus.copy(isVerified = true))))
      when(
        mockSave4LaterService
          .saveEmail(any[GroupId], any[EmailStatus])(any[HeaderCarrier])
      ).thenReturn(Future.successful(unit))
      when(mockSessionCache.saveEmail(any[String])(any[Request[_]]))
        .thenReturn(Future.successful(true))

      when(mockEmailVerificationService.createEmailVerificationRequest(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(false)))

      submitForm(ValidRequest + (yesNoInputName -> answerYes), service = atarService) {
        result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers("Location") should endWith("/customs-registration-services/atar/register/check-user")
      }
    }

    "throw  IllegalStateException when downstream CreateEmailVerificationRequest Fails" in {
      when(mockEmailVerificationService.createEmailVerificationRequest(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      the[IllegalStateException] thrownBy {
        submitForm(ValidRequest + (yesNoInputName -> answerYes), service = atarService) {
          result =>
            status(result) shouldBe SEE_OTHER
        }
      } should have message "CreateEmailVerificationRequest Failed"
    }

    "throw  IllegalStateException when save4LaterService.fetchEmail returns None" in {
      when(mockEmailVerificationService.createEmailVerificationRequest(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(true)))
      when(mockSave4LaterService.fetchEmail(any())(any())) thenReturn Future.successful(None)
      the[IllegalStateException] thrownBy {
        submitForm(ValidRequest + (yesNoInputName -> answerYes), service = atarService) {
          result =>
            status(result) shouldBe SEE_OTHER
        }
      } should have message "[CheckYourEmailController][submitNewDetails] - emailStatus cache none"
    }

    "throw  IllegalStateException when save4LaterService.fetchEmail returns EmailStatus with undefined email" in {
      when(mockEmailVerificationService.createEmailVerificationRequest(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(true)))
      val emailStatus = EmailStatus(None)
      when(mockSave4LaterService.fetchEmail(any())(any())) thenReturn Future.successful(Some(emailStatus))
      the[IllegalStateException] thrownBy {
        submitForm(ValidRequest + (yesNoInputName -> answerYes), service = atarService) {
          result =>
            status(result) shouldBe SEE_OTHER
        }
      } should have message "[CheckYourEmailController][submitNewDetails] - emailStatus.email none"
    }

    "redirect to What is Your Email Address Page on selecting No radio button" in {
      submitForm(ValidRequest + (yesNoInputName -> answerNo), service = atarService) {
        result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers("Location") should endWith(
            "/customs-registration-services/atar/register/matching/what-is-your-email"
          )
      }
    }

    "display an error message when no option is selected" in {
      submitForm(ValidRequest - yesNoInputName, service = atarService) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(CheckYourEmailPage.pageLevelErrorSummaryListXPath) shouldBe problemWithSelectionError
        page.getElementsText(
          CheckYourEmailPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: $problemWithSelectionError"
      }
    }

    "display an error message when no email in session" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      submitForm(ValidRequest - yesNoInputName, service = atarService) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(CheckYourEmailPage.pageLevelErrorSummaryListXPath) shouldBe problemWithSelectionError
        page.getElementsText(
          CheckYourEmailPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: $problemWithSelectionError"
      }
    }
  }

  "Redirecting to Verify Your Email Address Page" should {
    "display title as 'Confirm your email address'" in {
      verifyEmailViewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Confirm your email address")
      }
    }

    "display title as 'Confirm your email address' when no email in session" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      verifyEmailViewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Confirm your email address")
      }
    }
  }

  "Email Confirmed" should {
    "redirect to SecuritySignOutController when no email in session" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      emailConfirmed(defaultUserId) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(routes.SecuritySignOutController.signOut(atarService).url)
      }
    }

    "redirect to MatchingIdController when email is confirmed" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(EmailStatus(Some(email), isConfirmed = Some(true)))))
      emailConfirmed(defaultUserId) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(routes.UserLocationController.form(atarService).url)
      }
    }

    "display emailConfirmedView when email is not confirmed" in {
      when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(EmailStatus(Some(email)))))
      emailConfirmed(defaultUserId) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("You have confirmed your email address")
      }
    }
  }

  "Email Confirmed Continue" should {
    "redirect to MatchingIdController" in {
      emailConfirmedContinue() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(routes.UserLocationController.form(atarService).url)
      }
    }
  }

  private def submitForm(form: Map[String, String], userId: String = defaultUserId, service: Service)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller.submit(isInReviewMode = false, service)(
      SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
    )
    test(result)
  }

  private def showForm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .createForm(atarService)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def verifyEmailViewForm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .verifyEmailView(atarService)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def emailConfirmed(userId: String)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .emailConfirmed(atarService)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def emailConfirmedContinue()(test: Future[Result] => Any): Unit = {
    val result = controller
      .emailConfirmedContinue(atarService)
      .apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

}
