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

import common.pages.GYEEUVATNumber
import common.pages.subscription.{DisclosePersonalDetailsConsentPage, SoleTraderEuVatDetailsPage}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.{LOCATION, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{
  SubscriptionFlowManager,
  VatRegisteredEuController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  SubscriptionFlow,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService,
  SubscriptionVatEUDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_registered_eu
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.YesNoFormBuilder._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VatRegisteredEuControllerSpec extends ControllerSpec with AuthActionMock {

  private val mockAuthConnector                   = mock[AuthConnector]
  private val mockAuthAction                      = authAction(mockAuthConnector)
  private val mockSubscriptionFlowManager         = mock[SubscriptionFlowManager]
  private val mockSubscriptionVatEUDetailsService = mock[SubscriptionVatEUDetailsService]
  private val mockSubscriptionBusinessService     = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService      = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlow                = mock[SubscriptionFlow]
  private val mockSubscriptionFlowInfo            = mock[SubscriptionFlowInfo]
  private val mockSessionCache                    = mock[SessionCache]
  private val mockSubscriptionPage                = mock[SubscriptionPage]
  private val mockRequestSession                  = mock[RequestSessionData]
  private val vatRegisteredEuView                 = instanceOf[vat_registered_eu]

  private val emptyVatEuDetails: Seq[VatEUDetailsModel] = Seq.empty
  private val someVatEuDetails: Seq[VatEUDetailsModel]  = Seq(VatEUDetailsModel("1234", "FR"))

  // TODO Investigate this, mocks works incorrectly without it
  implicit val hc = mock[HeaderCarrier]

  private val controller = new VatRegisteredEuController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionDetailsService,
    mockSubscriptionVatEUDetailsService,
    mockRequestSession,
    mcc,
    vatRegisteredEuView,
    mockSubscriptionFlowManager
  )

  "Vat registered Eu Controller" should {
    "return OK when accessing page through createForm method" in {
      createForm() { result =>
        status(result) shouldBe OK
      }
    }
    "land on a correct location" in {
      createForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title should include(SoleTraderEuVatDetailsPage.title)
      }
    }
    "return ok when accessed from review method" in {
      reviewForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(SoleTraderEuVatDetailsPage.title)
      }
    }
  }

  "Submitting Vat registered Eu Controller in create mode" should {
    "return to the same location with bad request" in {
      submitForm(invalidRequest, atarService) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect to add vat details page for yes answer and no vat details in the cache" in {
      when(mockSubscriptionDetailsService.cacheVatRegisteredEu(any[YesNo])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier])).thenReturn(emptyVatEuDetails)
      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]))
        .thenReturn(mockSubscriptionFlowInfo)
      when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
      when(mockSubscriptionPage.url(any())).thenReturn(GYEEUVATNumber.url)
      submitForm(ValidRequest, atarService) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/vat-details-eu")
      }
    }

    "redirect to vat confirm page when vat details found in the cache" in {
      when(mockSubscriptionDetailsService.cacheVatRegisteredEu(any[YesNo])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier])).thenReturn(someVatEuDetails)
      submitForm(ValidRequest, atarService) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/vat-details-eu-confirm")
      }
    }

    "redirect to disclose page for no answer" in {
      when(mockSubscriptionDetailsService.cacheVatRegisteredEu(any[YesNo])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier])).thenReturn(emptyVatEuDetails)
      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]).nextPage.url(any())).thenReturn(
        DisclosePersonalDetailsConsentPage.url
      )
      submitForm(ValidRequest, atarService) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/register/disclose-personal-details-consent")
      }
    }
  }

  "Submitting Vat registered Eu Controller in review mode" should {
    "redirect to add vat details page for yes answer and none vat details in the cache" in {
      when(mockSubscriptionDetailsService.cacheVatRegisteredEu(any[YesNo])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier])).thenReturn(emptyVatEuDetails)
      submitForm(ValidRequest, atarService, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/vat-details-eu/review")
      }
    }

    "redirect to add vat confirm page for yes answer and some vat details in the cache" in {
      when(mockSubscriptionDetailsService.cacheVatRegisteredEu(any[YesNo])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      when(mockSubscriptionVatEUDetailsService.cachedEUVatDetails(any[HeaderCarrier])).thenReturn(someVatEuDetails)
      submitForm(ValidRequest, atarService, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("register/vat-details-eu-confirm/review")
      }
    }

    "redirect to review determine controller for no answer" in {
      when(mockSubscriptionDetailsService.cacheVatRegisteredEu(any[YesNo])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      when(mockSubscriptionVatEUDetailsService.saveOrUpdate(any[Seq[VatEUDetailsModel]])(any[HeaderCarrier]))
        .thenReturn(Future.successful[Unit](()))
      submitForm(validRequestNo, atarService, isInReviewMode = true) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/register/matching/review-determine")
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
    when(mockSessionCache.subscriptionDetails).thenReturn(any)
    when(mockSubscriptionBusinessService.getCachedVatRegisteredEu).thenReturn(true)
    test(controller.reviewForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String], service: Service, isInReviewMode: Boolean = false)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    mockIsIndividual()
    test(
      controller
        .submit(isInReviewMode: Boolean, service, Journey.Register)
        .apply(SessionBuilder.buildRequestWithFormValues(form))
    )
  }

  private def mockIsIndividual() = {
    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]])).thenReturn(mockSubscriptionFlow)
    when(mockSubscriptionFlow.isIndividualFlow).thenReturn(true)
  }

}
