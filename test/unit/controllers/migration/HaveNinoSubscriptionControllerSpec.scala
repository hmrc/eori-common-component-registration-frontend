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

import common.pages.matching.SubscriptionNinoPage
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.HaveNinoSubscriptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NinoMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  RowIndividualFlow,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.match_nino_subscription
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HaveNinoSubscriptionControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlowInfo       = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage           = mock[SubscriptionPage]

  private val matchNinoSubscriptionView = instanceOf[match_nino_subscription]

  private val ValidNinoNoRequest = Map("have-nino" -> "false")

  private val nextPageFlowUrl = "/customs-enrolment-services/subscribe/address"

  val controller = new HaveNinoSubscriptionController(
    mockAuthAction,
    mockSubscriptionFlowManager,
    mockRequestSessionData,
    mcc,
    matchNinoSubscriptionView,
    mockSubscriptionDetailsService
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsService.cachedNinoMatch(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))
  }

  override protected def afterEach(): Unit = {
    reset(mockRequestSessionData, mockSubscriptionDetailsService)

    super.afterEach()
  }

  "HaveNinoSubscriptionController createForm" should {
    "return OK and display correct page" in {
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(SubscriptionNinoPage.title)
      }
    }
  }

  "HaveNinoSubscriptionController submit" should {
    "return BadRequest when no option selected" in {
      submit(Journey.Subscribe, Map.empty[String, String]) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return BadRequest when invalid option" in {
      submit(Journey.Subscribe, Map("have-nino" -> "yes")) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "cache NINO and redirect to Get Nino Page of the flow" in {
      when(mockSubscriptionDetailsService.cacheNinoMatch(any[Option[NinoMatchModel]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(RowIndividualFlow)
      mockSubscriptionFlow(nextPageFlowUrl)
      submit(Journey.Subscribe, Map("have-nino" -> "true")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/row-get-nino"
      }
      verify(mockSubscriptionDetailsService).cacheNinoMatch(meq(Some(NinoMatchModel(Some(true), None))))(
        any[HeaderCarrier]
      )
    }

    "cache None for CustomsId and redirect to Country page" in {
      when(
        mockSubscriptionDetailsService.cacheNinoMatchForNoAnswer(any[Option[NinoMatchModel]])(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))
      when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(RowIndividualFlow)
      submit(Journey.Subscribe, ValidNinoNoRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/row-country"
      }
      verify(mockSubscriptionDetailsService).cacheNinoMatchForNoAnswer(meq(Some(NinoMatchModel(Some(false), None))))(
        any[HeaderCarrier]
      )
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
