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

package unit.controllers.subscription

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.CompletedEnrolmentController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.eori_enrol_success
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class CompletedEnrolmentControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector    = mock[AuthConnector]
  private val mockAuthAction       = authAction(mockAuthConnector)
  private val mockSessionCache     = mock[SessionCache]
  private val mockEnrolSuccessView = mock[eori_enrol_success]

  private val controller =
    new CompletedEnrolmentController(mockAuthAction, mockSessionCache, mcc, mockEnrolSuccessView)(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockEnrolSuccessView.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, mockSessionCache, mockEnrolSuccessView)

    super.afterEach()
  }

  "Completed enrolment controller" should {

    "display enrolled successfully page" in {

      val atarEnrolment =
        Enrolment("HMRC-ATAR-ORG", Seq(EnrolmentIdentifier("EORINumber", "GB123456789012")), "Activated")

      withAuthorisedUser(defaultUserId, mockAuthConnector, otherEnrolments = Set(atarEnrolment))
      when(mockSessionCache.remove(any())).thenReturn(Future.successful(true))

      val result = controller.enrolSuccess(atarService)(getRequest)

      status(result) shouldBe OK
      verify(mockEnrolSuccessView).apply(any(), any())(any(), any())
    }

    "throw an exception" when {

      "user doesn't have the enrolment" in {

        withAuthorisedUser(defaultUserId, mockAuthConnector)

        intercept[IllegalStateException] {
          await(controller.enrolSuccess(atarService)(getRequest))
        }
      }
    }
  }
}
