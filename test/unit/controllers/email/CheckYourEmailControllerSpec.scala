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

package unit.controllers.email

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Results.Status
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.CheckYourEmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.CheckYourEmailService
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.YesNoFormBuilder.validRequest
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.Future

class CheckYourEmailControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val yesNoInputName = "yes-no-answer"
  private val answerYes      = true.toString

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val mockCheckYourEmailService = mock[CheckYourEmailService]

  private val controller = new CheckYourEmailController(mockAuthAction, mockCheckYourEmailService, mcc)

  "createForm" should {
    "correctly display check_your_email view" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockCheckYourEmailService.fetchEmailAndPopulateView(any(), any(), any())(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Status(OK)))

      val result =
        await(controller.createForm(subscription).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
      result.header.status shouldBe OK
    }
  }

  "submit" should {
    "correctly handle valid confirmEmailYesNoAnswerForm" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockCheckYourEmailService.locationByAnswer(any(), any())(any(), any()))
        .thenReturn(Future.successful(Status(OK)))

      val result = await(
        controller
          .submit(isInReviewMode = false, subscription)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              defaultUserId,
              validRequest + (yesNoInputName -> answerYes)
            )
          )
      )
      result.header.status shouldBe OK
    }

    "correctly handle valid confirmEmailYesNoAnswerForm - inReviewMode" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockCheckYourEmailService.locationByAnswer(any(), any())(any(), any()))
        .thenReturn(Future.successful(Status(OK)))

      val result = await(
        controller
          .submit(isInReviewMode = true, subscription)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(
              defaultUserId,
              validRequest + (yesNoInputName -> answerYes)
            )
          )
      )
      result.header.status shouldBe OK
    }

    "correctly handle invalid confirmEmailYesNoAnswerForm - with errors" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockCheckYourEmailService.handleFormWithErrors(any(), any(), any(), any())(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Status(BAD_REQUEST)))

      val result = await(
        controller
          .submit(isInReviewMode = true, subscription)
          .apply(
            SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, validRequest + (yesNoInputName -> ""))
          )
      )
      result.header.status shouldBe BAD_REQUEST
    }
  }

  "verifyEmailView" should {
    "correctly display verify_your_email view" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockCheckYourEmailService.fetchEmailAndPopulateView(any(), any(), any())(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Status(OK)))

      val result =
        await(controller.verifyEmailView(subscription).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
      result.header.status shouldBe OK
    }
  }

  "emailConfirmed" should {
    "correctly display email_confirmed view" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockCheckYourEmailService.emailConfirmed(any(), any())(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Status(OK)))

      val result =
        await(controller.emailConfirmed(subscription).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
      result.header.status shouldBe OK
    }
  }

  "emailConfirmedContinue" should {
    "correctly redirect to MatchingIdController" in servicesToTest.foreach { subscription =>
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      val result = await(
        controller.emailConfirmedContinue(subscription).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
      )
      result.header.status shouldBe SEE_OTHER
    }
  }

}
