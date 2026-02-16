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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{DateOfVatRegistrationController, VatReturnController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.vat.registrationdate.VatRegistrationDateFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCacheService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{date_of_vat_registration, vat_return_total, we_cannot_confirm_your_identity}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DateOfVatRegistrationControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockDateOfVatRegistrationView = inject[date_of_vat_registration]
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]
  private val mockAppConfig = mock[AppConfig]

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)

  private val mockVatReturnTotalView = inject[vat_return_total]
  private val mockWeCannotConfirmYourIdentity = inject[we_cannot_confirm_your_identity]
  private val form = inject[VatRegistrationDateFormProvider]
  private val mockSessionCacheService = inject[SessionCacheService]

  private val controller = new DateOfVatRegistrationController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mcc,
    mockDateOfVatRegistrationView,
    form,
    mockSessionCacheService,
    mockSubscriptionDetailsService,
    mockAppConfig
  )(global)

  private val controllerVat = new VatReturnController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mcc,
    mockVatReturnTotalView,
    mockWeCannotConfirmYourIdentity
  )(global)

  private val vatControlListResponse = VatControlListResponse(
    postcode = Some("SE28 1AA"),
    dateOfReg = Some("2017-01-01"),
    lastNetDue = Some(10000.02d),
    lastReturnMonthPeriod = Option("MAR")
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionBusinessService.getCachedVatControlListResponse(any[Request[_]])).thenReturn(
      Future.successful(Some(vatControlListResponse))
    )

    when(mockSubscriptionDetailsService.cachedOrganisationType(any[Request[_]]))
      .thenReturn(Future.successful(Some(CdsOrganisationType.ThirdCountrySoleTrader)))
  }

  "Date of VAT registration Controller" should {
    "return OK when accessing page though createForm method" in {
      createForm() { result =>
        status(result) shouldBe OK
      }
    }
  }

  "Submitting Vat date" should {

    "be successful when submitted with valid and data matches API response" in {
      val validReturnTotal: Map[String, String] = Map(
        "vat-registration-date.day"   -> "01",
        "vat-registration-date.month" -> "01",
        "vat-registration-date.year"  -> "2017"
      )

      submitForm(validReturnTotal) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/customs-registration-services/atar/register/contact-details")
      }
    }

    "be successful when submitted with valid and save and redirect when feature switch is on" in {
      reset(mockSubscriptionBusinessService)
      when(mockAppConfig.allowNoIdJourney).thenReturn(true)
      when(mockSubscriptionDetailsService.cachedOrganisationType(any[Request[_]]))
        .thenReturn(Future.successful(Some(CdsOrganisationType.CharityPublicBodyNotForProfit)))

      when(mockSubscriptionDetailsService.cacheVatControlListResponse(any())(any[Request[_]]))
        .thenReturn(Future.unit)

      val validReturnTotal: Map[String, String] = Map(
        "vat-registration-date.day"   -> "01",
        "vat-registration-date.month" -> "01",
        "vat-registration-date.year"  -> "2017"
      )

      verify(mockSubscriptionBusinessService, never()).getCachedVatControlListResponse(any[Request[_]])

      submitForm(validReturnTotal) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/customs-registration-services/atar/register/contact-details")
      }
    }

    "be successful when submitted with valid and save and redirect when feature switch is off" in {
      reset(mockSubscriptionBusinessService)
      when(mockAppConfig.allowNoIdJourney).thenReturn(false)
      when(mockSubscriptionDetailsService.cachedOrganisationType(any[Request[_]]))
        .thenReturn(Future.successful(Some(CdsOrganisationType.CharityPublicBodyNotForProfit)))

      when(mockSubscriptionDetailsService.cacheVatControlListResponse(any())(any[Request[_]]))
        .thenReturn(Future.unit)

      when(mockSubscriptionBusinessService.getCachedVatControlListResponse(any())).thenReturn(Future.successful(None))

      val validReturnTotal: Map[String, String] = Map(
        "vat-registration-date.day"   -> "01",
        "vat-registration-date.month" -> "01",
        "vat-registration-date.year"  -> "2017"
      )

      verify(mockSubscriptionBusinessService, never()).getCachedVatControlListResponse(any[Request[_]])

      submitForm(validReturnTotal) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "/customs-registration-services/atar/register/cannot-confirm-vat-details"
        )
      }
    }

    "redirect to cannot verify your details when valid input supplied but not matching API response" in {
      val validReturnTotal: Map[String, String] = Map(
        "vat-registration-date.day"   -> "17",
        "vat-registration-date.month" -> "11",
        "vat-registration-date.year"  -> "2000"
      )
      submitForm(validReturnTotal) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "return to the same location with bad request when submitting invalid request" in {
      val invalidVatAmountInput: Map[String, String] = Map(
        "vat-registration-date.day"   -> "This",
        "vat-registration-date.month" -> "is",
        "vat-registration-date.year"  -> "wrong"
      )
      submitForm(invalidVatAmountInput) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "return to the same location with bad request when submitting empty request" in {
    val invalidVatAmountInput: Map[String, String] =
      Map("vat-registration-date.day" -> "", "vat-registration-date.month" -> "", "vat-registration-date.year" -> "")
    submitForm(invalidVatAmountInput) { result =>
      status(result) shouldBe BAD_REQUEST

    }
  }

  "redirectToCannotConfirmIdentity" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controllerVat.redirectToCannotConfirmIdentity(atarService)
    )

    "display redirectToCannotConfirmIdentity" in {
      redirectToCannotConfirmIdentity() { result =>
        status(result) shouldBe OK
        CdsPage(contentAsString(result)).title() should startWith("We cannot verify your VAT details")
      }
    }
  }

  private def createForm()(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String])(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithFormValues(form)))
  }

  private def redirectToCannotConfirmIdentity(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controllerVat.redirectToCannotConfirmIdentity(atarService).apply(SessionBuilder.buildRequestWithSession(userId))
    )
  }

}
