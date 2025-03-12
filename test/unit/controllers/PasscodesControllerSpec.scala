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

package unit.controllers

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EmailVerificationConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.testOnly.PasscodesController
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.{ExecutionContext, Future}

class PasscodesControllerSpec extends ControllerSpec with UnitSpec with MockitoSugar with AuthActionMock {

  implicit val ex: ExecutionContext = ExecutionContext.Implicits.global

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val messagesControllerComponents = inject[MessagesControllerComponents]
  private val emailVerificationConnector = mock[EmailVerificationConnector]

  private val passcodesController =
    new PasscodesController(mockAuthAction, messagesControllerComponents, emailVerificationConnector)

  "getEmailVerificationPasscodes" should {

    "return OK response with body when calling EmailVerificationConnector" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)

      when(emailVerificationConnector.getPasscodes(any[HeaderCarrier])).thenReturn(
        Future.successful(HttpResponse.apply(OK, "Some body"))
      )
      val result = await(
        passcodesController.getEmailVerificationPasscodes().apply(SessionBuilder.buildRequestWithSession(defaultUserId))
      )
      result.header.status shouldBe OK

    }

  }

}
