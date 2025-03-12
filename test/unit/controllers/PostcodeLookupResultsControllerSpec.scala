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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.AddressLookupConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{PostcodeLookupResultsController, routes}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.PostcodeLookupResultsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{AddressLookupFailure, AddressLookupSuccess}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegistrationDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.postcode_address_result
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostcodeLookupResultsControllerSpec extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport {

  val defaultOrganisationType = "individual"
  val soleTraderType          = "sole-trader"

  override protected val formId: String = "addressDetailsForm"

  private val addressLookup = Address("addressLine 1", None, None, Some("city"), Some("TF3 2BX"), "GB")

  val form: Map[String, String]        = Map("address" -> addressLookup.dropDownView)
  val invalidForm: Map[String, String] = Map()

  def submitInCreateModeUrl: String =
    PostcodeLookupResultsController.submit(atarService).url

  private val mockSessionCache               = mock[SessionCache]
  private val mockAddressLookupConnector     = mock[AddressLookupConnector]
  private val mockRegistrationDetailsService = mock[RegistrationDetailsService]
  private val view                           = mock[postcode_address_result]

  private val controller =
    new PostcodeLookupResultsController(
      mockAuthAction,
      mockSessionCache,
      mockRegistrationDetailsService,
      mockAddressLookupConnector,
      mcc,
      view
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(view.apply(any(), any(), any(), any())(any(), any()))
      .thenReturn(HtmlFormat.empty)
    when(mockRegistrationDetailsService.cacheAddress(any())(any()))
      .thenReturn(Future.successful(true))
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  "Address Lookup Postcode Controller" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.displayPage(atarService))

    "return 200 (OK)" when {

      "display successfully" in {

        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("TF3 2BX", Some("addressLine 1"))))
        )
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))

        showCreateForm(atarService) { result =>
          status(result) shouldBe OK
        }
      }
      "display successfully when no address is returned and try without address line 1" in {

        val postcode = "TF3 2BX"
        when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))

        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel(postcode, Some("addressLine 1"))))
        )
        when(
          mockAddressLookupConnector.lookup(
            ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
            ArgumentMatchers.eq(Some("addressLine 1"))
          )(any())
        )
          .thenReturn(Future.successful(AddressLookupSuccess(Seq())))

        when(
          mockAddressLookupConnector.lookup(
            ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
            ArgumentMatchers.eq(None)
          )(any())
        )
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))

        showCreateForm(atarService) { result =>
          status(result) shouldBe OK
        }
      }

      "redirect to manual address page" when {

        "no address is returned even with retry without address line 1" in {

          val postcode = "TF3 2BX"
          when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))

          when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
            Future.successful(Some(PostcodeViewModel(postcode, Some("addressLine 1"))))
          )
          when(
            mockAddressLookupConnector.lookup(
              ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
              ArgumentMatchers.eq(Some("addressLine 1"))
            )(any())
          )
            .thenReturn(Future.successful(AddressLookupSuccess(Seq())))

          when(
            mockAddressLookupConnector.lookup(
              ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
              ArgumentMatchers.eq(None)
            )(any())
          )
            .thenReturn(Future.successful(AddressLookupSuccess(Seq())))

          showCreateForm(atarService) { result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.ManualAddressController.createForm(atarService).url)
          }
        }

        "no address is returned and retry without address line 1 returns an error" in {

          val postcode = "TF3 2BX"
          when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))

          when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
            Future.successful(Some(PostcodeViewModel(postcode, Some("addressLine 1"))))
          )
          when(
            mockAddressLookupConnector.lookup(
              ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
              ArgumentMatchers.eq(Some("addressLine 1"))
            )(any())
          )
            .thenReturn(Future.successful(AddressLookupSuccess(Seq())))

          when(
            mockAddressLookupConnector.lookup(
              ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
              ArgumentMatchers.eq(None)
            )(any())
          )
            .thenReturn(Future.successful(AddressLookupFailure))

          showCreateForm(atarService) { result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.ManualAddressController.createForm(atarService).url)
          }
        }

        "no address is returned back" in {

          val postcode = "TF3 2BX"
          when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))

          when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
            Future.successful(Some(PostcodeViewModel(postcode, None)))
          )
          when(
            mockAddressLookupConnector.lookup(
              ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
              ArgumentMatchers.eq(None)
            )(any())
          )
            .thenReturn(Future.successful(AddressLookupSuccess(Seq())))

          showCreateForm(atarService) { result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.ManualAddressController.createForm(atarService).url)
          }
        }

        "postcode and line 1 details are not set" in {

          when(mockSessionCache.getPostcodeAndLine1Details(any()))
            .thenReturn(Future.successful(None))

          showCreateForm(atarService) { result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.ManualAddressController.createForm(atarService).url)
          }
        }

        "Address Lookup returns a failure" in {

          val postcode = "TF3 2BX"

          when(mockSessionCache.getPostcodeAndLine1Details(any()))
            .thenReturn(Future.successful(Some(PostcodeViewModel(postcode, None))))

          when(
            mockAddressLookupConnector.lookup(
              ArgumentMatchers.eq(postcode.replaceAll(" ", "")),
              ArgumentMatchers.eq(None)
            )(any())
          )
            .thenReturn(Future.successful(AddressLookupFailure))

          showCreateForm(atarService) { result =>
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(routes.ManualAddressController.createForm(atarService).url)
          }
        }
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "form has incorrect values" in {
        withAuthorisedUser(defaultUserId, mockAuthConnector)
        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("postcode", Some("addressLine 1"))))
        )
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))

        submitForm(invalidForm, atarService) { result =>
          status(result) shouldBe BAD_REQUEST
        }
      }
    }

    "redirect to manual address page" when {

      "lookup returns no addresses" in {

        withAuthorisedUser(defaultUserId, mockAuthConnector)

        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("TF3 2BX", Some("addressLine 1"))))
        )
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq())))

        submitForm(form, atarService) { result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe routes.ManualAddressController.createForm(atarService).url
        }
      }

      "address lookup fails" in {

        withAuthorisedUser(defaultUserId, mockAuthConnector)

        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("TF3 2BX", Some("addressLine 1"))))
        )
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupFailure))

        submitForm(form, atarService) { result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe routes.ManualAddressController.createForm(atarService).url
        }
      }

      "postcode and line 1 is not set" in {

        withAuthorisedUser(defaultUserId, mockAuthConnector)

        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(Future.successful(None))

        submitForm(form, atarService) { result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe routes.ManualAddressController.createForm(atarService).url
        }
      }
    }

    "return 303 (SEE_OTHER) and redirect to results page" when {
      "form is correct" in {
        withAuthorisedUser(defaultUserId, mockAuthConnector)

        when(mockSessionCache.getPostcodeAndLine1Details(any())).thenReturn(
          Future.successful(Some(PostcodeViewModel("TF3 2BX", Some("addressLine 1"))))
        )
        when(mockAddressLookupConnector.lookup(any(), any())(any()))
          .thenReturn(Future.successful(AddressLookupSuccess(Seq(addressLookup))))

        submitForm(form, atarService) { result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe "/customs-registration-services/atar/register/matching/confirm"
        }
      }
    }
  }

  private def showCreateForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.displayPage(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String], service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.submit(service).apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form)))
  }

}
