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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.EoriTextDownloadController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.Sub02Outcome
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_number_text_download
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AllowlistVerificationSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  val customConfiguration: Config = ConfigFactory.parseString("""
      |allowlistEnabled=true
      |allowlist="mister_allow@example.com, bob@example.com"
      """.stripMargin)

  private val mockAuthConnector = mock[AuthConnector]

  private val mockAuthAction =
    new AuthAction(Configuration(customConfiguration), Environment.simple(), mockAuthConnector, actionBuilder)(global)

  private val mockCache = mock[SessionCache]

  private val eoriNumberTextDownloadView = mock[eori_number_text_download]

  private val controller =
    new EoriTextDownloadController(mockAuthAction, mockCache, eoriNumberTextDownloadView, mcc)

  override def beforeEach(): Unit = {
    when(eoriNumberTextDownloadView.apply(any(), any(), any())(any())).thenReturn(HtmlFormat.empty)
    when(mockCache.sub02Outcome(any[HeaderCarrier]))
      .thenReturn(Future.successful(Sub02Outcome("20/01/2019", "John Doe", Some("GB123456789012"))))
  }

  "Allowlist verification" should {

    "redirect to unauthorised page when a non-allowlisted user attempts to access a route" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector, userEmail = Some("not@example.com"))

      val result = controller
        .download()
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/atar/subscribe/", defaultUserId)
        )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/customs-enrolment-services/atar/subscribe/unauthorised")
    }

    "redirect to unauthorised page when a user with no email address attempts to access a route" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector, userEmail = None)

      val result = controller
        .download()
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/atar/subscribe/", defaultUserId)
        )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/customs-enrolment-services/atar/subscribe/unauthorised")
    }

    "return OK (200) when a allowlisted user attempts to access a route" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("mister_allow@example.com"))

      val result = controller
        .download()
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/subscribe/", defaultUserId))

      status(result) shouldBe OK
    }

    "return OK (200) when the session is allowlisted" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = None)
      val request = SessionBuilder
        .buildRequestWithSessionAndPath("/customs-enrolment-services/subscribe/", defaultUserId)
        .withSession("allowlisted" -> "true")

      val result = controller.download().apply(request)

      status(result) shouldBe OK
    }

    "return OK (200) when a allowlisted user attempts to access a route with email address in a different case" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("BoB@example.com"))

      val result = controller
        .download()
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/subscribe/", defaultUserId))

      status(result) shouldBe OK
    }

    "not apply to Get Your EORI journey" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector, userEmail = Some("not@example.com"))

      val result = controller
        .download()
        .apply(SessionBuilder.buildRequestWithSessionAndPath("/customs-enrolment-services/register/", defaultUserId))

      status(result) shouldBe OK
    }
  }
}
