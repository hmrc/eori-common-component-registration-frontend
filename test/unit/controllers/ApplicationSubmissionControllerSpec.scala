/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ApplicationSubmissionController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.application_processing
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationSubmissionControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockSessionCache = mock[SessionCache]
  private val applicationProcessingView = inject[application_processing]

  private val controller =
    new ApplicationSubmissionController(mockAuthAction, mcc, mockSessionCache, applicationProcessingView)

  "Controller" should {
    "use correct data & redirect" in {
      // Given
      when(mockSessionCache.subscriptionDetails(any())).thenReturn(Future.successful(givenSubscriptionDetails))
      when(mockSessionCache.txe13ProcessingDate(any())).thenReturn(Future.successful("2025-02-14T14:18:34"))

      // When
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      val futureResult =
        controller.processing(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      // Then
      status(futureResult) shouldBe OK

      val page: CdsPage = CdsPage(contentAsString(futureResult))
      page.title() should startWith("Application sent")
    }
  }

  private def givenSubscriptionDetails = {
    SubscriptionDetails(
      embassyName = Some("Embassy Of Japan"),
      contactDetails = Some(
        ContactDetailsModel(
          "Masahiro Moro",
          "masahiro.moro@gmail.com",
          "07806674501",
          None,
          useAddressFromRegistrationDetails = true,
          None,
          None,
          None,
          Some("Gb")
        )
      )
    )

  }

}
