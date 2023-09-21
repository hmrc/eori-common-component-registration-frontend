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

import cats.data.EitherT
import common.pages.subscription.SubscriptionVatDetailsPage._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{ResponseError, VatControlListConnector}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.VatDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.VatDetailsSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatControlListRequest, VatControlListResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.VatRegistrationDateFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{date_of_vat_registration, error_template, vat_details}
import uk.gov.hmrc.http.HeaderCarrier
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatDetailsControllerSpec
    extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport
    with SubscriptionFlowReviewModeTestSupport {

  protected override val formId: String = "vat-details-form"

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
      .submit(isInReviewMode = false, atarService)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
      .submit(isInReviewMode = true, atarService)
      .url

  private val mockVatControlListConnector = mock[VatControlListConnector]
  private val vatDetailsView              = instanceOf[vat_details]
  private val errorTemplate               = instanceOf[error_template]
  private val weCannotConfirmYourIdentity = instanceOf[date_of_vat_registration]
  private val form                        = instanceOf[VatRegistrationDateFormProvider]

  private val controller = new VatDetailsController(
    mockAuthAction,
    mockVatControlListConnector,
    mockSubscriptionBusinessService,
    mcc,
    vatDetailsView,
    errorTemplate,
    weCannotConfirmYourIdentity,
    mockSubscriptionDetailsService,
    form
  )

  private val validRequest = Map("postcode" -> "Z9 1AA", "vat-number" -> "028836662")

  private val defaultVatControlResponse =
    VatControlListResponse(Some("Z9 1AA"), Some("2009-11-24"), Some(213.22), Some("MAR"))

  override protected def beforeEach(): Unit = {
    reset(mockSubscriptionFlowManager)
    reset(mockVatControlListConnector)
    setupMockSubscriptionFlowManager(VatDetailsSubscriptionFlowPage)
  }

  "Loading the page in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display the form" in {
      showCreateForm()(verifyFormActionInCreateMode)
    }
  }

  "Review form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display the form with cached details" in {
      when(mockSubscriptionBusinessService.getCachedUkVatDetails(any())) thenReturn Future.successful(
        Some(VatDetails("123", "123"))
      )
      reviewForm() { result =>
        status(result) shouldBe OK
        verifyFormActionInCreateMode
        CdsPage(contentAsString(result)).title() should startWith("Your UK VAT details")
      }
    }

    "display the form with no cached details" in {
      when(mockSubscriptionBusinessService.getCachedUkVatDetails(any())) thenReturn Future.successful(None)
      reviewForm() { result =>
        status(result) shouldBe OK
        verifyFormActionInCreateMode
        CdsPage(contentAsString(result)).title() should startWith("Your UK VAT details")
      }
    }
  }

  "Submitting the form" should {

    when(mockSubscriptionDetailsService.clearCachedVatControlListResponse()(any[Request[_]])).thenReturn(
      Future.successful((): Unit)
    )

    "show error when no postcode is supplied" in {
      submitFormInCreateMode(validRequest + ("postcode" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          pageLevelErrorSummaryListXPath
        ) shouldBe "Enter a valid postcode of your VAT registration address"
        page.getElementsText(
          vatPostcodeFieldLevelError
        ) shouldBe "Error: Enter a valid postcode of your VAT registration address"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "should match without space in the postcode" in {
      submitFormInCreateMode(validRequest + ("postcode" -> "Z91AA")) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("your-uk-vat-details-date")
      }
    }

    "should match when the postcode is entered in lowercase" in {
      submitFormInCreateMode(validRequest + ("postcode" -> "z91aa")) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("your-uk-vat-details-date")
      }
    }

    "show error when no VAT number is supplied" in {
      submitFormInCreateMode(validRequest + ("vat-number" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your VAT number"
        page.getElementsText(vatNumberFieldLevelError) shouldBe "Error: Enter your VAT number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "show error when VAT number is supplied in wrong format - 10 digits" in {
      submitFormInCreateMode(validRequest + ("vat-number" -> "1234567890")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The VAT number must be 9 digits"
        page.getElementsText(vatNumberFieldLevelError) shouldBe "Error: The VAT number must be 9 digits"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "show error when VAT number is supplied in wrong format - 8 digits" in {
      submitFormInCreateMode(validRequest + ("vat-number" -> "12345678")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The VAT number must be 9 digits"
        page.getElementsText(vatNumberFieldLevelError) shouldBe "Error: The VAT number must be 9 digits"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "show error when VAT number is supplied in wrong format - 8 digits and 1 char" in {
      submitFormInCreateMode(validRequest + ("vat-number" -> "12345678a")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The VAT number must be 9 digits"
        page.getElementsText(vatNumberFieldLevelError) shouldBe "Error: The VAT number must be 9 digits"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "show error when VAT number is supplied in wrong format - 8 digits and 1 symbol" in {
      submitFormInCreateMode(validRequest + ("vat-number" -> "12345678%")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The VAT number must be 9 digits"
        page.getElementsText(vatNumberFieldLevelError) shouldBe "Error: The VAT number must be 9 digits"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect to next page when valid vat number and effective date is supplied" in {
      submitFormInCreateMode(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("your-uk-vat-details-date")
      }
    }

    "redirect to next page when valid vat number and effective date is supplied but lastNetDue is missing" in {
      val vatControlResponse = VatControlListResponse(lastNetDue = None)
      submitForm(validRequest, false, vatControllerResponse = vatControlResponse) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to next page when valid vat number and effective date is supplied but lastReturnMonthPeriod is missing" in {
      val vatControlResponse = VatControlListResponse(lastReturnMonthPeriod = None)
      submitForm(validRequest, false, vatControllerResponse = vatControlResponse) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to next page when valid vat number is supplied but lastReturnMonthPeriod is N/A" in {
      val vatControlResponse = VatControlListResponse(lastReturnMonthPeriod = Some("N/A"))
      submitForm(validRequest, false, vatControllerResponse = vatControlResponse) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to next page when valid vat number is supplied and is in review mode" in {
      submitFormInReviewMode(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith(
          "/customs-registration-services/atar/register/matching/review-determine"
        )
      }
    }

    "redirect to cannot confirm your identity when postcode does not match" in {
      submitFormInCreateMode(validRequest + ("postcode" -> "NA1 7NO")) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to cannot confirm your identity when postcode is None" in {
      val vatControlResponse = VatControlListResponse(None, Some("2009-11-24"))
      submitForm(validRequest, false, vatControllerResponse = vatControlResponse) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to cannot confirm your identity when postcode does not match and it is in review mode" in {
      submitFormInReviewMode(validRequest + ("postcode" -> "NA1 7NO")) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to cannot confirm your identity url when a Not Found response is returned" in {
      val rightValueForEitherT: Either[ResponseError, VatControlListResponse] =
        Left(ResponseError(NOT_FOUND, "not found"))

      vatControlListMock()(EitherT[Future, ResponseError, VatControlListResponse] {
        Future.successful(rightValueForEitherT)
      })

      submitFormInCreateModeForInvalidHttpStatus(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to cannot confirm your identity url when a Bad Request response is returned" in {

      val rightValueForEitherT: Either[ResponseError, VatControlListResponse] =
        Left(ResponseError(BAD_REQUEST, "bad request"))

      vatControlListMock()(EitherT[Future, ResponseError, VatControlListResponse] {
        Future.successful(rightValueForEitherT)
      })

      submitFormInCreateModeForInvalidHttpStatus(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/when-did-you-become-vat-registered")
      }
    }

    "redirect to sorry we are experiencing techincal difficulties url when a service unavailable response is returned" in {
      val rightValueForEitherT: Either[ResponseError, VatControlListResponse] =
        Left(ResponseError(SERVICE_UNAVAILABLE, "service unavailable"))

      vatControlListMock()(EitherT[Future, ResponseError, VatControlListResponse] {
        Future.successful(rightValueForEitherT)
      })

      submitFormInCreateModeForInvalidHttpStatus(validRequest) { result =>
        status(result) shouldBe SERVICE_UNAVAILABLE
      }
    }
  }

  "vatDetailsNotMatched" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.vatDetailsNotMatched(atarService))

    "display weCannotConfirmYourIdentity" in {
      vatDetailsNotMatched() {
        result =>
          status(result) shouldBe OK
          CdsPage(contentAsString(result)).title() should startWith("When did you become VAT registered")
      }
    }
  }

  "vatDetailsNotMatched in review mode" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.vatDetailsNotMatched(atarService))

    "display weCannotConfirmYourIdentity isInReviewMode true" in {
      vatDetailsNotMatched() {
        result =>
          status(result) shouldBe OK
          CdsPage(contentAsString(result)).title() should startWith("When did you become VAT registered")
      }
    }
  }

  private def showCreateForm(userId: String = defaultUserId, cachedDate: Option[LocalDate] = None)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionBusinessService.maybeCachedDateEstablished(any[Request[_]]))
      .thenReturn(Future.successful(cachedDate))

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def reviewForm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitFormInCreateMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = false)(test)

  private def submitFormInReviewMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = true)(test)

  private def submitFormInCreateModeForInvalidHttpStatus(
    form: Map[String, String]
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockSubscriptionDetailsService.cacheUkVatDetails(any[VatDetails])(any[Request[_]]))
      .thenReturn(Future.successful(()))
    test(
      controller
        .submit(false, atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form))
    )
  }

  private def submitForm(
    form: Map[String, String],
    isInReviewMode: Boolean,
    userId: String = defaultUserId,
    vatControllerResponse: VatControlListResponse = defaultVatControlResponse
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    val rightValueForEitherT: Either[ResponseError, VatControlListResponse] =
      Right(vatControllerResponse)

    vatControlListMock()(EitherT[Future, ResponseError, VatControlListResponse] {
      Future.successful(rightValueForEitherT)
    })

    when(mockSubscriptionDetailsService.cacheUkVatDetails(any[VatDetails])(any[Request[_]]))
      .thenReturn(Future.successful(()))
    test(
      controller
        .submit(isInReviewMode, atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def vatDetailsNotMatched(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.vatDetailsNotMatched(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  def vatControlListMock()(response: EitherT[Future, ResponseError, VatControlListResponse]): Unit =
    when(mockVatControlListConnector.vatControlList(any[VatControlListRequest])(any[HeaderCarrier])) thenReturn response

}
