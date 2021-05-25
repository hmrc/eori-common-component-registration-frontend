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

import common.pages.subscription.ShortNamePage
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SignInWithDifferentDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.sign_in_with_different_details
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignInWithDifferentDetailsControllerSpec
    extends ControllerSpec with BeforeAndAfterEach with SubscriptionFlowReviewModeTestSupport {

  protected override val formId: String                = ShortNamePage.formId
  protected override val submitInReviewModeUrl: String = ""

  private val mockCdsFrontendDataCache       = mock[SessionCache]
  private val mockRegistrationDetails        = mock[RegistrationDetails](RETURNS_DEEP_STUBS)
  private val signInWithDifferentDetailsView = instanceOf[sign_in_with_different_details]
  when(mockRegistrationDetails.name).thenReturn("Test Org Name")

  private val controller = new SignInWithDifferentDetailsController(
    mockAuthAction,
    mockCdsFrontendDataCache,
    signInWithDifferentDetailsView,
    mcc
  )

  override def beforeEach: Unit = {
    reset(mockCdsFrontendDataCache)
    mockFunctionWithRegistrationDetails(mockRegistrationDetails)
  }

  "Displaying the form in create mode" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(atarService, Journey.Register)
    )

    "display para1 as 'Test Org Name has already registered for Advance Tariff Rulings with a different Government Gateway.'" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          "//*[@id='para1']"
        ) shouldBe "Test Org Name has already registered for Advance Tariff Rulings with a different Government Gateway."
      }
    }
  }

  private def showCreateForm(userId: String = defaultUserId, journey: Journey.Value = Journey.Register)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller.form(atarService, journey).apply(
        SessionBuilder.buildRequestWithSessionAndPath("/atar/subscribe", userId)
      )
    )
  }

  private def mockFunctionWithRegistrationDetails(registrationDetails: RegistrationDetails) {
    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(registrationDetails)
  }

}
