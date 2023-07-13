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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.VatGroupsCannotRegisterUsingThisServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_groups_cannot_register_using_this_service
import util.ControllerSpec
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.Future

class VatGroupsCannotRegisterUsingThisServiceControllerSpec
    extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val view = instanceOf[vat_groups_cannot_register_using_this_service]

  private val controller =
    new VatGroupsCannotRegisterUsingThisServiceController(view, mcc)

  "Accessing the page" should {

    "allow unauthenticated users to access the page" in {
      show() { result =>
        status(result) shouldBe OK
        CdsPage(contentAsString(result)).title() should startWith("You need to use a different service")
      }
    }
  }

  def show()(test: Future[Result] => Any): Unit =
    test(controller.form(atarService).apply(request = SessionBuilder.buildRequestWithSessionNoUserAndToken()))

}
