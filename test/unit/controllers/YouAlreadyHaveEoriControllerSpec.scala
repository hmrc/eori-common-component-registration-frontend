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

import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.YouAlreadyHaveEoriController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{standalone_already_have_eori, you_already_have_eori}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache

import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class YouAlreadyHaveEoriControllerSpec extends ControllerSpec with AuthActionMock {
  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)
  private val mockSessionCache  = mock[SessionCache]

  private val youAlreadyHaveEoriView = instanceOf[you_already_have_eori]
  private val standAloneHaveEoriView = instanceOf[standalone_already_have_eori]

  private val controller =
    new YouAlreadyHaveEoriController(
      mockAuthAction,
      mockSessionCache,
      youAlreadyHaveEoriView,
      standAloneHaveEoriView,
      mcc
    )

  "YouAlreadyHaveEoriController" should {
    "display correct page for ATAR" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      display { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should startWith(messages("cds.registration.you-already-have-eori.title"))
      }
    }

    "display correct page for Standalone" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockSessionCache.eori(any[Request[_]]))
        .thenReturn(Future.successful(Some("testEori")))
      displayStandAlone { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should startWith(messages("cds.registration.you-already-have-eori.group.title"))
      }
    }
  }

  private def display(test: Future[Result] => Any) =
    await(test(controller.display(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))))

  private def displayStandAlone(test: Future[Result] => Any) =
    await(
      test(controller.displayStandAlone(eoriOnlyService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
    )

}
