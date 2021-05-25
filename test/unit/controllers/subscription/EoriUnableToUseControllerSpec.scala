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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.EoriUnableToUseController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.ExistingEori
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  EnrolmentStoreProxyService,
  SubscriptionBusinessService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.eori_unable_to_use
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class EoriUnableToUseControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector               = mock[AuthConnector]
  private val mockAuthAction                  = authAction(mockAuthConnector)
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val eoriUnableToUsePage             = mock[eori_unable_to_use]
  private val mockEnrolmentStoreProxy         = mock[EnrolmentStoreProxyService]

  private val controller =
    new EoriUnableToUseController(
      mockAuthAction,
      mockSubscriptionBusinessService,
      mockEnrolmentStoreProxy,
      mcc,
      eoriUnableToUsePage
    )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(eoriUnableToUsePage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, mockSubscriptionBusinessService, eoriUnableToUsePage)

    super.afterEach()
  }

  "Eori unable to use controller" should {

    "display page when eori is presented in cache" in {

      when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(
        Future.successful(Some("GB123456789123"))
      )

      when(mockEnrolmentStoreProxy.isEnrolmentInUse(any(), any())(any())).thenReturn(
        Future.successful(Some(ExistingEori("GB123456789123", atarService.enrolmentKey)))
      )

      val result = controller.displayPage(atarService)(FakeRequest("GET", ""))

      status(result) shouldBe OK
      verify(eoriUnableToUsePage).apply(any(), any(), any())(any(), any())
    }

    "redirect to What is your eori page" when {

      "enrolment in use method returns empty" in {

        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(
          Future.successful(Some("GB123456789123"))
        )

        when(mockEnrolmentStoreProxy.isEnrolmentInUse(any(), any())(any())).thenReturn(Future.successful(None))

        val result = controller.displayPage(atarService)(FakeRequest("GET", ""))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori"
      }

      "eori is not available for display page method" in {

        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(Future.successful(None))

        val result = controller.displayPage(atarService)(FakeRequest("GET", ""))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori"
      }

      "eori is not available for submit method" in {

        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(Future.successful(None))

        val result = controller.submit(atarService)(FakeRequest("GET", ""))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori"
      }
    }

    "return 400 (Bad Request)" when {

      "answer is missing" in {

        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(
          Future.successful(Some("GB123456789123"))
        )

        val result = controller.submit(atarService)(FakeRequest("POST", ""))

        status(result) shouldBe BAD_REQUEST
        verify(eoriUnableToUsePage).apply(any(), any(), any())(any(), any())
      }
    }

    "redirect to What is Your Eori Page" when {

      "user answer with Change Eori option" in {

        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(
          Future.successful(Some("GB123456789123"))
        )

        val result =
          controller.submit(atarService)(FakeRequest("POST", "").withFormUrlEncodedBody("answer" -> "change"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori"
        verifyZeroInteractions(eoriUnableToUsePage)
      }
    }

    "redirect to Eori Unable to use Signout endpoint" when {

      "user answer with Signout" in {

        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(
          Future.successful(Some("GB123456789123"))
        )

        val result =
          controller.submit(atarService)(FakeRequest("POST", "").withFormUrlEncodedBody("answer" -> "signout"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/signout"
        verifyZeroInteractions(eoriUnableToUsePage)
      }
    }
  }
}
