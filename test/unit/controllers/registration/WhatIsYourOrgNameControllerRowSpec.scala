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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.WhatIsYourOrgNameController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.what_is_your_org_name
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.matching.OrganisationNameFormBuilder._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class WhatIsYourOrgNameControllerRowSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val whatIsYourOrgNameView          = instanceOf[what_is_your_org_name]

  private val controller = new WhatIsYourOrgNameController(
    mockAuthAction,
    mockRequestSessionData,
    mcc,
    whatIsYourOrgNameView,
    mockSubscriptionDetailsService
  )

  override def beforeEach: Unit =
    reset(mockRequestSessionData, mockSubscriptionDetailsService)

  "Submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, "third-country-organisation", atarService, Journey.Register),
      "and isInReviewMode is false"
    )
    "redirect to the 'Do you have a UTR? page when isInReviewMode is false" in {

      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))

      submitForm(isInReviewMode = false, form = ValidNameRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(
          "/customs-enrolment-services/atar/register/matching/utr/third-country-organisation"
        )
        verify(mockSubscriptionDetailsService).cacheNameDetails(any())(any())
      }

    }

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = true, "third-country-organisation", atarService, Journey.Register),
      "and isInReviewMode is true"
    )
    "redirect to the Determine Review page when isInReviewMode is true" in {

      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))
      when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]]))
        .thenReturn(Some(UserLocation.ThirdCountry))

      submitForm(isInReviewMode = true, form = ValidNameRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(
          "/customs-enrolment-services/atar/register/matching/review-determine"
        )
        verify(mockSubscriptionDetailsService).cacheNameDetails(any())(any())
      }
    }
  }

  def submitForm(isInReviewMode: Boolean, form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)
    val result = controller
      .submit(isInReviewMode, "third-country-organisation", atarService, Journey.Register)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

}
