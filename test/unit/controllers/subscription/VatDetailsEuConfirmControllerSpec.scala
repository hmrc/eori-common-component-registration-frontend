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

import common.pages.subscription.SubscriptionContactDetailsPage.pageLevelErrorSummaryListXPath
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{
  SubscriptionFlowManager,
  VatDetailsEuConfirmController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionVatEUDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_details_eu_confirm
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.YesNoFormBuilder._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VatDetailsEuConfirmControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                   = mock[AuthConnector]
  private val mockAuthAction                      = authAction(mockAuthConnector)
  private val mockSubscriptionFlowManager         = mock[SubscriptionFlowManager]
  private val mockSubscriptionFlowInfo            = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage                = mock[SubscriptionPage]
  private val mockSubscriptionVatEUDetailsService = mock[SubscriptionVatEUDetailsService]
  private val vatDetailsEuConfirmView             = instanceOf[vat_details_eu_confirm]

  private val controller = new VatDetailsEuConfirmController(
    mockAuthAction,
    mockSubscriptionVatEUDetailsService,
    mcc,
    vatDetailsEuConfirmView,
    mockSubscriptionFlowManager
  )

  private val VatEuDetailUnderLimit = Seq(VatEUDetailsModel("12345", "FR"))

  private val VatEuDetailsOnLimit = VatEuDetailUnderLimit ++ Seq(
    VatEUDetailsModel("12345", "CZ"),
    VatEUDetailsModel("12345", "ES"),
    VatEUDetailsModel("123456", "DK"),
    VatEUDetailsModel("12345", "DE")
  )

  override def beforeEach {
    reset(mockSubscriptionVatEUDetailsService, mockSubscriptionFlowManager, mockSubscriptionPage)
  }

  "Viewing the form" should {
    "return ok and display no errors for cached euVatDetails under limit" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailUnderLimit))
      displayForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        verify(mockSubscriptionVatEUDetailsService, times(1)).cachedEUVatDetails(any[HeaderCarrier])
      }
    }

    "return ok and display no errors for cached euVatDetails on limit" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailsOnLimit))
      displayForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        verify(mockSubscriptionVatEUDetailsService, times(1)).cachedEUVatDetails(any[HeaderCarrier])
      }
    }

    "redirect to EuVatRegistered page in create mode when cached euVatDetails was empty and isInReviewMode set to false" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Seq()))
      displayForm() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/customs-enrolment-services/atar/register/vat-registered-eu")
        verify(mockSubscriptionVatEUDetailsService, times(1)).cachedEUVatDetails(any[HeaderCarrier])
      }
    }

    "redirect to EuVatRegistered page in review mode when cached euVatDetails was empty and isInReviewMode set to true" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Seq()))
      reviewForm() { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          "/customs-enrolment-services/atar/register/vat-registered-eu/review"
        )
        verify(mockSubscriptionVatEUDetailsService, times(1)).cachedEUVatDetails(any[HeaderCarrier])
      }
    }
  }

  "Submitting the form with cached euDetails under limit in create mode" should {

    "redirect to vatDetails page when yes answer selected" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailUnderLimit))
      submitForm(ValidRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/customs-enrolment-services/atar/register/vat-details-eu")
      }
    }

    "redirect to disclose personal details eu page when yes selected" in {
      val url = "/customs-enrolment-services/register/disclose-personal-details-consent"
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailUnderLimit))
      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]))
        .thenReturn(mockSubscriptionFlowInfo)
      when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
      when(mockSubscriptionPage.url(atarService)).thenReturn(url)
      submitForm(validRequestNo) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(url)
        verify(mockSubscriptionFlowManager, times(1))
          .stepInformation(any())(any[Request[AnyContent]])
      }
    }
  }

  "Submitting the form with cached euDetails under limit in review mode" should {

    "redirect to vatDetails page when yes answer selected" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailUnderLimit))
      submitForm(ValidRequest, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          "/customs-enrolment-services/atar/register/vat-details-eu/review"
        )
      }
    }

    "redirect to review-determine when no selected" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailUnderLimit))
      submitForm(validRequestNo, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          "/customs-enrolment-services/atar/register/matching/review-determine"
        )
      }
    }
  }

  "Submitting the form with cached euDetails on limit and no option selected" should {
    "redirect to review-determine when yes answer selected and in review mode" in {
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailsOnLimit))
      submitForm(invalidRequest, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(
          "/customs-enrolment-services/atar/register/matching/review-determine"
        )
      }
    }

    "redirect to disclose personal details eu page when yes selected" in {
      val url = "/customs-enrolment-services/register/disclose-personal-details-consent"
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(VatEuDetailsOnLimit))
      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]))
        .thenReturn(mockSubscriptionFlowInfo)
      when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
      when(mockSubscriptionPage.url(atarService)).thenReturn(url)
      submitForm(invalidRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(url)
        verify(mockSubscriptionFlowManager, times(1))
          .stepInformation(any())(any[Request[AnyContent]])
      }
    }
  }

  private def displayForm()(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(
      test(
        controller.createForm(atarService, Journey.Register).apply(
          SessionBuilder.buildRequestWithSession(defaultUserId)
        )
      )
    )
  }

  private def reviewForm()(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(
      test(
        controller.reviewForm(atarService, Journey.Register).apply(
          SessionBuilder.buildRequestWithSession(defaultUserId)
        )
      )
    )
  }

  private def submitForm(form: Map[String, String], isInReviewMode: Boolean = false)(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(
      test(
        controller
          .submit(isInReviewMode, atarService, Journey.Register)
          .apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form))
      )
    )
  }

}
