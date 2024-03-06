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

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthActionMock

trait SubscriptionFlowTestSupport extends ControllerSpec with AuthActionMock {

  protected def formId: String

  protected val nextPageUrl                = "next-page-url"
  protected val nextPage: SubscriptionPage = mock[SubscriptionPage]

  protected val subscriptionFlowStepInfo: SubscriptionFlowInfo =
    SubscriptionFlowInfo(stepNumber = 7, totalSteps = 10, nextPage = nextPage)

  protected val mockSubscriptionFlowManager: SubscriptionFlowManager         = mock[SubscriptionFlowManager]
  protected val mockAuthConnector: AuthConnector                             = mock[AuthConnector]
  protected val mockAuthAction: AuthAction                                   = authAction(mockAuthConnector)
  protected val mockSubscriptionBusinessService: SubscriptionBusinessService = mock[SubscriptionBusinessService]
  protected val mockSubscriptionDetailsService: SubscriptionDetailsService   = mock[SubscriptionDetailsService]

  def setupMockSubscriptionFlowManager(currentPage: SubscriptionPage): Unit = {
    when(nextPage.url(any[Service])).thenReturn(nextPageUrl)
    when(mockSubscriptionFlowManager.stepInformation(meq(currentPage))(any[Request[AnyContent]], any[HeaderCarrier]))
      .thenReturn(Right(subscriptionFlowStepInfo))
  }

}
