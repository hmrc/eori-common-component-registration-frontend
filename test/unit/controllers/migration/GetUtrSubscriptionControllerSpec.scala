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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.GetUtrSubscriptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  OrganisationSubscriptionFlow,
  RowIndividualFlow,
  RowOrganisationFlow,
  SubscriptionFlowInfo,
  SubscriptionPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CustomsId, NameOrganisationMatchModel, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_utr
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.OrganisationUtrFormBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetUtrSubscriptionControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlowInfo       = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage           = mock[SubscriptionPage]

  private val matchUtrSubscriptionView = instanceOf[how_can_we_identify_you_utr]

  val controller = new GetUtrSubscriptionController(
    mockAuthAction,
    mockRequestSessionData,
    mcc,
    matchUtrSubscriptionView,
    mockSubscriptionDetailsService
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsService.cachedCustomsId(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))
  }

  override protected def afterEach(): Unit = {
    reset(
      mockAuthConnector,
      mockRequestSessionData,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsService,
      mockSubscriptionFlowInfo,
      mockSubscriptionPage
    )

    super.afterEach()
  }

  "HaveUtrSubscriptionController createForm" should {
    "return OK and display correct page when orgType is Company" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include("Enter your Unique Tax Reference number")
      }
    }

    "return OK and display correct page when orgType is Sole Trader" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include("Enter your Unique Tax Reference number")
      }
    }

    "return OK and display correct page when orgType is Individual" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Individual))
      createForm(Journey.Subscribe) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include("Enter your Unique Tax Reference number")
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

    "return BadRequest when invalidUtr provided" in {
      val invalidUtr = "0123456789"
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      submit(Journey.Subscribe, ValidUtrRequest + ("utr" -> invalidUtr)) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "cache UTR and redirect to Address Page of the flow when rest of world and Company" in {

      val nameOrganisationMatchModel = NameOrganisationMatchModel("orgName")
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(nameOrganisationMatchModel)))
      when(
        mockSubscriptionDetailsService.cacheNameAndCustomsId(any[String], any[CustomsId])(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))
      submit(Journey.Subscribe, Map("utr" -> "11 11 111111k")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/address"
      }
      verify(mockSubscriptionDetailsService).cacheNameAndCustomsId(meq("orgName"), meq(Utr("1111111111K")))(
        any[HeaderCarrier]
      )
    }

    "cache UTR and redirect to Address Page of the flow when rest of world other than Company" in {

      val nameOrganisationMatchModel = NameOrganisationMatchModel("orgName")
      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
      when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(nameOrganisationMatchModel)))
      submit(Journey.Subscribe, ValidUtrRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/address"
      }
      verify(mockSubscriptionDetailsService).cacheCustomsId(meq(ValidUtr))(any[HeaderCarrier])
    }

    "throws an exception with the orgType is Company and No business name or CustomsId cached" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
      when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier])).thenReturn(Future.successful(None))
      intercept[IllegalStateException] {
        submit(Journey.Subscribe, ValidUtrRequest)(result => status(result))
      }.getMessage shouldBe "No business name cached"
    }

    "redirect to Address page" when {

      "user is in review mode and during ROW organisation journey" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
        when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))
        when(mockSubscriptionDetailsService.cacheNameAndCustomsId(any(), any())(any()))
          .thenReturn(Future.successful((): Unit))
        when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(RowOrganisationFlow)

        submit(Journey.Subscribe, ValidUtrRequest, true) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/address"
        }
        verify(mockSubscriptionDetailsService).cacheNameAndCustomsId(any(), meq(ValidUtr))(any[HeaderCarrier])
      }

      "user is in review mode and during ROW individual journey" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(SoleTrader))
        when(mockSubscriptionDetailsService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))
        when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(RowIndividualFlow)

        submit(Journey.Subscribe, ValidUtrRequest, true) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/address"
        }
        verify(mockSubscriptionDetailsService).cacheCustomsId(meq(ValidUtr))(any[HeaderCarrier])
      }
    }

    "determine the route for the user" when {

      "user is in review mode and UK journey" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]])).thenReturn(Some(Company))
        when(mockSubscriptionDetailsService.cachedNameDetails(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(NameOrganisationMatchModel("orgName"))))
        when(mockSubscriptionDetailsService.cacheNameAndCustomsId(any(), any())(any()))
          .thenReturn(Future.successful((): Unit))
        when(mockRequestSessionData.userSubscriptionFlow(any())).thenReturn(OrganisationSubscriptionFlow)

        submit(Journey.Subscribe, ValidUtrRequest, true) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers(
            LOCATION
          ) shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
        }
        verify(mockSubscriptionDetailsService).cacheNameAndCustomsId(any(), meq(ValidUtr))(any[HeaderCarrier])
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

}
