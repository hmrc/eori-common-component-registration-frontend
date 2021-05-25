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

package unit.views.partials

import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.GroupEnrolmentExtractor
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.EnrolmentStoreProxyService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{start, start_subscribe}
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global

class HeaderSpec extends ControllerSpec with AuthActionMock {

  private val mockAuthConnector    = mock[AuthConnector]
  private val mockAuthAction       = authAction(mockAuthConnector)
  private val mockCdsFrontendCache = mock[SessionCache]

  private val mockEnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val viewStartRegister       = instanceOf[start]
  private val viewStartSubscribe      = instanceOf[start_subscribe]
  private val groupEnrolmentExtractor = mock[GroupEnrolmentExtractor]

  private val controller = new ApplicationController(
    mockAuthAction,
    mcc,
    viewStartSubscribe,
    viewStartRegister,
    mockCdsFrontendCache,
    groupEnrolmentExtractor,
    mockEnrolmentStoreProxyService,
    appConfig
  )

  "Header Sign in link" should {

    "be present when the user is logged in" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector)

      val result = controller.startRegister(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      val page = CdsPage(contentAsString(result))
      page.elementIsPresent("//a[@id='sign-out']") shouldBe true
    }

    "not be present when a user isn't logged in" in {
      AuthBuilder.withNotLoggedInUser(mockAuthConnector)

      val result = controller.startRegister(atarService).apply(SessionBuilder.buildRequestWithSessionNoUser)

      val page = CdsPage(contentAsString(result))
      page.elementIsPresent("//a[@id='sign-out']") shouldBe false
    }
  }

  "Feedback URL" should {
    "be present with service param equal to 'eori-common-component-subscribe''" in {
      val result = controller
        .startRegister(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPathNoUser(
            method = "GET",
            path = "/customs-enrolment-services/atar/subscribe/"
          )
        )

      val page = CdsPage(contentAsString(result))

      page.getElementAttribute("//a[@id='feedback-link']", "href") should endWith(
        "/contact/beta-feedback?service=eori-common-component-subscribe-atar"
      )
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
}
