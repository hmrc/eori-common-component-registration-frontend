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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.AddressLookupErrorController
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressLookupParams
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{
  address_lookup_error,
  address_lookup_no_results
}
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AddressLookupErrorControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSessionCache               = mock[SessionCache]
  private val mockAddressLookupErrorPage     = mock[address_lookup_error]
  private val mockAddressLookupNoResultsPage = mock[address_lookup_no_results]

  private val controller = new AddressLookupErrorController(
    mockAuthAction,
    mockSessionCache,
    mcc,
    mockAddressLookupErrorPage,
    mockAddressLookupNoResultsPage
  )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockAddressLookupErrorPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(mockAddressLookupNoResultsPage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, mockSessionCache, mockAddressLookupErrorPage, mockAddressLookupNoResultsPage)

    super.afterEach()
  }

  "Address Lookup Error Controller" should {

    "return 200 (OK)" when {

      "displayErrorPage method is invoked" in {

        val result = controller.displayErrorPage(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockAddressLookupErrorPage).apply(any(), ArgumentMatchers.eq(false))(any(), any())
      }

      "reviewErrorPage method is invoked" in {

        val result = controller.reviewErrorPage(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockAddressLookupErrorPage).apply(any(), ArgumentMatchers.eq(true))(any(), any())
      }

      "displayNoResultsPage method is invoked and address lookup params are in cache" in {

        when(mockSessionCache.addressLookupParams(any()))
          .thenReturn(Future.successful(Some(AddressLookupParams("postcode", None))))

        val result = controller.displayNoResultsPage(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockAddressLookupNoResultsPage).apply(any(), any(), ArgumentMatchers.eq(false))(any(), any())
      }

      "reviewNoResultsPage method is invoked and address lookup params are in cache" in {

        when(mockSessionCache.addressLookupParams(any()))
          .thenReturn(Future.successful(Some(AddressLookupParams("postcode", None))))

        val result = controller.reviewNoResultsPage(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockAddressLookupNoResultsPage).apply(any(), any(), ArgumentMatchers.eq(true))(any(), any())
      }
    }

    "return 303 (SEE_OTHER)" when {

      "displayNoResultsPage method is invoked and address lookup params are not in the cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))

        val result = controller.displayNoResultsPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode"
        verifyZeroInteractions(mockAddressLookupNoResultsPage)
      }

      "reviewNoResultsPage method is invoked and address lookup params are not in the cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))

        val result = controller.reviewNoResultsPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/review"
        verifyZeroInteractions(mockAddressLookupNoResultsPage)
      }
    }
  }
}
