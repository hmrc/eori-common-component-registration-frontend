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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Request
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.start
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)
  private val mockSessionCache  = mock[SessionCache]

  private val startRegisterView = instanceOf[start]

  val controller =
    new ApplicationController(mockAuthAction, mcc, startRegisterView, mockSessionCache, appConfig)

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSessionCache)

    super.afterEach()
  }

  "Navigating to start" should {

    "allow unauthenticated users to access the start page" in {
      val result = controller.startRegister(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser)
      status(result) shouldBe OK
    }
  }
  "Navigating to What is your email page" should {
    "if eori-only do not show Start page" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockSessionCache.remove(any[Request[_]])).thenReturn(Future.successful(true))
      val result =
        controller.startRegister(eoriOnlyService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should endWith("/customs-registration-services/eori-only/register/check-user")

    }
  }
  "Navigating to logout" should {
    "logout an authenticated user for register" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockSessionCache.remove(any[Request[_]])).thenReturn(Future.successful(true))

      val result =
        controller.logout(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      session(result).isEmpty shouldBe true
      await(result).header.headers("Location") should endWith("feedback/eori-common-component-register-atar")
    }
  }

  "Navigating to keepAlive" should {

    "return a status of OK" in {

      when(mockSessionCache.keepAlive(any())).thenReturn(Future.successful(true))

      val result =
        controller.keepAlive(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser)

      status(result) shouldBe OK

      verify(mockSessionCache).keepAlive(any())
    }
  }
}
