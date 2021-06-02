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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.PdfGeneratorConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.Sub02Controller
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.xi_eori_guidance
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.AuthActionMock

import scala.concurrent.ExecutionContext.global

class Sub02ControllerRegisterExistingSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSessionCache               = mock[SessionCache]
  private val mockCdsSubscriber              = mock[CdsSubscriber]
  private val mockPdfGeneratorService        = mock[PdfGeneratorConnector]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  private val sub01OutcomeView                = instanceOf[sub01_outcome_processing]
  private val sub02RequestNotProcessed        = instanceOf[sub02_request_not_processed]
  private val sub02SubscriptionInProgressView = instanceOf[sub02_subscription_in_progress]
  private val sub02EoriAlreadyAssociatedView  = instanceOf[sub02_eori_already_associated]
  private val sub02EoriAlreadyExists          = instanceOf[sub02_eori_already_exists]
  private val sub01OutcomeRejected            = instanceOf[sub01_outcome_rejected]
  private val subscriptionOutcomeView         = instanceOf[subscription_outcome]
  private val xiEoriGuidanceView              = mock[xi_eori_guidance]

  private val subscriptionController = new Sub02Controller(
    mockAuthAction,
    mockRequestSessionData,
    mockSessionCache,
    mockSubscriptionDetailsService,
    mcc,
    sub01OutcomeView,
    sub02RequestNotProcessed,
    sub02SubscriptionInProgressView,
    sub02EoriAlreadyAssociatedView,
    sub02EoriAlreadyExists,
    sub01OutcomeRejected,
    subscriptionOutcomeView,
    xiEoriGuidanceView,
    mockCdsSubscriber
  )(global)

  val eoriNumberResponse: String     = "EORI-Number"
  val formBundleIdResponse: String   = "Form-Bundle-Id"
  val processingDateResponse: String = "19 April 2018"

  val statusReceived = "Received"
  val statusReview   = "Review"
  val statusDecision = "Decision"

  val emulatedFailure = new UnsupportedOperationException("Emulated service call failure.")

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(xiEoriGuidanceView.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, mockCdsSubscriber, mockPdfGeneratorService, mockSessionCache, xiEoriGuidanceView)

    super.afterEach()
  }

  "SUB02Controller" should {

    "display xi eori guidance" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result = subscriptionController.xiEoriGuidance()(getRequest)

      status(result) shouldBe OK
      verify(xiEoriGuidanceView).apply()(any(), any())
    }
  }
}
