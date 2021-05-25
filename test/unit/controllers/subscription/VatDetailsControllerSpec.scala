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

import common.pages.subscription.SubscriptionVatDetailsPage._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  InvalidResponse,
  NotFoundResponse,
  ServiceUnavailableResponse,
  VatControlListConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.VatDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.VatDetailsSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatControlListRequest, VatControlListResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{vat_details, we_cannot_confirm_your_identity}
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VatDetailsControllerSpec
    extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport
    with SubscriptionFlowReviewModeTestSupport {

  protected override val formId: String = "vat-details-form"

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatDetailsController
      .submit(isInReviewMode = false, atarService, Journey.Register)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatDetailsController
      .submit(isInReviewMode = true, atarService, Journey.Register)
      .url

  private val mockVatControlListConnector = mock[VatControlListConnector]

  private val vatDetailsView              = instanceOf[vat_details]
  private val errorTemplate               = instanceOf[error_template]
  private val weCannotConfirmYourIdentity = instanceOf[we_cannot_confirm_your_identity]

  private val controller = new VatDetailsController(
    mockAuthAction,
    mockSubscriptionFlowManager,
    mockVatControlListConnector,
    mockSubscriptionBusinessService,
    mcc,
    vatDetailsView,
    errorTemplate,
    weCannotConfirmYourIdentity,
    mockSubscriptionDetailsHolderService
  )

  private val validRequest = Map(
    "postcode"                 -> "Z9 1AA",
    "vat-number"               -> "028836662",
    "vat-effective-date.day"   -> "24",
    "vat-effective-date.month" -> "11",
    "vat-effective-date.year"  -> "2009"
  )

  override protected def beforeEach(): Unit = {
    reset(mockSubscriptionFlowManager, mockVatControlListConnector)
    setupMockSubscriptionFlowManager(VatDetailsSubscriptionFlowPage)
  }

  "Loading the page in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.createForm(atarService, Journey.Subscribe)
    )

    "display the form" in {
      showCreateForm()(verifyFormActionInCreateMode)
    }
  }

  "Submitting the form" should {

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
        result.header.headers("Location") should endWith("next-page-url")
      }
    }

    "should match when the postcode is entered in lowercase" in {
      submitFormInCreateMode(validRequest + ("postcode" -> "z91aa")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("next-page-url")
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

    "show error when no effective date is supplied" in {
      submitFormInCreateMode(
        validRequest +
          ("vat-effective-date.day"  -> "",
          "vat-effective-date.month" -> "",
          "vat-effective-date.year"  -> "")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your effective VAT date"
        page.getElementsText(vatEffectiveDateFieldLevelError) shouldBe "Error: Enter your effective VAT date"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "show error when an invalid effective date is supplied" in {
      submitFormInCreateMode(
        validRequest +
          ("vat-effective-date.day"  -> "31",
          "vat-effective-date.month" -> "04",
          "vat-effective-date.year"  -> "2002")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Effective VAT date must be a real date"
        page.getElementsText(vatEffectiveDateFieldLevelError) shouldBe "Error: Effective VAT date must be a real date"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "show error when a future effective date is supplied" in {
      val tomorrow = LocalDate.now().plusDays(1)
      submitFormInCreateMode(
        validRequest +
          ("vat-effective-date.day"  -> tomorrow.getDayOfMonth.toString,
          "vat-effective-date.month" -> tomorrow.getMonthOfYear.toString,
          "vat-effective-date.year"  -> tomorrow.getYear.toString)
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Effective VAT date must be in the past"
        page.getElementsText(vatEffectiveDateFieldLevelError) shouldBe "Error: Effective VAT date must be in the past"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "redirect to next page when valid vat number and effective date is supplied" in {
      submitFormInCreateMode(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("next-page-url")
      }
    }

    "redirect to next page when valid vat number and effective date is supplied and is in review mode" in {
      submitFormInReviewMode(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith(
          "/customs-enrolment-services/atar/register/matching/review-determine"
        )
      }
    }

    "redirect to cannot confirm your identity when postcode does not match" in {
      submitFormInCreateMode(validRequest + ("postcode" -> "NA1 7NO")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/cannot-confirm-your-vat-details")
      }
    }

    "redirect to cannot confirm your identity when postcode does not match and it is in review mode" in {
      submitFormInReviewMode(validRequest + ("postcode" -> "NA1 7NO")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/cannot-confirm-your-vat-details/review")
      }
    }

    "redirect to cannot confirm your identity url when effective is not associated with the vrn" in {
      val updatedRequest = validRequest + ("vat-effective-date.day" -> "25")
      submitFormInCreateMode(updatedRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/cannot-confirm-your-vat-details")
      }
    }

    "redirect to cannot confirm your identity url when a Not Found response is returned" in {
      when(mockVatControlListConnector.vatControlList(any[VatControlListRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(NotFoundResponse)))
      submitFormInCreateModeForInvalidHttpStatus(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/cannot-confirm-your-vat-details")
      }
    }

    "redirect to cannot confirm your identity url when a Bad Request response is returned" in {
      when(mockVatControlListConnector.vatControlList(any[VatControlListRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(InvalidResponse)))
      submitFormInCreateModeForInvalidHttpStatus(validRequest) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/cannot-confirm-your-vat-details")
      }
    }

    "redirect to sorry we are experiencing techincal difficulties url when a service unavailable response is returned" in {
      when(mockVatControlListConnector.vatControlList(any[VatControlListRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ServiceUnavailableResponse)))
      submitFormInCreateModeForInvalidHttpStatus(validRequest) { result =>
        status(result) shouldBe SERVICE_UNAVAILABLE
      }
    }
  }

  private def showCreateForm(
    userId: String = defaultUserId,
    cachedDate: Option[LocalDate] = None,
    journey: Journey.Value = Journey.Register
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionBusinessService.maybeCachedDateEstablished(any[HeaderCarrier]))
      .thenReturn(Future.successful(cachedDate))

    test(controller.createForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitFormInCreateMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = false)(test)

  private def submitFormInReviewMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = true)(test)

  private def submitFormInCreateModeForInvalidHttpStatus(
    form: Map[String, String]
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockSubscriptionDetailsHolderService.cacheUkVatDetails(any[VatDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    test(
      controller
        .submit(false, atarService, Journey.Register)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form))
    )
  }

  private def submitForm(
    form: Map[String, String],
    isInReviewMode: Boolean,
    userId: String = defaultUserId,
    journey: Journey.Value = Journey.Register
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    val vatControlResponse = VatControlListResponse(Some("Z9 1AA"), Some("2009-11-24"))

    when(mockVatControlListConnector.vatControlList(any[VatControlListRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(vatControlResponse)))
    when(mockSubscriptionDetailsHolderService.cacheUkVatDetails(any[VatDetails])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    test(
      controller
        .submit(isInReviewMode, atarService, journey)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

}
