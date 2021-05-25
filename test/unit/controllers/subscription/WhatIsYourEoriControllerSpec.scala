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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.GroupEnrolmentExtractor
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.WhatIsYourEoriController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, ExistingEori, KeyValue}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.EoriNumberViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  EnrolmentStoreProxyService,
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.what_is_your_eori
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.AuthActionMock

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class WhatIsYourEoriControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector               = mock[AuthConnector]
  private val mockAuthAction                  = authAction(mockAuthConnector)
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService  = mock[SubscriptionDetailsService]
  private val mockRequestSessionData          = mock[RequestSessionData]
  private val groupEnrolmentExtractor         = mock[GroupEnrolmentExtractor]
  private val whatIsYourEoriView              = mock[what_is_your_eori]

  private val enrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val controller = new WhatIsYourEoriController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionDetailsService,
    groupEnrolmentExtractor,
    enrolmentStoreProxyService,
    mockRequestSessionData,
    mcc,
    whatIsYourEoriView
  )

  val existingGroupEnrolment: EnrolmentResponse =
    EnrolmentResponse("HMRC-OTHER-ORG", "Active", List(KeyValue("EORINumber", "GB1234567890")))

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(whatIsYourEoriView.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(
      mockAuthConnector,
      mockSubscriptionBusinessService,
      mockSubscriptionDetailsService,
      groupEnrolmentExtractor,
      enrolmentStoreProxyService,
      mockRequestSessionData,
      whatIsYourEoriView
    )

    super.afterEach()
  }

  private val eori               = "GB123456789012"
  private val eoriWithoutCountry = eori.drop(2)

  "What Is Your Eori Controller" should {

    "return 200 (OK) for user without existing enrolment" when {

      "createForm method is invoked and eori is presented in cache (dropped GB for eori for displaying purposes)" in {

        when(groupEnrolmentExtractor.groupIdEnrolments(any())(any())).thenReturn(Future.successful(List.empty))
        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(Future.successful(Some(eori)))

        val result = controller.createForm(atarService)(getRequest)

        val formCaptor: ArgumentCaptor[Form[EoriNumberViewModel]] =
          ArgumentCaptor.forClass(classOf[Form[EoriNumberViewModel]])

        status(result) shouldBe OK

        verify(whatIsYourEoriView).apply(formCaptor.capture(), meq(false), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe Map("eori-number" -> eoriWithoutCountry)
      }

      "createForm method is invoked and eori is not presented in cache" in {

        when(groupEnrolmentExtractor.groupIdEnrolments(any())(any())).thenReturn(Future.successful(List.empty))
        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(Future.successful(None))

        val result = controller.createForm(atarService)(getRequest)

        val formCaptor: ArgumentCaptor[Form[EoriNumberViewModel]] =
          ArgumentCaptor.forClass(classOf[Form[EoriNumberViewModel]])

        status(result) shouldBe OK

        verify(whatIsYourEoriView).apply(formCaptor.capture(), meq(false), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
      }

      "reviewForm method is invoked and eori is presented in cache (dropped GB for eori for displaying purposes)" in {

        when(groupEnrolmentExtractor.groupIdEnrolments(any())(any())).thenReturn(Future.successful(List.empty))
        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(Future.successful(Some(eori)))

        val result = controller.reviewForm(atarService)(getRequest)

        val formCaptor: ArgumentCaptor[Form[EoriNumberViewModel]] =
          ArgumentCaptor.forClass(classOf[Form[EoriNumberViewModel]])

        status(result) shouldBe OK

        verify(whatIsYourEoriView).apply(formCaptor.capture(), meq(true), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe Map("eori-number" -> eoriWithoutCountry)
      }

      "reviewForm method is invoked and eori is not presented in cache" in {

        when(groupEnrolmentExtractor.groupIdEnrolments(any())(any())).thenReturn(Future.successful(List.empty))
        when(mockSubscriptionBusinessService.cachedEoriNumber(any())).thenReturn(Future.successful(None))

        val result = controller.reviewForm(atarService)(getRequest)

        val formCaptor: ArgumentCaptor[Form[EoriNumberViewModel]] =
          ArgumentCaptor.forClass(classOf[Form[EoriNumberViewModel]])

        status(result) shouldBe OK

        verify(whatIsYourEoriView).apply(formCaptor.capture(), meq(true), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "eori provided by user is incorrect" in {

        val result = controller.submit(false, atarService)(postRequest("eori-number" -> "incorrect"))

        status(result) shouldBe BAD_REQUEST
        verify(whatIsYourEoriView).apply(any(), any(), any(), any())(any(), any())
      }
    }

    "return 303 (SEE_OTHER) and redirect to use this eori page" when {

      "user has existing eori" in {

        val enrolmentResponse =
          EnrolmentResponse("HMRC-TEST-ORG", "Activated", List(KeyValue("EORINumber", eori)))
        when(groupEnrolmentExtractor.groupIdEnrolments(any())(any())).thenReturn(
          Future.successful(List(enrolmentResponse))
        )
        when(mockSubscriptionDetailsService.cacheExistingEoriNumber(any())(any())).thenReturn(
          Future.successful((): Unit)
        )

        val result = controller.createForm(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/use-this-eori"
      }
    }

    "return 303 (SEE_OTHER) and redirect to eori unable to use page" when {

      "eori is already used for enrolment" in {

        when(mockSubscriptionDetailsService.cacheEoriNumber(any())(any())).thenReturn(Future.successful((): Unit))
        when(enrolmentStoreProxyService.isEnrolmentInUse(any(), any())(any()))
          .thenReturn(Future.successful(Some(ExistingEori(eori, "enrolmentKey"))))

        val result = controller.submit(false, atarService)(postRequest("eori-number" -> eoriWithoutCountry))

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori/unable-to-use-id"
      }
    }

    "return 303 (SEE_OTHER) and redirect to user location page and add GB to eori" when {

      "user is not in review mode and provided eori without GB" in {

        when(mockSubscriptionDetailsService.cacheEoriNumber(any())(any())).thenReturn(Future.successful((): Unit))
        when(enrolmentStoreProxyService.isEnrolmentInUse(any(), any())(any())).thenReturn(Future.successful(None))

        val result = controller.submit(false, atarService)(postRequest("eori-number" -> eoriWithoutCountry))

        val eoriCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/user-location"

        verify(mockSubscriptionDetailsService).cacheEoriNumber(eoriCaptor.capture())(any())

        eoriCaptor.getValue shouldBe eori
      }
    }

    "return 303 (SEE_OTHER) and redirect to user location page and don't add GB to eori" when {

      "user is not in review mode and provided eori with GB" in {

        when(mockSubscriptionDetailsService.cacheEoriNumber(any())(any())).thenReturn(Future.successful((): Unit))
        when(enrolmentStoreProxyService.isEnrolmentInUse(any(), any())(any())).thenReturn(Future.successful(None))

        val result = controller.submit(false, atarService)(postRequest("eori-number" -> eori))

        val eoriCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/user-location"

        verify(mockSubscriptionDetailsService).cacheEoriNumber(eoriCaptor.capture())(any())

        eoriCaptor.getValue shouldBe eori
      }
    }

    "return 303 (SEE_OTHER) and determine review page" when {

      "user is in review mode" in {

        when(mockSubscriptionDetailsService.cacheEoriNumber(any())(any())).thenReturn(Future.successful((): Unit))
        when(enrolmentStoreProxyService.isEnrolmentInUse(any(), any())(any())).thenReturn(Future.successful(None))

        val result = controller.submit(true, atarService)(postRequest("eori-number" -> eori))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
      }
    }
  }
}
