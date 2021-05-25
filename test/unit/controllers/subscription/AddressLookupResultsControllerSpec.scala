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
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{
  AddressLookupResultsController,
  SubscriptionFlowManager
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  ReviewDetailsPageSubscription,
  SubscriptionFlowInfo
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressLookupParams
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{
  AddressLookup,
  AddressLookupFailure,
  AddressLookupSuccess
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.address_lookup_results
import util.ControllerSpec
import util.builders.AuthActionMock
import util.builders.AuthBuilder.withAuthorisedUser

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AddressLookupResultsControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSessionCache               = mock[SessionCache]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSubscriptionFlowManager    = mock[SubscriptionFlowManager]
  private val mockAddressLookupConnector     = mock[AddressLookupConnector]
  private val mockAddressLookupResultsPage   = mock[address_lookup_results]

  private val controller = new AddressLookupResultsController(
    mockAuthAction,
    mockSessionCache,
    mockSubscriptionDetailsService,
    mockRequestSessionData,
    mockSubscriptionFlowManager,
    mockAddressLookupConnector,
    mcc,
    mockAddressLookupResultsPage
  )(global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockAddressLookupResultsPage.apply(any(), any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(
      mockAuthConnector,
      mockSessionCache,
      mockSubscriptionDetailsService,
      mockRequestSessionData,
      mockSubscriptionFlowManager,
      mockAddressLookupConnector,
      mockAddressLookupResultsPage
    )

    super.afterEach()
  }

  private val addressLookupParams = AddressLookupParams("postcode", None)

  private val addressLookup = AddressLookup("line1", "city", "postcode", "GB")

  "Address Lookup Results Controller" should {

    "return 200 (OK)" when {

      "display page method is invoked, address lookup params are in cache and connector returns addresses" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockAddressLookupResultsPage)
          .apply(any(), any(), any(), ArgumentMatchers.eq(false), any(), any())(any(), any())
      }

      "display review page method is invoked, address lookup params are in cache and connector returns addresses" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe OK
        verify(mockAddressLookupResultsPage)
          .apply(any(), any(), any(), ArgumentMatchers.eq(true), any(), any())(any(), any())
      }

      "display page with results" when {

        "there is no results for postcode and line1, but there are results for just postcode" in {

          when(mockSessionCache.addressLookupParams(any()))
            .thenReturn(Future.successful(Some(AddressLookupParams("postcode", Some("line1")))))
          when(mockAddressLookupConnector.lookup(meq("postcode"), meq(Some("line1")))(any()))
            .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty)))
          when(mockAddressLookupConnector.lookup(meq("postcode"), meq(None))(any()))
            .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
          when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))
          when(mockSessionCache.saveAddressLookupParams(any())(any())).thenReturn(Future.successful((): Unit))

          val result = controller.displayPage(atarService)(getRequest)

          status(result) shouldBe OK

          verify(mockAddressLookupConnector).lookup(meq("postcode"), meq(Some("line1")))(any())
          verify(mockAddressLookupConnector).lookup(meq("postcode"), meq(None))(any())
          verify(mockAddressLookupResultsPage)
            .apply(any(), any(), meq(Seq(addressLookup)), meq(false), any(), any())(any(), any())
        }
      }
    }

    "throw an exception" when {

      "user selected organisation type is not in cache" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(None)

        intercept[IllegalStateException] {
          await(controller.displayPage(atarService)(getRequest))
        }

        verifyZeroInteractions(mockAddressLookupResultsPage)
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "address has not been chosen" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

        val result = controller.submit(atarService, false)(postRequest("address" -> ""))

        status(result) shouldBe BAD_REQUEST
        verify(mockAddressLookupResultsPage)
          .apply(any(), any(), any(), ArgumentMatchers.eq(false), any(), any())(any(), any())
      }
    }

    "return 303 (SEE_OTHER) and redirect to no results page" when {

      "user is not in review mode and connector doesn't return any addresses for display page method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty[AddressLookup])))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"

        verify(mockAddressLookupConnector).lookup(any(), any())(any())
      }

      "user is not in review mode and connector doesn't return any addresses for display page method - line1 and postcode presented" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(
          Future.successful(Some(AddressLookupParams("postcode", Some("line1"))))
        )
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty[AddressLookup])))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"

        verify(mockAddressLookupConnector, times(2)).lookup(any(), any())(any())
      }

      "user is in review mode and connector doesn't return any addresses for display page method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty[AddressLookup])))

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results/review"
      }

      "user is in review mode and connector doesn't return any addresses for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty[AddressLookup])))

        val result = controller.submit(atarService, true)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results/review"
      }

      "user is not in review mode and connector doesn't return any addresses for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty[AddressLookup])))

        val result = controller.submit(atarService, false)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"
      }

      "address that come back doesn't have line 1 for display method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(AddressLookup("", "city", "postcode", "GB")))))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"

        verify(mockAddressLookupConnector).lookup(any(), any())(any())
      }

      "address that come back doesn't have line 1 for review method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(AddressLookup("", "city", "postcode", "GB")))))

        val result = controller.submit(atarService, false)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"

        verify(mockAddressLookupConnector).lookup(any(), any())(any())
      }

      "address that come back doesn't have line 1 for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(AddressLookup("", "city", "postcode", "GB")))))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"

        verify(mockAddressLookupConnector).lookup(any(), any())(any())
      }

      "address that come back doesn't have line 1 for second call during display method" in {

        when(mockSessionCache.addressLookupParams(any()))
          .thenReturn(Future.successful(Some(AddressLookupParams("postcode", Some("line1")))))
        when(mockAddressLookupConnector.lookup(meq("postcode"), meq(Some("line1")))(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty)))
        when(mockAddressLookupConnector.lookup(meq("postcode"), meq(None))(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(AddressLookup("", "city", "postcode", "GB")))))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results"
      }

      "address that come back doesn't have line 1 for second call during review method" in {

        when(mockSessionCache.addressLookupParams(any()))
          .thenReturn(Future.successful(Some(AddressLookupParams("postcode", Some("line1")))))
        when(mockAddressLookupConnector.lookup(meq("postcode"), meq(Some("line1")))(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty)))
        when(mockAddressLookupConnector.lookup(meq("postcode"), meq(None))(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(AddressLookup("", "city", "postcode", "GB")))))

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/no-results/review"
      }
    }

    "return 303 (SEE_OTHER) and redirect to error page" when {

      "user is in review mode and connector request fail for display page method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupFailure))

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/unavailable/review"
      }

      "user is in review mode and connector request fail for the second call for display page method" in {

        when(mockSessionCache.addressLookupParams(any()))
          .thenReturn(Future.successful(Some(AddressLookupParams("postcode", Some("line1")))))
        when(mockAddressLookupConnector.lookup(meq("postcode"), meq(Some("line1")))(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq.empty)))
        when(mockAddressLookupConnector.lookup(meq("postcode"), meq(None))(any()))
          .thenReturn(Future.successful(AddressLookupFailure))

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/unavailable/review"

        verify(mockAddressLookupConnector, times(2)).lookup(any(), any())(any())
      }

      "user is not in review mode and connector request fail for display page method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupFailure))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/unavailable"
      }

      "user is in review mode and connector request fail for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupFailure))

        val result = controller.submit(atarService, true)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(
          result
        ).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/unavailable/review"
      }

      "user is not in review mode and connector request fail for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any())).thenReturn(Future.successful(AddressLookupFailure))

        val result = controller.submit(atarService, false)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/unavailable"
      }
    }

    "return 303 (SEE_OTHER) and redirect to postcode page" when {

      "user is in review mode and cache doesn't contain address lookup params for display page method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))

        val result = controller.reviewPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/review"
      }

      "user is not in review mode and cache doesn't contain address lookup params for display page method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))

        val result = controller.displayPage(atarService)(getRequest)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode"
      }

      "user is in review mode and cache doesn't contain address lookup params for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))

        val result = controller.submit(atarService, true)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode/review"
      }

      "user is not in review mode and cache doesn't contain address lookup params for submit method" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(None))

        val result = controller.submit(atarService, false)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode"
      }
    }

    "return 303 (SEE_OTHER) and redirect to next page" when {

      "user is in review mode and provide correct address" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.successful((): Unit))

        val result = controller.submit(atarService, true)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
        verifyZeroInteractions(mockAddressLookupResultsPage)
      }

      "user is not in review mode and provide correct address" in {

        when(mockSessionCache.addressLookupParams(any())).thenReturn(Future.successful(Some(addressLookupParams)))
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))
        when(mockSubscriptionDetailsService.cacheAddressDetails(any())(any())).thenReturn(Future.successful((): Unit))
        when(mockSubscriptionFlowManager.stepInformation(any())(any())).thenReturn(
          SubscriptionFlowInfo(0, 0, ReviewDetailsPageSubscription)
        )

        val result = controller.submit(atarService, false)(postRequest("address" -> addressLookup.dropDownView))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
        verifyZeroInteractions(mockAddressLookupResultsPage)
      }
    }
  }
}
