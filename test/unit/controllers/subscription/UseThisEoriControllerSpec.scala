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
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.UseThisEoriController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.MissingExistingEori
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.ExistingEori
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.use_this_eori
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class UseThisEoriControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockUseThisEoriPage            = mock[use_this_eori]

  private val controller =
    new UseThisEoriController(mockAuthAction, mockSubscriptionDetailsService, mcc, mockUseThisEoriPage)(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockUseThisEoriPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, mockSubscriptionDetailsService, mockUseThisEoriPage)

    super.afterEach()
  }

  "Use this eori controller" should {

    "return 200 (OK)" when {

      "display method is invoked" in {

        when(mockSubscriptionDetailsService.cachedExistingEoriNumber(any()))
          .thenReturn(Future.successful(Some(ExistingEori("GB123456789123", "enrolmentKey"))))

        val result = controller.display(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockUseThisEoriPage).apply(any(), any())(any(), any())
      }
    }

    "throw an exception" when {

      "eori is not presented in cache for display method" in {

        when(mockSubscriptionDetailsService.cachedExistingEoriNumber(any())).thenReturn(Future.successful(None))

        intercept[MissingExistingEori] {
          await(controller.display(atarService)(getRequest))
        }
      }

      "eori is not presented in cache for submit method" in {

        when(mockSubscriptionDetailsService.cachedExistingEoriNumber(any())).thenReturn(Future.successful(None))

        intercept[MissingExistingEori] {
          await(controller.submit(atarService)(postRequest()))
        }
      }
    }

    "return 303 (SEE_OTHER) and cache eori" when {

      "submit method is invoked and eori is presented in cache" in {

        when(mockSubscriptionDetailsService.cachedExistingEoriNumber(any()))
          .thenReturn(Future.successful(Some(ExistingEori("GB123456789123", "enrolmentKey"))))
        when(mockSubscriptionDetailsService.cacheEoriNumber(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService)(postRequest())

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/user-location"
      }
    }
  }
}
