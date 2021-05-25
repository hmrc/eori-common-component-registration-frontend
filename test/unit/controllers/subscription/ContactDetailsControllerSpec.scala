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

import common.pages.subscription.SubscriptionContactDetailsPage
import common.pages.subscription.SubscriptionContactDetailsPage._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{
  ContactDetailsController,
  SubscriptionFlowManager
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.AddressViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.contact_details
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder
import util.builders.SubscriptionContactDetailsFormBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactDetailsControllerSpec extends SubscriptionFlowSpec with BeforeAndAfterEach {

  protected override val mockSubscriptionFlowManager: SubscriptionFlowManager = mock[SubscriptionFlowManager]
  protected override val formId: String                                       = SubscriptionContactDetailsPage.formId

  protected override val submitInCreateModeUrl: String =
    ContactDetailsController.submit(isInReviewMode = false, atarService).url

  protected override val submitInReviewModeUrl: String =
    ContactDetailsController.submit(isInReviewMode = true, atarService).url

  private val mockRequestSessionData  = mock[RequestSessionData]
  private val mockRegistrationDetails = mock[RegistrationDetails](RETURNS_DEEP_STUBS)
  private val mockSubscriptionDetails = mock[SubscriptionDetails](RETURNS_DEEP_STUBS)

  private val hintTextTelAndFax = "Only enter numbers, for example 01632 960 001"

  private val mockCdsFrontendDataCache = mock[SessionCache]
  private val mockOrgTypeLookup        = mock[OrgTypeLookup]
  private val contactDetailsView       = instanceOf[contact_details]

  private val controller = new ContactDetailsController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockCdsFrontendDataCache,
    mockSubscriptionFlowManager,
    mockSubscriptionDetailsHolderService,
    mockOrgTypeLookup,
    mcc,
    contactDetailsView
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier])).thenReturn(None)
    when(mockCdsFrontendDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(mockSubscriptionDetails)
    registerSaveContactDetailsMockSuccess()
    when(mockSubscriptionDetailsHolderService.cachedAddressDetails(any()))
      .thenReturn(Future.successful(Some(AddressViewModel("street", "city", None, "code"))))
    mockFunctionWithRegistrationDetails(mockRegistrationDetails)
    setupMockSubscriptionFlowManager(ContactDetailsSubscriptionFlowPageMigrate)
    when(mockCdsFrontendDataCache.email(any[HeaderCarrier])).thenReturn(Future.successful(Email))
    when(mockSubscriptionDetailsHolderService.cachedCustomsId(any())).thenReturn(Future.successful(None))
    when(mockSubscriptionDetailsHolderService.cachedNameIdDetails(any())).thenReturn(Future.successful(None))
  }

  override protected def afterEach(): Unit = {
    reset(
      mockSubscriptionBusinessService,
      mockCdsFrontendDataCache,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsHolderService
    )

    super.afterEach()
  }

  "Viewing the create form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display back link correctly" in {
      showCreateForm()(verifyBackLinkInCreateModeRegister)
    }

    "display the correct text in the heading and intro" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(headingXPath) shouldBe "Who can we contact?"
        page.getElementsText(
          introXPathSubscribe
        ) shouldBe "We will use these details to contact you about your application."
      }
    }

    "display the correct text for the continue button" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(continueButtonXpath) shouldBe ContinueButtonTextInCreateMode
      }
    }

    "display the correct hint text for telephone and fax number field" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(hintTextTelephonXpath) shouldBe hintTextTelAndFax
      }
    }
  }

  "Viewing the review form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewForm(atarService))

    "display relevant data in form fields when subscription details exist in the cache" in {

      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName
        page.getElementText(emailFieldXPath) shouldBe Email
        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
      }
    }

    "not display the number of steps and back link to review page" in {
      showReviewForm()(verifyNoStepsAndBackLinkInReviewMode)
    }

    "display the correct text for the continue button" in {
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(continueButtonXpath) shouldBe ContinueButtonTextInReviewMode
      }
    }
  }

  "submitting the form in Create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService)
    )

    "produce validation error when full name is not submitted" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe + (fullNameFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your contact name"
        page.getElementsText(fullNameFieldLevelErrorXPath) shouldBe "Error: Enter your contact name"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when full name more than 70 characters" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe + (fullNameFieldName -> oversizedString(70))) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(
            pageLevelErrorSummaryListXPath
          ) shouldBe "The full name can be a maximum of 70 characters"
          page.getElementsText(
            fullNameFieldLevelErrorXPath
          ) shouldBe "Error: The full name can be a maximum of 70 characters"
          page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone is not submitted" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe + (telephoneFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your contact telephone number"
        page.getElementsText(telephoneFieldLevelErrorXPath) shouldBe "Error: Enter your contact telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone more than 24 characters" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe + (telephoneFieldName -> oversizedString(24))) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The telephone number must be 24 digits or less"
          page.getElementsText(
            telephoneFieldLevelErrorXPath
          ) shouldBe "Error: The telephone number must be 24 digits or less"
          page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone contains invalid characters" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe + (telephoneFieldName -> "$£")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Please enter a valid telephone number"
        page.getElementsText(telephoneFieldLevelErrorXPath) shouldBe "Error: Please enter a valid telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "Allow when Telephone contains plus character" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe + (telephoneFieldName -> "+")) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "display page level errors when nothing is entered" in {
      submitFormInCreateMode(createFormAllFieldsEmptyMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe
          "Enter your contact name " + "Enter your contact telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "fail when system fails to create contact details" in {
      val unsupportedException = new UnsupportedOperationException("Emulation of service call failure")

      registerSaveContactDetailsMockFailure(unsupportedException)

      val caught = intercept[RuntimeException] {
        submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe) { result =>
          await(result)
        }
      }
      caught shouldBe unsupportedException
    }

    "allow resubmission in create mode when details are invalid" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe - fullNameFieldName)(verifyFormActionInCreateMode)
    }

    "redirect to next page when details are valid" in {
      submitFormInCreateMode(createFormMandatoryFieldsMapSubscribe)(verifyRedirectToNextPageInCreateMode)
    }

    "redirect to next page without validating contact address when 'Is this the right contact address' is Yes and country code is GB" in {
      val params = Map(
        fullNameFieldName                 -> FullName,
        emailFieldName                    -> Email,
        telephoneFieldName                -> Telephone,
        useRegisteredAddressFlagFieldName -> "true",
        countryCodeFieldName              -> "GB"
      )
      submitFormInCreateMode(params)(verifyRedirectToNextPageInCreateMode)
    }
  }

  private def mockFunctionWithRegistrationDetails(registrationDetails: RegistrationDetails) {
    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier])).thenReturn(registrationDetails)
  }

  private def submitFormInCreateMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(isInReviewMode = false, atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def showCreateForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    orgType: EtmpOrganisationType = CorporateBody
  )(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    when(mockOrgTypeLookup.etmpOrgTypeOpt(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(Some(orgType))
    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def showReviewForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    contactDetailsModel: ContactDetailsModel = contactDetailsModel
  )(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)
    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
      .thenReturn(Some(contactDetailsModel))

    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def registerSaveContactDetailsMockSuccess() {
    when(
      mockSubscriptionDetailsHolderService
        .cacheContactDetails(any[ContactDetailsModel], any[Boolean])(any[HeaderCarrier])
    ).thenReturn(Future.successful(()))
  }

  private def registerSaveContactDetailsMockFailure(exception: Throwable) {
    when(
      mockSubscriptionDetailsHolderService
        .cacheContactDetails(any[ContactDetailsModel], any[Boolean])(any[HeaderCarrier])
    ).thenReturn(Future.failed(exception))
  }

}
