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
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.start
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationControllerWithAllowlistVerificationSpec extends ControllerSpec with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)
  private val mockSessionCache  = mock[SessionCache]
  private val startRegisterView = instanceOf[start]

  val controller =
    new ApplicationController(mockAuthAction, mcc, startRegisterView, mockSessionCache, appConfig)

  "Navigating to logout" should {
    "logout a non-allowlisted user" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector, userEmail = Some("not@example.com"))
      when(mockSessionCache.remove(any[Request[_]])).thenReturn(Future.successful(true))

      controller.logout(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)) map { _ =>
        verify(mockSessionCache).remove(any[Request[_]])
      }
    }
  }
}
