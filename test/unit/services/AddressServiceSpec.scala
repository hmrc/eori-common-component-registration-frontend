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

package unit.services

import common.pages.NinoMatchPage.mustBe
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import play.api.test.Helpers.{LOCATION, defaultAwaitTimeout, header, status}
import sttp.model.StatusCode.{InternalServerError, Ok}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{SubscriptionFlowInfo, SubscriptionPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.AddressDetailsForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{AddressService, SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{address, error_template}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.SessionBuilder
import util.builders.SubscriptionContactDetailsFormBuilder.contactDetailsModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressServiceSpec extends ControllerSpec with MockitoSugar with BeforeAndAfterEach with MatchingServiceTestData {

  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  private val mockSubscriptionPage = mock[SubscriptionPage]
  private val mockFlowError = mock[FlowError]
  private val mockAddress = inject[address]
  private val mockErrorTemplate = inject[error_template]
  private val messagesControllerComponents = inject[MessagesControllerComponents]
  private val mockAddressDetailsForm = mock[AddressDetailsForm]
  private val mockFormAddressViewModel = mock[Form[AddressViewModel]]

  private val subscriptionToTest = Seq(atarService, otherService, cdsService, eoriOnlyService)

  private val formMappings =
    Map("street" -> "street", "city" -> "city", "postcode" -> "SE28 1AA", "countryCode" -> "GB")

  val service = new AddressService(
    mockAddressDetailsForm,
    mockSubscriptionDetailsService,
    mockSubscriptionBusinessService,
    mockSubscriptionFlowManager,
    mockAddress,
    mockErrorTemplate,
    messagesControllerComponents
  )

  "populateViewIfContactDetailsCached" should {
    "Successfully populate view when details are cached" in {
      when(mockAddressDetailsForm.addressDetailsCreateForm()).thenCallRealMethod()

      subscriptionToTest.foreach { subscription =>
        when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
          Future.successful(Some(contactDetailsModel))
        )
        val result = service.populateViewIfContactDetailsCached(subscription)(
          SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
        )

        status(result) mustBe Ok.code
      }

    }
  }

  "Should 'successfully' display InternalServerError when data not found in cache" in subscriptionToTest.foreach { subscription =>
    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
      Future.successful(None)
    )
    val result = service.populateViewIfContactDetailsCached(subscription)(
      SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
    )

    status(result) mustBe InternalServerError.code
  }

  "handleFormDataAndRedirect" should {
    when(mockAddressDetailsForm.addressDetailsCreateForm()).thenCallRealMethod()

    "successfully redirect when correct input provided" in subscriptionToTest.foreach { subscription =>
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
        Future.successful(Some(contactDetailsModel))
      )
      val subscriptionFlowInfo = SubscriptionFlowInfo(1, 2, mockSubscriptionPage)

      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenReturn(Right(subscriptionFlowInfo))
      when(subscriptionFlowInfo.nextPage.url(any[Service])).thenReturn("/nextPage")

      val result = service.handleFormDataAndRedirect(mockAddressDetailsForm.addressDetailsCreateForm(), isInReviewMode = false, subscription)(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
      )

      header(LOCATION, result).value.endsWith("/nextPage")
    }

    "redirect to start page when subscriptionFlow unavailable" in subscriptionToTest.foreach { subscription =>
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(
        Future.successful(Some(contactDetailsModel))
      )

      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenReturn(Left(mockFlowError))

      val result = service.handleFormDataAndRedirect(mockAddressDetailsForm.addressDetailsCreateForm(), isInReviewMode = false, subscription)(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, formMappings)
      )

      header(LOCATION, result).value shouldBe s"/customs-registration-services/${subscription.code}/register"
    }

  }
}
