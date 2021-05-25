/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.YouAlreadyHaveEoriController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.you_already_have_eori
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.Future

class YouAlreadyHaveEoriControllerSpec extends ControllerSpec with AuthActionMock {
  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)

  private val youAlreadyHaveEoriView = instanceOf[you_already_have_eori]

  private val controller =
    new YouAlreadyHaveEoriController(mockAuthAction, youAlreadyHaveEoriView, mcc)

  "YouAlreadyHaveEoriController" should {
    "display correct page" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      display { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should startWith(messages("cds.registration.you-already-have-eori.title"))
      }
    }
  }

  private def display(test: Future[Result] => Any) =
    await(test(controller.display(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))))

}
