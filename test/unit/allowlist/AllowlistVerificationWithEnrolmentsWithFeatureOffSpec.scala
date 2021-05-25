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

package unit.allowlist

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.YouNeedADifferentServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.you_need_different_service
import util.ControllerSpec
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}

class AllowlistVerificationWithEnrolmentsWithFeatureOffSpec extends ControllerSpec with AuthActionMock {

  val app: Application = new GuiceApplicationBuilder()
    .disable[com.kenshoo.play.metrics.PlayModule]
    .configure("metrics.enabled" -> false)
    .configure(Map("allowlistEnabled" -> false, "allowlist" -> "  mister_allow@example.com, bob@example.com"))
    .build()

  private val auth           = mock[AuthConnector]
  private val mockAuthAction = authAction(auth)

  private val youNeedDifferentServiceView = instanceOf[you_need_different_service]

  private val controller = new YouNeedADifferentServiceController(mockAuthAction, youNeedDifferentServiceView, mcc)

  "Allowlist verification" should {

    "return OK (200) when a non-allowlisted user attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser(defaultUserId, auth, userEmail = Some("not@example.com"))

      val result = controller
        .form(Journey.Subscribe)
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/subscribe/", defaultUserId))

      status(result) shouldBe OK
    }

    "return OK (200) when a allowlisted user attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser(defaultUserId, auth, userEmail = Some("mister_allow@example.com"))

      val result = controller
        .form(Journey.Subscribe)
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/subscribe/", defaultUserId))

      status(result) shouldBe OK
    }
  }
}
