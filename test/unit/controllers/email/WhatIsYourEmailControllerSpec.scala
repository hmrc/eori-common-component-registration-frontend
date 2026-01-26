/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.WhatIsYourEmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.GroupId
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.what_is_your_email
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatIsYourEmailControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)

  private val mockSave4LaterService = mock[Save4LaterService]

  private val whatIsYourEmailView = inject[what_is_your_email]

  private val controller =
    new WhatIsYourEmailController(mockAuthAction, mcc, whatIsYourEmailView, mockSave4LaterService)

  val email = "test@example.com"
  val emailStatus = EmailStatus(Some(email))

  val EmailFieldsMap = Map("email" -> email)
  val unpopulatedEmailFieldsMap = Map("email" -> "")

  override def beforeEach(): Unit = {
    when(mockSave4LaterService.fetchEmail(any[GroupId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(emailStatus)))

    when(
      mockSave4LaterService
        .saveEmail(any[GroupId], any[EmailStatus])(any[HeaderCarrier])
    ).thenReturn(Future.successful(()))
  }

  "What Is Your Email form in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display title as 'What is your email address'" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("What is your email address?")
      }
    }
  }

  "What Is Your Email form" should {
    "be mandatory" in {
      submitFormInCreateMode(unpopulatedEmailFieldsMap) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "be restricted to 50 characters for email length" in {
      val maxEmail = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx@xxxxxxxxxx"
      submitFormInCreateMode(unpopulatedEmailFieldsMap ++ Map("email" -> maxEmail)) { result =>
        status(result) shouldBe BAD_REQUEST

      }
    }

    "be valid for correct email format" in {

      submitFormInCreateMode(EmailFieldsMap) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith(
          "/customs-registration-services/atar/register/matching/check-your-email"
        )

      }
    }
  }

  private def submitFormInCreateMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result =
      controller.submit(atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

  private def showCreateForm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .createForm(atarService)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
