/*
 * Copyright 2025 HM Revenue & Customs
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

import common.pages.subscription.AddressPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Results.Status
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.ManualAddressController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ManualAddressController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.AddressService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.manual_address
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ManualAddressControllerSpec extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport {

  val defaultOrganisationType = "individual"
  val soleTraderType = "sole-trader"

  val form: Map[String, String] = Map("street" -> "1", "city" -> "Tel", "postcode" -> "SE12 1AA", "countryCode" -> "Uk")
  val invalidForm: Map[String, String] = Map("street" -> "1", "city" -> "Tel", "postcode" -> "SE1a")

  override protected val formId: String = AddressPage.formId

  def submitInCreateModeUrl: String =
    ManualAddressController.submit(atarService).url

  private val mockAddressService = mock[AddressService]
  private val mockSessionCache = mock[SessionCache]
  private val view = inject[manual_address]

  private val controller =
    new ManualAddressController(mockAuthAction, view, mcc, mockSessionCache)

  override def beforeEach(): Unit =
    super.beforeEach()

  override protected def afterEach(): Unit = {
    reset(mockAddressService)
    super.afterEach()
  }

  "Address Controller form in create mode" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "Display successfully" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockAddressService.populateViewIfContactDetailsCached(any())(any[Request[AnyContent]])).thenReturn(
        Future.successful(Status(OK))
      )
      showCreateForm(atarService) { result =>
        status(result) shouldBe OK
      }
    }
  }

  "Address Controller form" should {
    "Submit successfully" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)

      when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))
      when(mockAddressService.handleFormDataAndRedirect(any(), any(), any())(any[Request[AnyContent]])).thenReturn(
        Future.successful(Status(SEE_OTHER))
      )
      submitForm(form, atarService) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/customs-registration-services/atar/register/matching/confirm")
      }
    }
    "return 400 with invalid data" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)

      when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))
      when(mockAddressService.handleFormDataAndRedirect(any(), any(), any())(any[Request[AnyContent]])).thenReturn(
        Future.successful(Status(SEE_OTHER))
      )
      submitForm(invalidForm, atarService) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  private def showCreateForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.createForm(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String], service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.submit(service).apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form)))
  }

}
