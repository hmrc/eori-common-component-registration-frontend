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

package unit.controllers.registration

import common.pages.subscription.SubscriptionContactDetailsPage
import common.pages.subscription.SubscriptionContactDetailsPage._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor3
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.ContactDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.contact_details
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import unit.controllers.subscription.SubscriptionFlowSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.RegistrationDetailsBuilder.{
  defaultAddress,
  defaultAddressWithMandatoryValuesOnly,
  defaultCountryName
}
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
    mcc,
    contactDetailsView
  )

  override def beforeEach: Unit = {
    reset(
      mockSubscriptionBusinessService,
      mockCdsFrontendDataCache,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsHolderService
    )
    when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier])).thenReturn(None)
    when(mockCdsFrontendDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(mockSubscriptionDetails)
    registerSaveContactDetailsMockSuccess()
    mockFunctionWithRegistrationDetails(mockRegistrationDetails)
    setupMockSubscriptionFlowManager(ContactDetailsSubscriptionFlowPageGetEori)
    when(mockCdsFrontendDataCache.email(any[HeaderCarrier])).thenReturn(Future.successful(Email))
  }

  val orgTypeFlows: TableFor3[SubscriptionFlow, String, EtmpOrganisationType] =
    Table[SubscriptionFlow, String, EtmpOrganisationType](
      ("Flow name", "Address Label", "orgType"),
      (IndividualSubscriptionFlow, "Is this the right contact address?", NA),
      (OrganisationSubscriptionFlow, "Is this the right contact address?", CorporateBody),
      (SoleTraderSubscriptionFlow, "Is this the right contact address?", NA)
    )

  val formModesGYE = Table(
    ("formMode", "showFormFunction"),
    (
      "create Register",
      (flow: SubscriptionFlow, orgType: EtmpOrganisationType) => showCreateForm(flow, orgType = orgType)(_)
    ),
    ("review Register", (flow: SubscriptionFlow, orgType: EtmpOrganisationType) => showReviewForm(flow)(_))
  )

  forAll(formModesGYE) { (formMode, showFormFunction) =>
    s"The registration address when viewing the $formMode form" should {
      forAll(orgTypeFlows) {
        case (flow, expectedLabel, orgType) =>
          s"display appropriate label for address question in subscription flow $flow for mode $formMode" in {
            when(mockRegistrationDetails.address).thenReturn(defaultAddress)
            showFormFunction(flow, orgType) { result =>
              val page = CdsPage(contentAsString(result))
              page.getElementsText(registeredAddressQuestionXPath) shouldBe expectedLabel
            }
          }
      }

      s"display address correctly when all fields are populated for mode $formMode" in {
        when(mockRegistrationDetails.address).thenReturn(defaultAddress)
        showFormFunction(OrganisationSubscriptionFlow, CorporateBody) { result =>
          val page = CdsPage(contentAsString(result))
          val expectedAddress =
            s"${defaultAddress.addressLine1} ${defaultAddress.addressLine2.get}<br>${defaultAddress.addressLine3.get}<br>${defaultAddress.postalCode.get}<br>$defaultCountryName"
          page.getElementsHtml(registeredAddressParaXPath) shouldBe expectedAddress
        }
      }

      s"display address correctly when only mandatory fields are populated for mode $formMode" in {
        when(mockRegistrationDetails.address).thenReturn(defaultAddressWithMandatoryValuesOnly)
        showFormFunction(OrganisationSubscriptionFlow, CorporateBody) { result =>
          val page            = CdsPage(contentAsString(result))
          val expectedAddress = s"${defaultAddressWithMandatoryValuesOnly.addressLine1}<br>$defaultCountryName"
          page.getElementsHtml(registeredAddressParaXPath) shouldBe expectedAddress
        }
      }

      s"display country correctly when EU address is used for mode $formMode" in {
        val addressFirstLine = "euAddressFirstLine"
        val euAddress        = Address(addressFirstLine, None, None, None, None, "PL")
        when(mockRegistrationDetails.address).thenReturn(euAddress)
        showFormFunction(OrganisationSubscriptionFlow, CorporateBody) { result =>
          val page            = CdsPage(contentAsString(result))
          val expectedAddress = s"$addressFirstLine<br>Poland"
          page.getElementsHtml(registeredAddressParaXPath) shouldBe expectedAddress
        }
      }

      s"display country correctly when non-EU address is used for mode $formMode" in {
        val addressFirstLine = "nonEuAddressFirstLine"
        val nonEuAddress     = Address(addressFirstLine, None, None, None, None, "CA")
        when(mockRegistrationDetails.address).thenReturn(nonEuAddress)
        showFormFunction(OrganisationSubscriptionFlow, CorporateBody) { result =>
          val page            = CdsPage(contentAsString(result))
          val expectedAddress = s"$addressFirstLine<br>Canada"
          page.getElementsHtml(registeredAddressParaXPath) shouldBe expectedAddress
        }
      }
    }
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
          introXPathRegister
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

    "fill fields with contact details if stored in cache (new address entered)" in {
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
        .thenReturn(Some(contactDetailsModel))
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName
        page.getElementText(emailFieldXPath) shouldBe Email
        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
        page.radioButtonIsChecked(useRegisteredAddressYesRadioButtonXPath) shouldBe false
        page.radioButtonIsChecked(useRegisteredAddressNoRadioButtonXPath) shouldBe true
        page.getElementValue(streetFieldXPath) shouldBe Street
        page.getElementValue(cityFieldXPath) shouldBe City
        page.getElementValue(postcodeFieldXPath) shouldBe Postcode
        page.getElementValue(countryCodeSelectedOptionXPath) shouldBe CountryCode
      }
    }

    "restore state properly if registered address was used" in {
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any[HeaderCarrier]))
        .thenReturn(Some(contactDetailsModelWithRegisteredAddress))
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName
        page.getElementText(emailFieldXPath) shouldBe Email
        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
        page.radioButtonIsChecked(useRegisteredAddressYesRadioButtonXPath) shouldBe true
        page.radioButtonIsChecked(useRegisteredAddressNoRadioButtonXPath) shouldBe false
        page.getElementValue(streetFieldXPath) shouldBe empty
        page.getElementValue(cityFieldXPath) shouldBe empty
        page.getElementValue(postcodeFieldXPath) shouldBe empty
        page.elementIsPresent(countryCodeSelectedOptionXPath) shouldBe false
      }
    }

    "leave fields empty if contact details weren't found in cache" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe empty
        page.getElementValue(emailFieldXPath) shouldBe empty
        page.getElementValue(telephoneFieldXPath) shouldBe empty
        page.getElementValue(streetFieldXPath) shouldBe empty
        page.getElementValue(cityFieldXPath) shouldBe empty
        page.getElementValue(postcodeFieldXPath) shouldBe empty
        page.elementIsPresent(countryCodeSelectedOptionXPath) shouldBe false
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
        page.getElementValue(streetFieldXPath) shouldBe Street
        page.getElementValue(cityFieldXPath) shouldBe City
        page.getElementValue(postcodeFieldXPath) shouldBe Postcode
        page.getElementValue(countryCodeSelectedOptionXPath) shouldBe CountryCode
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

    "display the contact details stored in the cache under as 'subscription details'" in {
      mockFunctionWithRegistrationDetails(mockRegistrationDetails)
      when(mockSubscriptionBusinessService.cachedContactDetailsModel(any())).thenReturn(
        Some(revisedContactDetailsModel)
      )
      showReviewForm(contactDetailsModel = revisedContactDetailsModel) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(fullNameFieldXPath) shouldBe FullName
        page.getElementText(emailLabelXPath) shouldBe emailAddressFieldLabel
        page.getElementText(emailFieldXPath) shouldBe Email
        page.getElementValue(telephoneFieldXPath) shouldBe Telephone
        page.getElementValue(streetFieldXPath) shouldBe Street
        page.getElementValue(cityFieldXPath) shouldBe City
        page.getElementValue(postcodeFieldXPath) shouldBe Postcode
        page.getElementValue(countryCodeSelectedOptionXPath) shouldBe RevisedCountryCode
      }
    }
  }

  "submitting the form in Create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService)
    )

    "save the details when user chooses to use Registered Address for GYE journey" in {
      submitFormInCreateMode(createFormAllFieldsWhenUseRegAddressMap) { result =>
        await(result)
        verify(mockSubscriptionDetailsHolderService)
          .cacheContactDetails(meq(createContactDetailsViewModelWhenUseRegAddress), meq(false))(any[HeaderCarrier])
      }
    }

    "save the details when user chooses not to use Registered Address for GYE journey" in {
      submitFormInCreateMode(createFormAllFieldsWhenNotUsingRegAddressMap) { result =>
        await(result)
        verify(mockSubscriptionDetailsHolderService)
          .cacheContactDetails(meq(createContactDetailsViewModelWhenNotUsingRegAddress), meq(false))(any[HeaderCarrier])
      }
    }

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

    "produce validation error when Use registered address is not selected" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap - useRegisteredAddressFlagFieldName) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Select yes if the contact address is right"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "Select Yes when validation fails and use registered address flag Yes was submitted" in {
      submitFormInCreateMode(createFormMandatoryFieldsMap - fullNameFieldName) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementAttribute(useRegisteredAddressYesRadioButtonXPath, "checked") shouldBe "checked"
        page.getElementAttribute(useRegisteredAddressNoRadioButtonXPath, "checked") shouldBe ""
      }
    }

    "Select No when validation fails and use registered address flag No was submitted" in {
      submitFormInCreateMode(createFormMandatoryFieldsWhenNotUsingRegAddressMap - fullNameFieldName) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementAttribute(useRegisteredAddressNoRadioButtonXPath, "checked") shouldBe "checked"
        page.getElementAttribute(useRegisteredAddressYesRadioButtonXPath, "checked") shouldBe ""
      }
    }

    "require Street when user does not want to use registered address as contact address" in {
      submitFormInCreateMode(createFormMandatoryFieldsWhenNotUsingRegAddressMap + (streetFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter the first line of your address"
        page.getElementsText(streetFieldLevelErrorXPath) shouldBe "Error: Enter the first line of your address"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Street more than 70 characters" in {
      submitFormInCreateMode(
        createFormMandatoryFieldsWhenNotUsingRegAddressMap + (streetFieldName -> oversizedString(70))
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The street must be 70 characters or less"
        page.getElementsText(streetFieldLevelErrorXPath) shouldBe "Error: The street must be 70 characters or less"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "require City when user does not want to use registered address as contact address" in {
      submitFormInCreateMode(createFormMandatoryFieldsWhenNotUsingRegAddressMap + (cityFieldName -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your town or city"
        page.getElementsText(cityFieldLevelErrorXPath) shouldBe "Error: Enter your town or city"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when City more than 35 characters" in {
      submitFormInCreateMode(
        createFormMandatoryFieldsWhenNotUsingRegAddressMap + (cityFieldName -> oversizedString(35))
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The town or city must be 35 characters or less"
        page.getElementsText(cityFieldLevelErrorXPath) shouldBe "Error: The town or city must be 35 characters or less"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "produce validation error when Country is not selected" in {
      submitFormInCreateMode(createFormMandatoryFieldsWhenNotUsingRegAddressMap + (countryCodeFieldName -> "")) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter a valid country name"
          page.getElementsText(countryFieldLevelErrorXPath) shouldBe "Error: Enter a valid country name"
          page.getElementsText("title") should startWith("Error: ")
      }
    }

    "require Postcode when user does not want to use registered address as contact address and Country is GB" in {
      submitFormInCreateMode(createFormMandatoryFieldsWhenNotUsingRegAddressMap + (countryCodeFieldName -> "GB")) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter a valid postcode"
          page.getElementsText(postcodeFieldLevelErrorXPath) shouldBe "Error: Enter a valid postcode"
          page.getElementsText("title") should startWith("Error: ")
      }
    }

    "not require postcode when user does not want to use registered address as contact address and Country is not GB" in {
      submitFormInCreateMode(createFormMandatoryFieldsWhenNotUsingRegAddressMap + (countryCodeFieldName -> "FR"))(
        verifyRedirectToNextPageInCreateMode
      )
    }

    "produce validation error when postcode more than 9 characters and country is not GB" in {
      submitFormInCreateMode(
        createFormMandatoryFieldsWhenNotUsingRegAddressMap + (countryCodeFieldName -> "DE") + (postcodeFieldName -> oversizedString(
          9
        ))
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))

        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The postcode must be 9 characters or less"

        withClue(
          s"Not found in the page: field level error block for '$postcodeFieldLabel' with xpath $postcodeFieldLevelErrorXPath"
        ) {
          page.elementIsPresent(postcodeFieldLevelErrorXPath) shouldBe true
        }

        page.getElementsText(postcodeFieldLevelErrorXPath) shouldBe "Error: The postcode must be 9 characters or less"
      }
    }

    "display page level errors when nothing is entered" in {
      submitFormInCreateMode(createFormAllFieldsEmptyMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe
          "Enter your contact name " +
            "Enter your contact telephone number " +
            "Select yes if the contact address is right"
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

    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(orgType)
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
