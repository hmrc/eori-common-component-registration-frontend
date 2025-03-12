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

import common.pages.subscription.SubscriptionContactDetailsPage
import common.pages.subscription.SubscriptionContactDetailsPage._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.{TableFor2, TableFor3}
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{ContactDetailsController, SubscriptionFlowManager}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError.FlowNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.contact_details
import uk.gov.hmrc.http.HeaderCarrier
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder
import util.builders.SubscriptionContactDetailsFormBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactDetailsControllerSpec extends SubscriptionFlowSpec with BeforeAndAfterEach {

  override protected val mockSubscriptionFlowManager: SubscriptionFlowManager = mock[SubscriptionFlowManager]
  override protected val formId: String = SubscriptionContactDetailsPage.formId

  override protected val submitInCreateModeUrl: String =
    ContactDetailsController.submit(isInReviewMode = false, atarService).url

  override protected val submitInReviewModeUrl: String =
    ContactDetailsController.submit(isInReviewMode = true, atarService).url

  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockRegistrationDetails = mock[RegistrationDetails](RETURNS_DEEP_STUBS)
  private val mockSubscriptionDetails = mock[SubscriptionDetails](RETURNS_DEEP_STUBS)
  private val mockSessionCacheService = inject[SessionCacheService]

  private val mockCdsFrontendDataCache = mock[SessionCache]
  private val mockOrgTypeLookup = mock[OrgTypeLookup]
  private val contactDetailsView = inject[contact_details]

  private val controller = new ContactDetailsController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockCdsFrontendDataCache,
    mockSubscriptionFlowManager,
    mockSubscriptionDetailsService,
    mcc,
    contactDetailsView,
    mockSessionCacheService
  )(global)

  override def beforeEach(): Unit = {
    reset(mockSubscriptionBusinessService)
    reset(mockCdsFrontendDataCache)
    reset(mockSubscriptionFlowManager)
    reset(mockSubscriptionDetailsService)
    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]])).thenReturn(Future.successful(None))
    when(mockCdsFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
      Future.successful(mockSubscriptionDetails)
    )
    registerSaveContactDetailsMockSuccess()
    mockFunctionWithRegistrationDetails(mockRegistrationDetails)
    setupMockSubscriptionFlowManager(ContactDetailsSubscriptionFlowPageGetEori)
    when(mockCdsFrontendDataCache.email(any[Request[_]])).thenReturn(Future.successful(Email))
  }

  val orgTypeFlows: TableFor3[SubscriptionFlow, String, EtmpOrganisationType] =
    Table[SubscriptionFlow, String, EtmpOrganisationType](
      ("Flow name", "Address Label", "orgType"),
      (IndividualSubscriptionFlow, "Is this the right contact address?", NA),
      (OrganisationSubscriptionFlow, "Is this the right contact address?", CorporateBody),
      (SoleTraderSubscriptionFlow, "Is this the right contact address?", NA)
    )

  val formModesGYE: TableFor2[String, (SubscriptionFlow, EtmpOrganisationType) => (Future[Result] => Any) => Unit] =
    Table(
      ("formMode", "showFormFunction"),
      (
        "create Register",
        (flow: SubscriptionFlow, orgType: EtmpOrganisationType) => showCreateForm(flow, orgType = orgType)(_)
      ),
      ("review Register", (flow: SubscriptionFlow, _: EtmpOrganisationType) => showReviewForm(flow)(_))
    )

  "Viewing the create form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display back link correctly" in {
      showCreateForm()(verifyBackLinkInCreateModeRegister)
    }

    "display the correct text in the heading and intro" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(headingXPath) shouldBe "EORI number application contact details"
        page.getElementsText(
          introXPathRegister
        ) shouldBe "We will use these details to contact you about your EORI number application."
      }
    }

    "display the correct text for the continue button" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(continueButtonXpath) shouldBe ContinueButtonTextInCreateMode
      }
    }

    "fill fields with contact details if stored in cache (new address entered)" in {
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]]))
        .thenReturn(Future.successful(Some(contactDetailsModel)))
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName
        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
      }
    }

    "restore state properly if registered address was used" in {
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]]))
        .thenReturn(Future.successful(Some(contactDetailsModelWithRegisteredAddress)))
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName

        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
      }
    }

    "leave fields empty if contact details weren't found in cache" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe empty

        page.getElementValue(telephoneFieldXPath) shouldBe empty
      }
    }
  }

  "Viewing the review form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewForm(atarService))

    "display relevant data in form fields when subscription details exist in the cache" in {

      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName

        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
      }
    }

    "not display the number of steps and back link to review page" in {
      showReviewForm()(verifyNoStepsAndBackLinkInReviewMode)
    }

    "display the correct text for the continue button" in {
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(continueButtonXpath) shouldBe ContinueButtonTextInReviewMode
      }
    }

    "display the contact details stored in the cache under as 'subscription details'" in {
      mockFunctionWithRegistrationDetails(mockRegistrationDetails)
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any())).thenReturn(
        Future.successful(Some(revisedContactDetailsModel))
      )
      showReviewForm(contactDetailsModel = revisedContactDetailsModel) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName
        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
      }
    }
  }

  "submitting the form in Create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService)
    )

    "produce validation error when full name is not submitted" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap + (fullNameFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your contact name"
        page.getElementsText(fullNameFieldLevelErrorXPath) shouldBe "Error: Enter your contact name"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when full name more than 70 characters" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap + (fullNameFieldName -> oversizedString(70))) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The full name can be a maximum of 70 characters"
        page.getElementsText(
          fullNameFieldLevelErrorXPath
        ) shouldBe "Error: The full name can be a maximum of 70 characters"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone is not submitted" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap + (telephoneFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your contact telephone number"
        page.getElementsText(telephoneFieldLevelErrorXPath) shouldBe "Error: Enter your contact telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone more than 24 characters" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap + (telephoneFieldName -> oversizedString(24))) { result =>
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
      submitFormInCreateMode(createFormMandatoryFieldsMap + (telephoneFieldName -> "$£")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Please enter a valid telephone number"
        page.getElementsText(telephoneFieldLevelErrorXPath) shouldBe "Error: Please enter a valid telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "Allow when Telephone contains plus character" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap + (telephoneFieldName -> "+")) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "allow when fax contains plus character" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap + (faxFieldName -> "+")) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "display page level errors when nothing is entered" in {
      submitFormInCreateMode(createFormAllFieldsEmptyMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe
          "Enter your contact name Enter your contact telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "fail when system fails to create contact details" in {
      val unsupportedException = new UnsupportedOperationException("Emulation of service call failure")

      registerSaveContactDetailsMockFailure(unsupportedException)

      val caught = intercept[RuntimeException] {
        submitFormInCreateMode(createFormMandatoryFieldsMap) { result =>
          await(result)
        }
      }
      caught shouldBe unsupportedException
    }

    "allow resubmission in create mode when details are invalid" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap - fullNameFieldName)(verifyFormActionInCreateMode)
    }

    "redirect to next page when details are valid" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap)(verifyRedirectToNextPageInCreateMode)
    }

    "redirect to start of the page" in {
      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenReturn(Left(FlowNotFound()))
      submitFormInCreateMode(createFormMandatoryFieldsMap) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "/customs-registration-services/atar/register"

      }
    }

  }
  "submitting the form in Review mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = true, atarService)
    )

    "produce validation error when full name is not submitted" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap + (fullNameFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your contact name"
        page.getElementsText(fullNameFieldLevelErrorXPath) shouldBe "Error: Enter your contact name"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when full name more than 70 characters" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap + (fullNameFieldName -> oversizedString(70))) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The full name can be a maximum of 70 characters"
        page.getElementsText(
          fullNameFieldLevelErrorXPath
        ) shouldBe "Error: The full name can be a maximum of 70 characters"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone is not submitted" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap + (telephoneFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your contact telephone number"
        page.getElementsText(telephoneFieldLevelErrorXPath) shouldBe "Error: Enter your contact telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Telephone more than 24 characters" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap + (telephoneFieldName -> oversizedString(24))) { result =>
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
      submitFormInReviewMode(createFormMandatoryFieldsMap + (telephoneFieldName -> "$£")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Please enter a valid telephone number"
        page.getElementsText(telephoneFieldLevelErrorXPath) shouldBe "Error: Please enter a valid telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "Allow when Telephone contains plus character" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap + (telephoneFieldName -> "+")) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "allow when fax contains plus character" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap + (faxFieldName -> "+")) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "display page level errors when nothing is entered" in {
      submitFormInReviewMode(createFormAllFieldsEmptyMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe
          "Enter your contact name Enter your contact telephone number"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "fail when system fails to create contact details" in {
      val unsupportedException = new UnsupportedOperationException("Emulation of service call failure")

      registerSaveContactDetailsMockFailure(unsupportedException)

      val caught = intercept[RuntimeException] {
        submitFormInReviewMode(createFormMandatoryFieldsMap) { result =>
          await(result)
        }
      }
      caught shouldBe unsupportedException
    }

    "redirect to review page when details are valid" in {
      submitFormInReviewMode(createFormMandatoryFieldsMap) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("/review-determine")
      }
    }

  }

  private def mockFunctionWithRegistrationDetails(registrationDetails: RegistrationDetails): Unit =
    when(mockCdsFrontendDataCache.registrationDetails(any[Request[_]])).thenReturn(
      Future.successful(registrationDetails)
    )

  private def submitFormInCreateMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(isInReviewMode = false, atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def submitFormInReviewMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(isInReviewMode = true, atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def showCreateForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    orgType: EtmpOrganisationType = CorporateBody
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(orgType))
    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(subscriptionFlow)
    )

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def showReviewForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    contactDetailsModel: ContactDetailsModel = contactDetailsModel
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(subscriptionFlow)
    )
    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[Request[_]]))
      .thenReturn(Future.successful(Some(contactDetailsModel)))

    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def registerSaveContactDetailsMockSuccess(): Unit =
    when(
      mockSubscriptionDetailsService
        .cacheContactDetails(any[ContactDetailsModel], any[Boolean])(any[Request[_]])
    ).thenReturn(Future.successful(()))

  private def registerSaveContactDetailsMockFailure(exception: Throwable): Unit =
    when(
      mockSubscriptionDetailsService
        .cacheContactDetails(any[ContactDetailsModel], any[Boolean])(any[Request[_]])
    ).thenReturn(Future.failed(exception))

}
