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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Results.Status
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ConfirmContactDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.YesNoWrongAddress.wrongAddress
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.sub01_outcome_processing
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.YesNoFormBuilder.validRequest
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmContactDetailsControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                = mock[AuthConnector]
  private val mockAuthAction                   = authAction(mockAuthConnector)
  private val mockConfirmContactDetailsService = mock[ConfirmContactDetailsService]
  private val mockSessionCache                 = mock[SessionCache]
  private val sub01OutcomeProcessingView       = instanceOf[sub01_outcome_processing]

  private val controller = new ConfirmContactDetailsController(
    mockAuthAction,
    mockConfirmContactDetailsService,
    mockSessionCache,
    mcc,
    sub01OutcomeProcessingView
  )

  private val mockSub01Outcome = mock[Sub01Outcome]
  private val mockRegDetails   = mock[RegistrationDetails]

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  "form" should {

    withAuthorisedUser(defaultUserId, mockAuthConnector)

    "Correctly populate form" in servicesToTest.foreach { testService =>
      when(
        mockConfirmContactDetailsService.handleAddressAndPopulateView(any(), any())(any[Request[AnyContent]], any())
      ).thenReturn(Future.successful(Status(OK)))

      val result = controller.form(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      status(result) shouldBe OK
    }
  }

  "submit" should {

    withAuthorisedUser(defaultUserId, mockAuthConnector)

    "Correctly handle a form with errors" in servicesToTest.foreach { testService =>
      when(
        mockConfirmContactDetailsService.handleFormWithErrors(any(), any(), any())(any[Request[AnyContent]], any())
      ).thenReturn(Future.successful(Status(OK)))

      val result = controller.submit(testService).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, validRequest + ("wrong-address" -> ""))
      )

      status(result) shouldBe OK

    }

    "Correctly handle a valid form" in servicesToTest.foreach { testService =>
      when(
        mockConfirmContactDetailsService.checkAddressDetails(any(), any(), any())(any[Request[AnyContent]], any())
      ).thenReturn(Future.successful(Status(OK)))

      val result = controller.submit(testService).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(
          defaultUserId,
          validRequest + ("yes-no-wrong-address" -> wrongAddress)
        )
      )

      status(result) shouldBe OK

    }
  }

  "processing" should {

    "Correctly redirect to sub01OutcomeProcessingView" in servicesToTest.foreach { testService =>
      when(mockSessionCache.registrationDetails(any[Request[AnyContent]])).thenReturn(Future.successful(mockRegDetails))
      when(mockSessionCache.sub01Outcome(any[Request[AnyContent]])).thenReturn(Future.successful(mockSub01Outcome))

      val result =
        controller.processing(testService).apply(SessionBuilder.buildRequestWithSession(defaultUserId))

      status(result) shouldBe OK
    }
  }

}
