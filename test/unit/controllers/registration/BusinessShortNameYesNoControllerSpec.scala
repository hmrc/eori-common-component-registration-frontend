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

package unit.controllers.registration

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.BusinessShortNameYesNoController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  BusinessShortName,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.business_short_name_yes_no
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class BusinessShortNameYesNoControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlow           = mock[SubscriptionFlowManager]
  private val mockSubscriptionFlowInfo       = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage           = mock[SubscriptionPage]
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val businessShortNameYesNoPage     = mock[business_short_name_yes_no]
  private val orgTypeLookup                  = mock[OrgTypeLookup]

  private val controller = new BusinessShortNameYesNoController(
    mockAuthAction,
    mockSubscriptionDetailsService,
    mockSubscriptionFlow,
    mockRequestSessionData,
    mcc,
    businessShortNameYesNoPage,
    orgTypeLookup
  )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(businessShortNameYesNoPage.apply(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)
    when(orgTypeLookup.etmpOrgType(any(), any())).thenReturn(Future.successful(CorporateBody))

    when(mockSubscriptionFlow.stepInformation(any())(any[Request[AnyContent]]))
      .thenReturn(mockSubscriptionFlowInfo)
    when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
    when(mockSubscriptionPage.url(any())).thenReturn("next-page-url")
  }

  override protected def afterEach(): Unit = {
    reset(
      mockAuthConnector,
      mockSubscriptionDetailsService,
      mockSubscriptionFlow,
      mockRequestSessionData,
      businessShortNameYesNoPage,
      orgTypeLookup
    )

    super.afterEach()
  }

  "Business Short Name Yes No Controller" should {

    "display the page" when {

      "yes/no asnwer is presented in cache" in {

        when(mockSubscriptionDetailsService.cachedCompanyShortName(any()))
          .thenReturn(Some(BusinessShortName(false, None)))
        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe OK

        val formCaptor: ArgumentCaptor[Form[YesNo]]       = ArgumentCaptor.forClass(classOf[Form[YesNo]])
        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage).apply(
          formCaptor.capture(),
          any(),
          isRowCaptor.capture(),
          isInReviewModeCaptor.capture(),
          any()
        )(any(), any())

        formCaptor.getValue.data shouldBe Map("yes-no-answer" -> "false")
        isRowCaptor.getValue shouldBe false
        isInReviewModeCaptor.getValue shouldBe false
      }

      "yes/no answer is not presented in cache" in {

        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(None)
        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe OK

        val formCaptor: ArgumentCaptor[Form[YesNo]]       = ArgumentCaptor.forClass(classOf[Form[YesNo]])
        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage).apply(
          formCaptor.capture(),
          any(),
          isRowCaptor.capture(),
          isInReviewModeCaptor.capture(),
          any()
        )(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
        isRowCaptor.getValue shouldBe false
        isInReviewModeCaptor.getValue shouldBe false
      }

      "user is RoW" in {

        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(None)
        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(false)

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe OK

        val formCaptor: ArgumentCaptor[Form[YesNo]]       = ArgumentCaptor.forClass(classOf[Form[YesNo]])
        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage).apply(
          formCaptor.capture(),
          any(),
          isRowCaptor.capture(),
          isInReviewModeCaptor.capture(),
          any()
        )(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
        isRowCaptor.getValue shouldBe true
        isInReviewModeCaptor.getValue shouldBe false
      }
    }

    "review the page" when {

      "yes/no asnwer is presented in cache" in {

        when(mockSubscriptionDetailsService.cachedCompanyShortName(any()))
          .thenReturn(Some(BusinessShortName(false, None)))
        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe OK

        val formCaptor: ArgumentCaptor[Form[YesNo]]       = ArgumentCaptor.forClass(classOf[Form[YesNo]])
        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage).apply(
          formCaptor.capture(),
          any(),
          isRowCaptor.capture(),
          isInReviewModeCaptor.capture(),
          any()
        )(any(), any())

        formCaptor.getValue.data shouldBe Map("yes-no-answer" -> "false")
        isRowCaptor.getValue shouldBe false
        isInReviewModeCaptor.getValue shouldBe true
      }

      "yes/no answer is not presented in cache" in {

        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(None)
        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe OK

        val formCaptor: ArgumentCaptor[Form[YesNo]]       = ArgumentCaptor.forClass(classOf[Form[YesNo]])
        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage).apply(
          formCaptor.capture(),
          any(),
          isRowCaptor.capture(),
          isInReviewModeCaptor.capture(),
          any()
        )(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
        isRowCaptor.getValue shouldBe false
        isInReviewModeCaptor.getValue shouldBe true
      }

      "user is RoW" in {

        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(None)
        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(false)

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe OK

        val formCaptor: ArgumentCaptor[Form[YesNo]]       = ArgumentCaptor.forClass(classOf[Form[YesNo]])
        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage).apply(
          formCaptor.capture(),
          any(),
          isRowCaptor.capture(),
          isInReviewModeCaptor.capture(),
          any()
        )(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
        isRowCaptor.getValue shouldBe true
        isInReviewModeCaptor.getValue shouldBe true
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user didn't choose any option as UK user" in {

        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)

        val result = controller.submit(atarService, false)(postRequest("yes-no-answer" -> ""))

        status(result) shouldBe BAD_REQUEST

        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage)
          .apply(any(), any(), isRowCaptor.capture(), isInReviewModeCaptor.capture(), any())(any(), any())

        isRowCaptor.getValue shouldBe false
        isInReviewModeCaptor.getValue shouldBe false
      }

      "user didn't choose any option as RoW user" in {

        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(false)

        val result = controller.submit(atarService, true)(postRequest("yes-no-answer" -> ""))

        status(result) shouldBe BAD_REQUEST

        val isRowCaptor: ArgumentCaptor[Boolean]          = ArgumentCaptor.forClass(classOf[Boolean])
        val isInReviewModeCaptor: ArgumentCaptor[Boolean] = ArgumentCaptor.forClass(classOf[Boolean])

        verify(businessShortNameYesNoPage)
          .apply(any(), any(), isRowCaptor.capture(), isInReviewModeCaptor.capture(), any())(any(), any())

        isRowCaptor.getValue shouldBe true
        isInReviewModeCaptor.getValue shouldBe true
      }
    }

    "return 303 (SEE_OTHER) and redirect to Business Short Name page" when {

      "user choose Yes and is not in review mode" in {

        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)
        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDetailsService.cacheCompanyShortName(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, false)(postRequest("yes-no-answer" -> "true"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/register/company-short-name"
      }

      "user choose Yes and is in review mode" in {

        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)
        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDetailsService.cacheCompanyShortName(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, true)(postRequest("yes-no-answer" -> "true"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/register/company-short-name/review"
      }
    }

    "return 303 (SEE_OTHER) and redirect to Determine Review page" when {

      "user choose No and is in review mode" in {

        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)
        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDetailsService.cacheCompanyShortName(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, true)(postRequest("yes-no-answer" -> "false"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/register/matching/review-determine"
      }
    }

    "return 303 (SEE_OTHER) and redirect to Subscriprion Flow Manager" when {

      "user choose No and is not in review mode" in {

        when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(true)
        when(mockSubscriptionDetailsService.cachedCompanyShortName(any())).thenReturn(Future.successful(None))
        when(mockSubscriptionDetailsService.cacheCompanyShortName(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, false)(postRequest("yes-no-answer" -> "false"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "next-page-url"
      }
    }
  }
}
