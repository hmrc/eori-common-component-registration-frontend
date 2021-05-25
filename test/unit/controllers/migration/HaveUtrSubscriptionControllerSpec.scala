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

import common.pages.matching.{SubscriptionRowCompanyUtr, SubscriptionRowIndividualsUtr}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.HaveUtrSubscriptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  RowOrganisationFlow,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{NameOrganisationMatchModel, UtrMatchModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.match_utr_subscription
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.OrganisationUtrFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HaveUtrSubscriptionControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlowInfo       = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage           = mock[SubscriptionPage]

  private val matchUtrSubscriptionView = instanceOf[match_utr_subscription]

  private val nextPageFlowUrl = "/customs-enrolment-services/subscribe/row-nino"

  val controller = new HaveUtrSubscriptionController(
    mockAuthAction,
    mockRequestSessionData,
    mockSubscriptionFlowManager,
    mcc,
    matchUtrSubscriptionView,
    mockSubscriptionDetailsService
  )

  override def beforeEach: Unit = {
    super.beforeEach()
    reset(
      mockAuthConnector,
      mockRequestSessionData,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsService,
      mockSubscriptionFlowInfo,
      mockSubscriptionPage
    )
    when(mockSubscriptionDetailsService.cachedUtrMatch(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))
  }

  "HaveUtrSubscriptionController createForm" should {
    "return OK and display correct page when orgType is Company" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(SubscriptionRowCompanyUtr.title)
      }
    }

    "return OK and display correct page when orgType is Sole Trader" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(SubscriptionRowIndividualsUtr.title)

      }
    }

    "return OK and display correct page when orgType is Individual" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Individual))
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(SubscriptionRowIndividualsUtr.title)
      }
    }

    "throws an exception if orgType is not found" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      intercept[IllegalStateException] {
        createForm(Journey.Subscribe)(result => status(result))
      }.getMessage shouldBe "No organisation type selected by user"
    }
  }

  "HaveUtrSubscriptionController Submit" should {
    "throws an exception if orgType is not found" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(None)
      intercept[IllegalStateException] {
        submit(Journey.Subscribe, ValidUtrRequest)(result => status(result))
      }.getMessage shouldBe "No organisation type selected by user"
    }

    "return BadRequest when no option selected" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      submit(Journey.Subscribe, Map.empty[String, String]) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return BadRequest when invalid option provided" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      submit(Journey.Subscribe, Map("have-utr" -> "non")) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "cache UTR and redirect to Get Utr Page of the flow when rest of world and Company" in {
      reset(mockSubscriptionDetailsService)
      val nameOrganisationMatchModel = NameOrganisationMatchModel("orgName")
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(nameOrganisationMatchModel)))
      when(mockSubscriptionDetailsService.cacheUtrMatch(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
      submit(Journey.Subscribe, Map("have-utr" -> "true", "utr" -> "11 11 111111k")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/row-get-utr"
      }
      verify(mockSubscriptionDetailsService, times(1)).cacheUtrMatch(meq(Some(UtrMatchModel(Some(true), None))))(
        any[HeaderCarrier]
      )
    }

    "cache UTR and redirect to Get Utr Page of the flow when rest of world other than Company" in {
      reset(mockSubscriptionDetailsService)
      val nameOrganisationMatchModel = NameOrganisationMatchModel("orgName")
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
      when(mockSubscriptionDetailsService.cacheUtrMatch(any[Option[UtrMatchModel]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(nameOrganisationMatchModel)))
      submit(Journey.Subscribe, ValidUtrRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/row-get-utr"
      }
      verify(mockSubscriptionDetailsService, times(1)).cacheUtrMatch(meq(Some(UtrMatchModel(Some(true), None))))(
        any[HeaderCarrier]
      )
    }

    "redirect to next page in the flow when 'No' UTR selected" in {
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
      when(mockSubscriptionDetailsService.cacheUtrMatchForNoAnswer(any[Option[UtrMatchModel]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      mockSubscriptionFlow(nextPageFlowUrl)
      submit(Journey.Subscribe, NoUtrRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe nextPageFlowUrl
      }
    }

    "redirect to Company Registered Country page" when {

      "'No' UTR selected and user is during Row organisation journey" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
        when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(RowOrganisationFlow)
        when(mockSubscriptionDetailsService.cacheUtrMatchForNoAnswer(any[Option[UtrMatchModel]])(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))
        submit(Journey.Subscribe, NoUtrRequest) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/row-country"
        }
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
    when(mockSubscriptionPage.url(atarService)).thenReturn(url)
  }

}
