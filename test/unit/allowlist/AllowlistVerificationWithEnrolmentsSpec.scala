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

import com.typesafe.config.{Config, ConfigFactory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.{Configuration, Environment}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.YouNeedADifferentServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.you_need_different_service
import util.ControllerSpec
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global

class AllowlistVerificationWithEnrolmentsSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  val customConfiguration: Config = ConfigFactory.parseString("""
      |allowlistEnabled=true
      |allowlist="mister_allow@example.com, bob@example.com"
      """.stripMargin)

  private val mockAuthConnector = mock[AuthConnector]

  private val mockAuthAction =
    new AuthAction(Configuration(customConfiguration), Environment.simple(), mockAuthConnector, actionBuilder)(global)

  private val youNeedDifferentServiceView = mock[you_need_different_service]

  private val controller = new YouNeedADifferentServiceController(mockAuthAction, youNeedDifferentServiceView, mcc)

  override def beforeEach(): Unit =
    when(youNeedDifferentServiceView.apply()(any(), any())).thenReturn(HtmlFormat.empty)

  "Allowlist verification" should {

    "return OK (200) when a allowlisted user attempts to access a route" in {
      AuthBuilder.withAuthorisedUser(defaultUserId, mockAuthConnector, userEmail = Some("mister_allow@example.com"))

      val result = controller
        .form(Journey.Subscribe)
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/subscribe/", defaultUserId))

      status(result) shouldBe OK
    }

    "not apply to Get Your EORI journey" in {
      AuthBuilder.withAuthorisedUser(defaultUserId, mockAuthConnector, userEmail = Some("not@example.com"))

      val result = controller
        .form(Journey.Register)
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/register/", defaultUserId))

      status(result) shouldBe OK
    }
  }
}
