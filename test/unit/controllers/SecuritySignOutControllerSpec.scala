/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.Mockito.{verify, _}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SecuritySignOutController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.display_sign_out
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SecuritySignOutControllerSpec extends ControllerSpec with AuthActionMock {
  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)
  private val mockSessionCache  = mock[SessionCache]

  private val displaySignOutView = instanceOf[display_sign_out]

  private val controller =
    new SecuritySignOutController(mockAuthAction, mockSessionCache, displaySignOutView, mcc)

  "Security Sign Out Controller" should {
    "return Ok 200 when displayPage method is requested" in {
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      displayPage(atarService) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should startWith("For your security, we signed you out")
      }
    }

    "return Ok 303 when signOut method is requested" in {
      when(mockSessionCache.remove(any[HeaderCarrier])).thenReturn(Future.successful(true))
      signOut(atarService) { result =>
        status(result) shouldBe SEE_OTHER
      }
      verify(mockSessionCache).remove(any[HeaderCarrier])
    }
  }

  private def displayPage(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(test(controller.displayPage(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId))))
  }

  private def signOut(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(test(controller.signOut(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId))))
  }

}
