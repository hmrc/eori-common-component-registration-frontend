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

package unit.views.partials

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Request
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.start
import unit.controllers.CdsPage
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}
import util.{CSRFTest, ControllerSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HeaderSpec extends ControllerSpec with AuthActionMock with CSRFTest {

  private val mockAuthConnector    = mock[AuthConnector]
  private val mockAuthAction       = authAction(mockAuthConnector)
  private val mockCdsFrontendCache = mock[SessionCache]
  private val viewStartRegister    = instanceOf[start]

  private val controller =
    new ApplicationController(mockAuthAction, mcc, viewStartRegister, mockCdsFrontendCache, appConfig)

  "Header Sign in link" should {

    "be present when the user is logged in" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector)

      val result = controller.startRegister(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      val page = CdsPage(contentAsString(result))
      page.elementIsPresent("//a[@class='hmrc-sign-out-nav__link']") shouldBe true
    }

    "not be present when a user isn't logged in" in {
      AuthBuilder.withNotLoggedInUser(mockAuthConnector)

      val result = controller.startRegister(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser)

      val page = CdsPage(contentAsString(result))
      page.elementIsPresent("//a[@id='sign-out']") shouldBe false
    }
  }

  "Language switch" should {

    "be always presented" in {

      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector)

      val result = controller.startRegister(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      val page = CdsPage(contentAsString(result))
      page.elementIsPresent("//nav[@class='hmrc-language-select']") shouldBe true
    }
  }

  "Signing out action" should {

    "have a correct href defined" in {
      implicit val request = withFakeCSRF(fakeAtarRegisterRequest)

      val doc = Jsoup.parse(contentAsString(viewStartRegister(atarService, "test", "test")))
      doc.body().getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr(
        "href"
      ) shouldBe "/customs-registration-services/atar/register/logout"
    }

    "take to the feedback page" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector)
      when(mockCdsFrontendCache.remove(any[Request[_]])).thenReturn(Future.successful(true))

      val result = controller.logout(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      await(result).header.headers("Location") should endWith("feedback/eori-common-component-register-atar")
    }

  }
}
