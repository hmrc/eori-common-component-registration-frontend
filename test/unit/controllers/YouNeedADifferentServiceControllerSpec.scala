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

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.YouNeedADifferentServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.you_need_different_service
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

class YouNeedADifferentServiceControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val youNeedDifferenceServicePage = instanceOf[you_need_different_service]
  private val authConnector                = mock[AuthConnector]
  private val mockAuthAction               = authAction(authConnector)

  private val controller = new YouNeedADifferentServiceController(mockAuthAction, youNeedDifferenceServicePage, mcc)

  override protected def beforeEach(): Unit =
    super.beforeEach()

  override protected def afterEach(): Unit = {
    reset(authConnector)
    super.afterEach()
  }

  "YouNeedADifferentServiceController on method form" should {

    "return youNeedDifferentService page" in {

      withAuthorisedUser(defaultUserId, authConnector)

      val result = controller.form(atarService)(FakeRequest())

      status(result) shouldBe OK
    }
  }
}
