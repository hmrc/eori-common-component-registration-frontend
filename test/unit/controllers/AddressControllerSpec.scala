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

package unit.controllers.address

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.Results.Status
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.AddressController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.AddressService
import unit.controllers.{
  SubscriptionFlowCreateModeTestSupport,
  SubscriptionFlowReviewModeTestSupport,
  SubscriptionFlowTestSupport
}
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder


import scala.concurrent.Future

class AddressControllerSpec
    extends SubscriptionFlowTestSupport with SubscriptionFlowCreateModeTestSupport
    with SubscriptionFlowReviewModeTestSupport {

  private val servicesToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val mockAddressService = mock[AddressService]

  private val controller = new AddressController(mockAuthAction, mockAddressService)

  protected override val formId: String = "addressDetailsForm"

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressController
      .submit(isInReviewMode = false, atarService)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.AddressController
      .submit(isInReviewMode = true, atarService)
      .url

  "createForm" should {
    "Display successfully" in servicesToTest.foreach { service =>
      when(mockAddressService.populateOkView(any(), any(), any())(any[Request[AnyContent]])).thenReturn(
        Future.successful(Status(OK))
      )
      createForm(service) { result =>
        status(result) shouldBe OK
      }
    }
  }

  "reviewForm" should {
    "Display successfully" in servicesToTest.foreach { service =>
      when(mockAddressService.populateViewIfContactDetailsCached(any())(any[Request[AnyContent]])).thenReturn(
        Future.successful(Status(SEE_OTHER))
      )
      reviewForm(service) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  "submitForm" should {
    "Submit successfully" in servicesToTest.foreach { service =>
      when(mockAddressService.handleFormDataAndRedirect(any(), any(), any())(any[Request[AnyContent]])).thenReturn(
        Future.successful(Status(SEE_OTHER))
      )
      submitForm(service) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  private def createForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.createForm(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def reviewForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.reviewForm(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      controller.submit(isInReviewMode = false, service).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    )
  }

}
