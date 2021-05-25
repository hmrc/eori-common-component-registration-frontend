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
import org.mockito.Mockito.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.EoriUnableToUseSignoutController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_signout
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

class EoriUnableToUseSignoutControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction    = authAction(mockAuthConnector)
  private val eoriSignoutPage   = mock[eori_signout]

  private val controller = new EoriUnableToUseSignoutController(mockAuthAction, mcc, eoriSignoutPage)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(eoriSignoutPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, eoriSignoutPage)

    super.afterEach()
  }

  "Eori unable to use signout controller" should {

    "display page" in {

      val result = controller.displayPage(atarService)(FakeRequest("GET", ""))

      status(result) shouldBe OK
      verify(eoriSignoutPage).apply(any(), any())(any(), any())
    }

    "return 400 (BAD REQUEST)" when {

      "user doesn't answer on the question" in {

        val result = controller.submit(atarService)(FakeRequest("POST", ""))

        status(result) shouldBe BAD_REQUEST
        verify(eoriSignoutPage).apply(any(), any())(any(), any())
      }
    }

    "redirect to Application Controller logout endpoint" when {

      "user answer yes" in {

        val result =
          controller.submit(atarService)(FakeRequest("POST", "").withFormUrlEncodedBody("yes-no-answer" -> "true"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/logout"
        verifyZeroInteractions(eoriSignoutPage)
      }
    }

    "redirect to Eori Unable to use page" when {

      "user answer no" in {

        val result =
          controller.submit(atarService)(FakeRequest("POST", "").withFormUrlEncodedBody("yes-no-answer" -> "false"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/unable-to-use-id"
        verifyZeroInteractions(eoriSignoutPage)
      }
    }
  }
}
