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

package unit.controllers.migration

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.GetNinoSubscriptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  IndividualSubscriptionFlow,
  RowIndividualFlow,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CustomsId, Nino}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_nino
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetNinoSubscriptionControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlowInfo       = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage           = mock[SubscriptionPage]
  private val mockRequestSessionData         = mock[RequestSessionData]

  private val matchNinoSubscriptionView = instanceOf[how_can_we_identify_you_nino]

  private val nextPageFlowUrl = "/customs-enrolment-services/subscribe/address"

  override protected def beforeEach: Unit = {
    super.beforeEach()
    reset(mockSubscriptionDetailsService)
    when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))
  }

  val controller = new GetNinoSubscriptionController(
    mockAuthAction,
    mockSubscriptionFlowManager,
    mockRequestSessionData,
    mcc,
    matchNinoSubscriptionView,
    mockSubscriptionDetailsService
  )

  "HaveNinoSubscriptionController createForm" should {
    "return OK and display correct page" in {
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include("Enter your National Insurance number")
      }
    }
  }

  "HaveNinoSubscriptionController submit" should {
    "return BadRequest when no option selected" in {
      submit(Journey.Subscribe, Map.empty[String, String]) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return BadRequest when invalidUtr provided" in {
      val invalidNino = "01234567890123"
      submit(Journey.Subscribe, Map("nino" -> invalidNino)) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "cache NINO and redirect to Address Page of the flow" in {
      when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      mockSubscriptionFlow(nextPageFlowUrl)
      submit(Journey.Subscribe, Map("nino" -> "ab 12 34 56 c")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/subscribe/address"
      }
      verify(mockSubscriptionDetailsService).cacheCustomsId(meq(Nino("AB123456C")))(any[HeaderCarrier])
    }

    "redirect to Address page" when {

      "user is in review mode and during ROW individual journey" in {

        when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(RowIndividualFlow)
        when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))
        mockSubscriptionFlow(nextPageFlowUrl)
        submit(Journey.Subscribe, Map("nino" -> "ab 12 34 56 c"), true) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/subscribe/address"
        }
        verify(mockSubscriptionDetailsService).cacheCustomsId(meq(Nino("AB123456C")))(any[HeaderCarrier])
      }
    }

    "determine the route for the user" when {

      "user is in review mode and UK journey" in {
        when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(IndividualSubscriptionFlow)
        when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))
        mockSubscriptionFlow(nextPageFlowUrl)
        submit(Journey.Subscribe, Map("nino" -> "ab 12 34 56 c"), true) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(
            LOCATION
          ) shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
        }
        verify(mockSubscriptionDetailsService).cacheCustomsId(meq(Nino("AB123456C")))(any[HeaderCarrier])
      }
    }
  }

  private def createForm(journey: Journey.Value)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(
      test(controller.createForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
    )
  }

  private def submit(journey: Journey.Value, form: Map[String, String], isInReviewMode: Boolean = false)(
    test: Future[Result] => Any
  ) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    await(
      test(
        controller.submit(isInReviewMode, atarService, journey).apply(
          SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form)
        )
      )
    )
  }

  private def mockSubscriptionFlow(url: String) = {
    when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]))
      .thenReturn(mockSubscriptionFlowInfo)
    when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
    when(mockSubscriptionPage.url(any())).thenReturn(url)
  }

}
