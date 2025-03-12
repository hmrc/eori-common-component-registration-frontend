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

import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.IndStCannotRegisterUsingThisServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.ind_st_cannot_register_using_this_service
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.Future

class IndStCannotRegisterUsingThisServiceControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val view = inject[ind_st_cannot_register_using_this_service]

  private val controller =
    new IndStCannotRegisterUsingThisServiceController(view, mcc)

  override def beforeEach(): Unit =
    super.beforeEach()

  override protected def afterEach(): Unit =
    super.afterEach()

  "Individual Sole Trader CannotRegisterUsingThisServiceController Controller form in create mode" should {

    "Display successfully" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)

      showCreateForm(atarService) { result =>
        status(result) shouldBe OK
      }
    }
  }

  private def showCreateForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.form(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

}
