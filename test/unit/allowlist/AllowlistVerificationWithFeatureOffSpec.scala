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

package unit.allowlist

import common.pages.migration.NameDobSoleTraderPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.NameDobSoleTraderController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.NameDobDetailsSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, NameDobMatchModel, RegistrationDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.enter_your_details
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.subscription.SubscriptionFlowSpec
import util.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AllowlistVerificationWithFeatureOffSpec
    extends SubscriptionFlowSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach {

  protected override val mockSubscriptionFlowManager: SubscriptionFlowManager = mock[SubscriptionFlowManager]
  protected override val formId: String                                       = NameDobSoleTraderPage.formId

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameDobSoleTraderController
      .submit(isInReviewMode = false, atarService, Journey.Subscribe)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameDobSoleTraderController
      .submit(isInReviewMode = true, atarService, Journey.Subscribe)
      .url

  private val mockRequestSessionData   = mock[RequestSessionData]
  private val mockRegistrationDetails  = mock[RegistrationDetails](RETURNS_DEEP_STUBS)
  private val mockCdsFrontendDataCache = mock[SessionCache]
  private val enterYourDetails         = instanceOf[enter_your_details]

  private val controller = new NameDobSoleTraderController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockRequestSessionData,
    mockCdsFrontendDataCache,
    mockSubscriptionFlowManager,
    mcc,
    enterYourDetails,
    mockSubscriptionDetailsHolderService
  )

  override def beforeEach: Unit = {
    reset(
      mockSubscriptionBusinessService,
      mockCdsFrontendDataCache,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsHolderService
    )
    when(mockSubscriptionBusinessService.cachedSubscriptionNameDobViewModel(any[HeaderCarrier])).thenReturn(None)
    when(mockSubscriptionBusinessService.getCachedSubscriptionNameDobViewModel(any[HeaderCarrier]))
      .thenReturn(Future.successful(NameDobSoleTraderPage.filledValues))
    when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.SoleTrader))
    when(mockSubscriptionDetailsHolderService.cacheNameDobDetails(any[NameDobMatchModel])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(mockRegistrationDetails)
    setupMockSubscriptionFlowManager(NameDobDetailsSubscriptionFlowPage)
  }

  "Allowlist verification" should {

    "return OK (200) when a non-allowlisted user attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector, userEmail = Some("not@example.com"))

      val result = controller.createForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )

      status(result) shouldBe OK
    }

    "return OK (200) when a allowlisted user attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("mister_allow@example.com"))

      val result = controller.createForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )

      status(result) shouldBe OK
    }

    "return OK (200) when a user with no email address attempts to access a route and the feature is OFF" in {
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = None)

      val result = controller.createForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSession(defaultUserId)
      )

      status(result) shouldBe OK
    }
  }
}
