/*
 * Copyright 2025 HM Revenue & Customs
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

package unit.controllers

import common.pages.subscription.VatRegisterUKPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.{LOCATION, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{SubscriptionFlowManager, VatRegisteredUkController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlow, SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_registered_uk
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.YesNoFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegisteredUkControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlow = mock[SubscriptionFlow]
  private val mockSessionError = mock[SessionError]
  private val mockRequestSession = mock[RequestSessionData]
  private val vatRegisteredUkView = inject[vat_registered_uk]
  private val mockSessionCacheService = inject[SessionCacheService]

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsService.cacheVatRegisteredUk(any[YesNo])(any[Request[_]]))
      .thenReturn(Future.successful {})

    when(mockRequestSession.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSubscriptionFlowManager)
    reset(mockSubscriptionBusinessService)
    reset(mockSubscriptionDetailsService)
    reset(mockSubscriptionFlow)
    reset(mockRequestSession)

    super.afterEach()
  }

  private val controller = new VatRegisteredUkController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionDetailsService,
    mockRequestSession,
    mcc,
    vatRegisteredUkView,
    mockSessionCacheService
  )(global)

  "Vat registered Uk Controller" should {
    when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(mockSubscriptionFlow)
    )
    "return OK when accessing page through createForm method" in {

      createForm() { result =>
        status(result) shouldBe OK
      }
    }
    "land on a correct location" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )
      createForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should include(VatRegisterUKPage.title)
      }
    }
  }

  "Vat registered Uk Controller in review mode" should {
    when(mockSubscriptionBusinessService.getCachedVatRegisteredUk(any[Request[_]])).thenReturn(Future.successful(true))

    "return OK when accessing page through createForm method" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )
      reviewForm() { result =>
        status(result) shouldBe OK
      }
    }
    "land on a correct location" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )
      reviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should include(VatRegisterUKPage.title)
      }
    }
  }

  "Submitting Vat registered UK Controller in create mode" should {
    "return to the same location with bad request" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to add vat group page for yes answer" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )
      val url = "register/vat-group"
      subscriptionFlowUrl(url)

      submitForm(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("/register/your-uk-vat-details")
      }
    }

    "redirect for Iom to vat group page for yes answer" in {
      when(mockRequestSession.selectedUserLocation(any())).thenReturn(Some(UserLocation.Iom))
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Right(mockSubscriptionFlow)
      )
      val url = "register/vat-group"
      subscriptionFlowUrl(url)

      submitForm(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("/register/your-vat-details")
      }
    }

    "redirect to start new journey for no data left case - submit form" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Left(mockSessionError)
      )
      val url = "register/vat-group"
      subscriptionFlowUrl(url)

      submitForm(invalidRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("atar/register")
      }
    }

    "redirect to start new journey for no data left case - review form" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Left(mockSessionError)
      )
      val url = "register/vat-group"
      subscriptionFlowUrl(url)

      reviewForm() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("atar/register")
      }
    }

    "redirect to start new journey for no data left case - create form" in {
      when(mockRequestSession.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
        Left(mockSessionError)
      )
      val url = "register/vat-group"
      subscriptionFlowUrl(url)

      createForm() { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("atar/register")
      }
    }

    "redirect to eu vat page for no answer using vat details controller" in {
      val url = "register/vat-registered-eu"
      when(mockSubscriptionDetailsService.clearCachedUkVatDetails(any[Request[_]])).thenReturn(
        Future.successful((): Unit)
      )

      subscriptionFlowUrl(url)

      submitForm(validRequestNo) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("register/contact-details")
      }
    }

    "redirect to vat groups review page for yes answer and is in review mode" in {
      submitForm(validRequest, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("register/your-uk-vat-details/review")
      }
    }

    "redirect for Iom to vat groups review page for yes answer and is in review mode" in {
      when(mockRequestSession.selectedUserLocation(any())).thenReturn(Some(UserLocation.Iom))
      submitForm(validRequest, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("register/your-vat-details/review")
      }
    }

    "redirect to check answers page for no answer and is in review mode" in {
      when(mockSubscriptionDetailsService.clearCachedUkVatDetails(any[Request[_]])).thenReturn(
        Future.successful((): Unit)
      )
      submitForm(validRequestNo, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith(
          "customs-registration-services/atar/register/contact-details/review"
        )
      }
    }
  }

  private def createForm()(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def reviewForm()(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    when(mockSubscriptionBusinessService.getCachedVatRegisteredUk(any[Request[_]])).thenReturn(Future.successful(true))
    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String], isInReviewMode: Boolean = false)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    test(
      controller
        .submit(isInReviewMode: Boolean, atarService)
        .apply(SessionBuilder.buildRequestWithFormValues(form))
    )
  }

  private def mockIsIndividual(isIndividual: Boolean = false) = {
    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(mockSubscriptionFlow)
    )
    when(mockSubscriptionFlow.isIndividualFlow).thenReturn(isIndividual)
  }

  private def subscriptionFlowUrl(url: String) = {
    val mockSubscriptionPage = mock[SubscriptionPage]
    val mockSubscriptionFlowInfo = mock[SubscriptionFlowInfo]
    when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
      .thenReturn(Right(mockSubscriptionFlowInfo))
    when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
    when(mockSubscriptionPage.url(any())).thenReturn(url)
  }

}
