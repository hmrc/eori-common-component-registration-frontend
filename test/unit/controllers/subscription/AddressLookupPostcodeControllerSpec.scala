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

import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.AddressLookupPostcodeController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressLookupParams
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.address_lookup_postcode
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AddressLookupPostcodeControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector             = mock[AuthConnector]
  private val mockAuthAction                = authAction(mockAuthConnector)
  private val mockSessionCache              = mock[SessionCache]
  private val mockRequestSessionData        = mock[RequestSessionData]
  private val mockAddressLookupPostcodePage = mock[address_lookup_postcode]

  private val controller = new AddressLookupPostcodeController(
    mockAuthAction,
    mockSessionCache,
    mockRequestSessionData,
    mcc,
    mockAddressLookupPostcodePage
  )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockAddressLookupPostcodePage.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector, mockSessionCache, mockRequestSessionData, mockAddressLookupPostcodePage)

    super.afterEach()
  }

  private val addressLookupParams = AddressLookupParams("postcode", None)

  "Address Lookup Postcode Controller" should {

    "return 200 (OK)" when {

      "display page method is invoked with address lookup is in the cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.displayPage(atarService)(getRequest)

        val expectedForm = AddressLookupParams.form().fill(addressLookupParams)

        val formCaptor: ArgumentCaptor[Form[AddressLookupParams]] =
          ArgumentCaptor.forClass(classOf[Form[AddressLookupParams]])

        status(result) shouldBe OK
        verify(mockAddressLookupPostcodePage)
          .apply(formCaptor.capture(), ArgumentMatchers.eq(false), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe expectedForm.data
      }

      "display page method is invoked with address lookup is not in the cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.displayPage(atarService)(getRequest)

        val formCaptor: ArgumentCaptor[Form[AddressLookupParams]] =
          ArgumentCaptor.forClass(classOf[Form[AddressLookupParams]])

        status(result) shouldBe OK
        verify(mockAddressLookupPostcodePage)
          .apply(formCaptor.capture(), ArgumentMatchers.eq(false), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
      }

      "display review page method is invoked with address lookup is in the cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.reviewPage(atarService)(getRequest)

        val expectedForm = AddressLookupParams.form().fill(addressLookupParams)

        val formCaptor: ArgumentCaptor[Form[AddressLookupParams]] =
          ArgumentCaptor.forClass(classOf[Form[AddressLookupParams]])

        status(result) shouldBe OK
        verify(mockAddressLookupPostcodePage)
          .apply(formCaptor.capture(), ArgumentMatchers.eq(true), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe expectedForm.data
      }

      "display review page method is invoked with address lookup is not in the cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.reviewPage(atarService)(getRequest)

        val formCaptor: ArgumentCaptor[Form[AddressLookupParams]] =
          ArgumentCaptor.forClass(classOf[Form[AddressLookupParams]])

        status(result) shouldBe OK
        verify(mockAddressLookupPostcodePage)
          .apply(formCaptor.capture(), ArgumentMatchers.eq(true), any(), any())(any(), any())

        formCaptor.getValue.data shouldBe Map.empty
      }
    }

    "throw IllegalStateException" when {

      "user selected organisation type is not in cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(None)

        intercept[IllegalStateException] {
          await(controller.displayPage(atarService)(getRequest))
        }

        verifyZeroInteractions(mockAddressLookupPostcodePage)
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "form has incorrect values" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.submit(atarService, true)(postRequest("postcode" -> "incorrect"))

        status(result) shouldBe BAD_REQUEST
        verify(mockAddressLookupPostcodePage).apply(any(), ArgumentMatchers.eq(true), any(), any())(any(), any())
      }
    }

    "return 303 (SEE_OTHER) and redirect to results page" when {

      "form is correct and user is not in the review mode" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))
        when(mockSessionCache.saveAddressLookupParams(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, false)(postRequest("postcode" -> "AA11 1AA"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/results"
        verifyZeroInteractions(mockAddressLookupPostcodePage)
      }

      "form is correct and user is in the review mode" in {

        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))
        when(mockSessionCache.saveAddressLookupParams(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, true)(postRequest("postcode" -> "AA11 1AA"))

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/results/review"
        verifyZeroInteractions(mockAddressLookupPostcodePage)
      }
    }
  }
}
