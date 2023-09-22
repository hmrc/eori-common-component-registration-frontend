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
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.LockedEmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.locked_email_view
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

class LockedEmailControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)

  private val view                         = instanceOf[locked_email_view]
  private val messagesControllerComponents = instanceOf[MessagesControllerComponents]

  private val controller = new LockedEmailController(messagesApi, messagesControllerComponents, mockAuthAction, view)

  override protected def beforeEach(): Unit =
    super.beforeEach()

  "onPageLoad" should {

    "display the locked email page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val service = Service.cds

      val result = controller.onPageLoad(service).apply(SessionBuilder.buildRequestWithSession("user"))

      status(result) shouldBe OK
      val page = CdsPage(contentAsString(result))
      page.h1() shouldBe messages("lockedEmail.heading")
      page.getElementById("paragraph1").text shouldBe messages("lockedEmail.para1")
      page.getElementById("paragraph2").text shouldBe messages("lockedEmail.para2")

    }
  }
}
