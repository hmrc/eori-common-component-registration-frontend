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

import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.YouCannotUseServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{unauthorized, you_cant_use_service}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class YouCannotUseServiceControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockEnv           = mock[Environment]
  private val mockAppConfig     = mock[Configuration]
  private val youCantUseService = instanceOf[you_cant_use_service]
  private val unauthorisedView  = instanceOf[unauthorized]

  private val controller =
    new YouCannotUseServiceController(
      mockAppConfig,
      mockEnv,
      mockAuthConnector,
      youCantUseService,
      unauthorisedView,
      mcc
    )(global)

  "Accessing the page" should {

    "show youCantUseService view" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      page() { result =>
        status(result) shouldBe UNAUTHORIZED
        CdsPage(contentAsString(result)).title() should startWith("You cannot use this service")
      }
    }

    "show unauthorised view" in {
      unauthorisedPage() { result =>
        status(result) shouldBe UNAUTHORIZED
        CdsPage(contentAsString(result)).title() should startWith("Service unavailable")
      }
    }
  }

  def page()(test: Future[Result] => Any): Unit =
    test(controller.page(atarService).apply(request = SessionBuilder.buildRequestWithSessionNoUserAndToken()))

  def unauthorisedPage()(test: Future[Result] => Any): Unit =
    test(controller.unauthorisedPage(atarService).apply(SessionBuilder.buildRequestWithSessionNoUserAndToken()))

}
