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

import common.pages.subscription.VatRegisterUKPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.{LOCATION, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{
  SubscriptionFlowManager,
  VatRegisteredUkController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  SubscriptionFlow,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_registered_uk
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.YesNoFormBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegisteredUkSubscriptionControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector               = mock[AuthConnector]
  private val mockAuthAction                  = authAction(mockAuthConnector)
  private val mockSubscriptionFlowManager     = mock[SubscriptionFlowManager]
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService  = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlow            = mock[SubscriptionFlow]
  private val mockRequestSession              = mock[RequestSessionData]
  private val vatRegisteredUkView             = instanceOf[vat_registered_uk]

  override def beforeEach: Unit = {
    reset(
      mockAuthConnector,
      mockSubscriptionFlowManager,
      mockSubscriptionBusinessService,
      mockSubscriptionDetailsService,
      mockSubscriptionFlow,
      mockRequestSession
    )
    when(mockSubscriptionDetailsService.cacheVatRegisteredUk(any[YesNo])(any[HeaderCarrier]))
      .thenReturn(Future.successful {})
  }

  private val controller = new VatRegisteredUkController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionFlowManager,
    mockSubscriptionDetailsService,
    mockRequestSession,
    mcc,
    vatRegisteredUkView
  )

  "Vat registered Uk Controller" should {
    "return OK when accessing page through createForm method" in {
      createForm() { result =>
        status(result) shouldBe OK
      }
    }
    "land on a correct location" in {
      createForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title should include(VatRegisterUKPage.title)
      }
    }
    "return ok when accessed from review method" in {
      reviewForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(VatRegisterUKPage.title)
      }
    }
  }

  "Submitting Vat registered UK Controller in create mode" should {
    "return to the same location with bad request" in {
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to add vat group page for yes answer" in {
      val url = "register/vat-group"
      subscriptionFlowUrl(url)

      submitForm(ValidRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/vat-group")
      }
    }

    "redirect to eu vat page for no answer" in {
      val url = "register/vat-registered-eu"

      subscriptionFlowUrl(url)

      submitForm(validRequestNo) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/vat-registered-eu")
      }
    }
    "redirect to vat groups review page for yes answer and is in review mode" in {
      submitForm(ValidRequest, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/what-are-your-uk-vat-details/review")
      }
    }

    "redirect to check answers page for no answer and is in review mode" in {
      submitForm(validRequestNo, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          "customs-enrolment-services/atar/register/matching/review-determine"
        )
      }
    }
  }

  private def createForm(journey: Journey.Value = Journey.Register)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    test(controller.createForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def reviewForm(journey: Journey.Value = Journey.Register)(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    when(mockSubscriptionBusinessService.getCachedVatRegisteredUk(any[HeaderCarrier])).thenReturn(true)
    test(controller.reviewForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String], isInReviewMode: Boolean = false)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    test(
      controller
        .submit(isInReviewMode: Boolean, atarService, Journey.Register)
        .apply(SessionBuilder.buildRequestWithFormValues(form))
    )
  }

  private def mockIsIndividual(isIndividual: Boolean = false) = {
    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]])).thenReturn(mockSubscriptionFlow)
    when(mockSubscriptionFlow.isIndividualFlow).thenReturn(isIndividual)
  }

  private def subscriptionFlowUrl(url: String) = {
    val mockSubscriptionPage     = mock[SubscriptionPage]
    val mockSubscriptionFlowInfo = mock[SubscriptionFlowInfo]
    when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]))
      .thenReturn(mockSubscriptionFlowInfo)
    when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
    when(mockSubscriptionPage.url(any())).thenReturn(url)
  }

}
