/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.WhatIsYourOrgNameController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_org_name
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.OrganisationNameFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatIsYourOrgNameControllerRowSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val whatIsYourOrgNameView          = instanceOf[what_is_your_org_name]
  private val mockAppConfig                  = mock[AppConfig]

  private val controller =
    new WhatIsYourOrgNameController(
      mockAuthAction,
      mcc,
      whatIsYourOrgNameView,
      mockSubscriptionDetailsService,
      mockAppConfig
    )

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionDetailsService)

    super.afterEach()
  }

  "Submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, "third-country-organisation", atarService),
      "and isInReviewMode is false"
    )
    "redirect to the 'Do you have a UTR? page when isInReviewMode is false" in {
      when(mockSubscriptionDetailsService.updateSubscriptionDetailsOrgName(any())(any[Request[_]])).thenReturn(
        Future.successful((): Unit)
      )
      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[Request[_]]))
        .thenReturn(Future.successful(()))

      submitForm(isInReviewMode = false, form = ValidNameRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith(
          "/customs-registration-services/atar/register/matching/utr/third-country-organisation"
        )
        verify(mockSubscriptionDetailsService).cacheNameDetails(any())(any())
      }

    }

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = true, "third-country-organisation", atarService),
      "and isInReviewMode is true"
    )
    "redirect to the Determine Review page when isInReviewMode is true" in {

      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[Request[_]]))
        .thenReturn(Future.successful(()))

      submitForm(isInReviewMode = true, form = ValidNameRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith(
          "/customs-registration-services/atar/register/matching/review-determine"
        )
        verify(mockSubscriptionDetailsService).cacheNameDetails(any())(any())
      }
    }
  }

  def submitForm(isInReviewMode: Boolean, form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .submit(isInReviewMode, "third-country-organisation", atarService)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

}
