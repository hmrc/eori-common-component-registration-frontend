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

import common.pages.matching.OrganisationNamePage
import common.pages.subscription.EnterYourBusinessAddress
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.{Assertion, BeforeAndAfterEach}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.NameOrgController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  NameIdOrganisationMatchModel,
  NameOrganisationMatchModel,
  RegistrationDetailsOrganisation
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.nameOrg
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NameOrgControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector               = mock[AuthConnector]
  private val mockAuthAction                  = authAction(mockAuthConnector)
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSessionCache                = mock[SessionCache]
  private val mockSubscriptionFlowManager     = mock[SubscriptionFlowManager]
  private val mockSubscriptionDetailsService  = mock[SubscriptionDetailsService]
  private val mockSubscriptionFlowInfo        = mock[SubscriptionFlowInfo]
  private val mockSubscriptionPage            = mock[SubscriptionPage]

  private val nameOrgView = instanceOf[nameOrg]

  private val registrationDetails          = RegistrationDetailsOrganisation()
  private val nameOrgMatchModel            = NameOrganisationMatchModel("testName")
  private val nameIdOrganisationMatchModel = NameIdOrganisationMatchModel("testName", "GB1234567")
  private val incorrectForm                = Map("name" -> "")
  private val correcctForm                 = Map("name" -> "testName")

  val nameOrgController = new NameOrgController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSessionCache,
    mockSubscriptionFlowManager,
    mcc,
    nameOrgView,
    mockSubscriptionDetailsService
  )

  override def beforeEach(): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    reset(mockSubscriptionBusinessService, mockSessionCache, mockSubscriptionDetailsService)
  }

  "NameOrgController" should {
    "return ok for createForm" in {
      when(mockSubscriptionBusinessService.cachedNameOrganisationViewModel(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(registrationDetails)

      createForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(OrganisationNamePage.title)
      }
    }

    "return ok for reviewForm" in {
      when(mockSubscriptionBusinessService.getCachedNameViewModel(any[HeaderCarrier])).thenReturn(nameOrgMatchModel)
      when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(registrationDetails)

      reviewForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title should include(OrganisationNamePage.title)
      }
    }

    "return bad request for form with errors " in {
      when(mockSessionCache.registrationDetails(any[HeaderCarrier])).thenReturn(registrationDetails)

      submit(false, incorrectForm) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.title should include(OrganisationNamePage.title)
      }
    }

    "return see other and redirect to address page when not in review mode" in {
      mockSubscriptionFlowNextPage()
      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
      when(mockSubscriptionDetailsService.cachedNameIdDetails(any[HeaderCarrier])).thenReturn(Future.successful(None))

      submit(false, correcctForm) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/subscribe/address"
      }
    }

    "return see other and redirect to when in review mode" in {
      when(mockSubscriptionDetailsService.cacheNameDetails(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
      when(mockSubscriptionDetailsService.cachedNameIdDetails(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(nameIdOrganisationMatchModel)))
      when(
        mockSubscriptionDetailsService.cacheNameIdDetails(
          meq(NameIdOrganisationMatchModel(correcctForm("name"), nameIdOrganisationMatchModel.id))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      submit(true, correcctForm) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
      }
    }
  }

  private def createForm()(test: Future[Result] => Assertion) =
    test(
      nameOrgController.createForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )
    )

  private def reviewForm()(test: Future[Result] => Assertion) =
    test(
      nameOrgController.reviewForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )
    )

  private def submit(isInReviewMode: Boolean, form: Map[String, String])(test: Future[Result] => Assertion) =
    test(
      nameOrgController
        .submit(isInReviewMode, atarService, Journey.Subscribe)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form))
    )

  private def mockSubscriptionFlowNextPage() = {
    when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]]))
      .thenReturn(mockSubscriptionFlowInfo)
    when(mockSubscriptionFlowInfo.nextPage).thenReturn(mockSubscriptionPage)
    when(mockSubscriptionPage.url(atarService)).thenReturn(EnterYourBusinessAddress.url)
  }

}
