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

import common.pages.subscription.SicCodePage._
import common.pages.subscription.{SicCodePage, SubscriptionAmendCompanyDetailsPage}
import common.support.testdata.subscription.BusinessDatesOrganisationTypeTables
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.SicCodeController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.{
  Company,
  Individual,
  SoleTrader,
  ThirdCountryOrganisation,
  Partnership => CdsPartnership
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SicCodeSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.sic_code
import util.StringThings._
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SicCodeControllerSpec
    extends SubscriptionFlowTestSupport with BusinessDatesOrganisationTypeTables with BeforeAndAfterEach
    with SubscriptionFlowCreateModeTestSupport with SubscriptionFlowReviewModeTestSupport {

  protected override val formId: String = SicCodePage.formId

  protected override def submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.SicCodeController
      .submit(isInReviewMode = false, atarService)
      .url

  protected override def submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.SicCodeController
      .submit(isInReviewMode = true, atarService)
      .url

  private val mockOrgTypeLookup      = mock[OrgTypeLookup]
  private val mockRequestSessionData = mock[RequestSessionData]
  private val sicCodeView            = instanceOf[sic_code]

  private val controller = new SicCodeController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionFlowManager,
    mockSubscriptionDetailsHolderService,
    mockOrgTypeLookup,
    mcc,
    sicCodeView,
    mockRequestSessionData
  )

  private val emulatedFailure = new UnsupportedOperationException("Emulation of service call failure")

  override protected def beforeEach: Unit = {
    super.beforeEach()

    when(mockSubscriptionBusinessService.cachedSicCode(any[Request[_]])).thenReturn(None)
    registerSaveDetailsMockSuccess()
    setupMockSubscriptionFlowManager(SicCodeSubscriptionFlowPage)
  }

  override protected def afterEach(): Unit = {
    reset(
      mockSubscriptionBusinessService,
      mockSubscriptionFlowManager,
      mockOrgTypeLookup,
      mockSubscriptionDetailsHolderService,
      mockRequestSessionData
    )

    super.afterEach()
  }

  val sic = "99996"

  private val mandatoryFieldsMap = Map("sic" -> sic)

  private val populatedSicCodeFieldsMap   = Map("sic" -> sic)
  private val unpopulatedSicCodeFieldsMap = Map("sic" -> "")

  "Subscription Sic Code form in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display title as 'What is your Standard Industrial Classification (SIC) code?' for non-partnership org type" in {
      showCreateForm(orgType = CorporateBody, userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        page.title should startWith("What is your Standard Industrial Classification (SIC) code?")
      }
    }

    "display correct hint text when org type is company and user location is UK" in {
      showCreateForm(orgType = CorporateBody, userSelectedOrgType = Company, userLocation = Some("UK")) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(
          sicDescriptionLabelXpath
        ) shouldBe "Find your SIC code (opens in new window or tab) If you do not have one, you can search for a relevant SIC code on Companies House (opens in a new window or tab)"
      }
    }

    "display heading as 'What is the Standard Industrial Classification (SIC) code for your organisation?' for non-partnership org type" in {
      showCreateForm(orgType = CorporateBody, userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(
          SicCodePage.headingXpath
        ) shouldBe "What is your Standard Industrial Classification (SIC) code?"
      }
    }

    "display title as 'What is your Standard Industrial Classification (SIC) code?' for Partnership org type" in {
      showCreateForm(orgType = Partnership, userSelectedOrgType = CdsPartnership) { result =>
        val page = CdsPage(contentAsString(result))
        page.title should startWith("What is your Standard Industrial Classification (SIC) code?")
      }
    }

    "display heading as 'What is the Standard Industrial Classification (SIC) code?' for Partnership org type" in {
      showCreateForm(orgType = Partnership, userSelectedOrgType = CdsPartnership) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(
          SicCodePage.headingXpath
        ) shouldBe "What is your Standard Industrial Classification (SIC) code?"
      }
    }

    "display title as 'What is the Standard Industrial Classification (SIC) code?' for LLP org type" in {
      showCreateForm(orgType = LLP, userSelectedOrgType = CdsPartnership) { result =>
        val page = CdsPage(contentAsString(result))
        page.title should startWith("What is your Standard Industrial Classification (SIC) code?")
      }
    }

    "display heading as 'What is the Standard Industrial Classification (SIC) code?' for LLP org type" in {
      showCreateForm(orgType = LLP, userSelectedOrgType = CdsPartnership) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(
          SicCodePage.headingXpath
        ) shouldBe "What is your Standard Industrial Classification (SIC) code?"
      }
    }

    "display correct description for non-partnership org type" in {
      showCreateForm(orgType = CorporateBody, userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
      }
    }

    "display correct description for Partnership org type" in {
      showCreateForm(orgType = Partnership, userSelectedOrgType = CdsPartnership) { result =>
        val page = CdsPage(contentAsString(result))
      }
    }

    "display correct description for LLP org type" in {
      showCreateForm(orgType = LLP, userSelectedOrgType = CdsPartnership) { result =>
        val page = CdsPage(contentAsString(result))
      }
    }

    val formModes = Table(("userSelectedOrgType", "orgType"), (SoleTrader, "SoleTrader"), (Individual, "Individual"))

    forAll(formModes) { (userSelectedOrgType, orgType) =>
      s"display title as 'What is your Standard Industrial Classification (SIC) code?' for $orgType (NA) org type" in {
        showCreateForm(orgType = NA, userSelectedOrgType = userSelectedOrgType) { result =>
          val page = CdsPage(contentAsString(result))
          userSelectedOrgType match {

            case Individual =>
              page.title should startWith(
                "Enter a Standard Industrial Classification (SIC) code that describes what your business does"
              )
            case _ =>
              page.title should startWith("What is your Standard Industrial Classification (SIC) code?")
          }
        }
      }

      s"display heading as 'What is your Standard Industrial Classification (SIC) code?' for $orgType (NA) org type" in {
        showCreateForm(orgType = NA, userSelectedOrgType = userSelectedOrgType) { result =>
          val page = CdsPage(contentAsString(result))
          userSelectedOrgType match {
            case Individual =>
              page.getElementText(SicCodePage.headingXpath) should startWith(
                "Enter a Standard Industrial Classification (SIC) code that describes what your business does"
              )
            case _ =>
              page.getElementText(SicCodePage.headingXpath) should startWith(
                "What is your Standard Industrial Classification (SIC) code"
              )
          }
        }
      }

      s"display correct description for $orgType (NA) org type" in {
        showCreateForm(orgType = NA, userSelectedOrgType = userSelectedOrgType) {
          result =>
            val page = CdsPage(contentAsString(result))
            userSelectedOrgType match {
              case Individual =>
                page.getElementText(SicCodePage.sicDescriptionLabelXpath) should startWith(
                  "A SIC code is a 5 digit number that helps HMRC identify what your organisation does. If you do not have one, you can search for a relevant SIC code on Companies House (opens in new tab)."
                )
                page.getElementsHref("//*[@id='description']/a") shouldBe "https://resources.companieshouse.gov.uk/sic/"
              case _ =>
                page.getElementText(SicCodePage.sicDescriptionLabelXpath) should startWith(
                  "Find your SIC code (opens in new window or tab) If you do not have one, you can search for a relevant SIC code on Companies House (opens in a new window or tab)"
                )
                page.getElementsHref(
                  "//*[@id='description']/a"
                ) shouldBe "https://www.gov.uk/get-information-about-a-company"
            }
        }
      }

      s"display correct label description for $orgType (NA) org type" in {
        showCreateForm(orgType = NA, userSelectedOrgType = userSelectedOrgType) { result =>
          val page = CdsPage(contentAsString(result))
        }
      }
    }

    "submit in create mode" in {
      showCreateForm(userSelectedOrgType = Company)(verifyFormActionInCreateMode)
    }

    "display the back link" in {
      showCreateForm(userSelectedOrgType = Company)(verifyBackLinkInCreateModeRegister)
    }

    "have SIC code input field without data if not cached previously" in {
      showCreateForm(userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        verifyPrincipalEconomicActivityFieldExistsWithNoData(page)
      }
    }

    "have SIC code input field prepopulated if cached previously" in {
      when(mockSubscriptionBusinessService.cachedSicCode(any[Request[_]])).thenReturn(Future.successful(Some(sic)))
      showCreateForm(userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        verifyPrincipalEconomicActivityFieldExistsAndPopulatedCorrectly(page)
      }
    }

    "display the correct text for the continue button" in {
      showCreateForm(userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(SicCodePage.continueButtonXpath) shouldBe ContinueButtonTextInCreateMode
      }
    }
  }

  "Subscription Sic Code form in review mode" should {

    "submit in review mode" in {
      showReviewForm(userSelectedOrgType = Company)(verifyFormSubmitsInReviewMode)
    }

    "retrieve the cached data" in {
      showReviewForm(userSelectedOrgType = Company) { result =>
        CdsPage(contentAsString(result))
        verify(mockSubscriptionBusinessService).getCachedSicCode(any[Request[_]])
      }
    }

    "have all the required input fields without data" in {
      showReviewForm(sic, userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        verifyPrincipalEconomicActivityFieldExistsAndPopulatedCorrectly(page)
      }
    }

    "display the correct text for the continue button" in {
      showReviewForm(userSelectedOrgType = Company) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(SicCodePage.continueButtonXpath) shouldBe ContinueButtonTextInReviewMode
      }
    }
  }

  "submitting the form with all mandatory fields filled when in create mode for all organisation types" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService)
    )

    "wait until the saveSubscriptionDetailsHolder is completed before progressing" in {
      registerSaveDetailsMockFailure(emulatedFailure)

      val caught = intercept[RuntimeException] {
        submitFormInCreateMode(mandatoryFieldsMap, userSelectedOrgType = Company) { result =>
          await(result)
        }
      }

      caught shouldBe emulatedFailure
    }

    "redirect to next screen" in {
      submitFormInCreateMode(mandatoryFieldsMap, userSelectedOrgType = Company)(verifyRedirectToNextPageInCreateMode)
    }
  }

  "submitting the form with all mandatory fields filled when in review mode for all organisation types" should {

    "wait until the saveSubscriptionDetailsHolder is completed before progressing" in {
      registerSaveDetailsMockFailure(emulatedFailure)

      val caught = intercept[RuntimeException] {
        submitFormInReviewMode(populatedSicCodeFieldsMap, userSelectedOrgType = Company) { result =>
          await(result)
        }
      }

      caught shouldBe emulatedFailure
    }

    "redirect to review screen" in {
      submitFormInReviewMode(populatedSicCodeFieldsMap, userSelectedOrgType = Company)(verifyRedirectToReviewPage())
    }
  }

  "Submitting in Create Mode when entries are invalid" should {

    "allow resubmission in create mode" in {
      submitFormInCreateMode(unpopulatedSicCodeFieldsMap, userSelectedOrgType = Company)(verifyFormActionInCreateMode)
    }
  }

  "Submitting in Review Mode when entries are invalid" should {

    "allow resubmission in review mode" in {
      submitFormInReviewMode(unpopulatedSicCodeFieldsMap, userSelectedOrgType = Company)(verifyFormSubmitsInReviewMode)
    }
  }

  "SIC Code" should {

    "be mandatory" in {
      submitFormInCreateMode(Map("sic" -> ""), userSelectedOrgType = Company) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.pageLevelErrorSummaryListXPath
        ) shouldEqual "Enter a SIC code"
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.sicFieldLevelErrorXpath
        ) shouldEqual "Error: Enter a SIC code"
      }
    }

    "not allow spaces instead of numbers" in {
      submitFormInCreateMode(Map("sic" -> 5.spaces), userSelectedOrgType = Company) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.pageLevelErrorSummaryListXPath
        ) shouldEqual "Enter a SIC code"
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.sicFieldLevelErrorXpath
        ) shouldEqual "Error: Enter a SIC code"
      }
    }

    "be numeric" in {
      submitFormInCreateMode(Map("sic" -> "G1A3"), userSelectedOrgType = Company) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Enter a SIC code in the right format"
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.sicFieldLevelErrorXpath
        ) shouldEqual "Error: Enter a SIC code in the right format"
      }
    }

    "be at least 4 digits" in {
      submitFormInCreateMode(Map("sic" -> "123"), userSelectedOrgType = Company) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The SIC code must be more than 3 digits"
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.sicFieldLevelErrorXpath
        ) shouldEqual "Error: The SIC code must be more than 3 digits"
      }
    }

    "be maximum of 5 digits" in {
      submitFormInCreateMode(Map("sic" -> "123456"), userSelectedOrgType = Company) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The SIC code must be 5 digits or less"
        page.getElementsText(
          SubscriptionAmendCompanyDetailsPage.sicFieldLevelErrorXpath
        ) shouldEqual "Error: The SIC code must be 5 digits or less"
      }
    }

    "allow 5 digit number with non significant digits" in {
      submitFormInCreateMode(Map("sic" -> "00120"), userSelectedOrgType = Company) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "allow 5 digit number with whitespace" in {
      submitFormInCreateMode(Map("sic" -> " 20 92 0 "), userSelectedOrgType = Company) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  val formModelsROW = Table(
    ("userSelectedOrgType", "orgType", "userLocation"),
    (SoleTrader, "SoleTrader", "iom"),
    (ThirdCountryOrganisation, "Organisation", "third-country")
  )

  forAll(formModelsROW) { (userSelectedOrgType, orgType, userLocation) =>
    s"display correct description for $orgType (NA) org type and when user-location is NON-UK: $userLocation" in {
      showCreateForm(userSelectedOrgType = userSelectedOrgType, userLocation = Some(userLocation)) { result =>
        val page = CdsPage(contentAsString(result))
        if (userSelectedOrgType == SoleTrader) {
          page.getElementsText(
            sicDescriptionLabelXpath
          ) shouldBe "A SIC code is a 5 digit number that helps HMRC identify what your business does. In some countries it is also known as a trade number. If you do not have one, you can search for a relevant SIC code on Companies House (opens in new tab)."
          page.getElementText(
            SicCodePage.headingXpath
          ) shouldBe "Enter a Standard Industrial Classification (SIC) code that describes what your business does"
          page.getElementsHref("//*[@id='description']/a") shouldBe "https://resources.companieshouse.gov.uk/sic/"
        } else {
          page.getElementsText(
            sicDescriptionLabelXpath
          ) shouldBe "Find your SIC code (opens in new window or tab) If you do not have one, you can search for a relevant SIC code on Companies House (opens in a new window or tab)"
          page.getElementText(
            SicCodePage.headingXpath
          ) shouldBe "What is your Standard Industrial Classification (SIC) code?"
          page.getElementsHref("//*[@id='description']/a") shouldBe "https://www.gov.uk/get-information-about-a-company"
        }
      }
    }
  }

  private def submitFormInCreateMode(
    form: Map[String, String],
    userId: String = defaultUserId,
    orgType: EtmpOrganisationType = CorporateBody,
    userSelectedOrgType: CdsOrganisationType
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(orgType)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(userSelectedOrgType))

    test(
      controller
        .submit(isInReviewMode = false, atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def submitFormInReviewMode(
    form: Map[String, String],
    userId: String = defaultUserId,
    orgType: EtmpOrganisationType = CorporateBody,
    userSelectedOrgType: CdsOrganisationType
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(orgType)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(userSelectedOrgType))

    test(
      controller
        .submit(isInReviewMode = true, atarService)(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  private def registerSaveDetailsMockSuccess() {
    when(mockSubscriptionDetailsHolderService.cacheSicCode(anyString())(any[Request[_]]))
      .thenReturn(Future.successful(()))
  }

  private def registerSaveDetailsMockFailure(exception: Throwable) {
    when(mockSubscriptionDetailsHolderService.cacheSicCode(anyString)(any[Request[_]]))
      .thenReturn(Future.failed(exception))
  }

  private def showCreateForm(
    userId: String = defaultUserId,
    orgType: EtmpOrganisationType = CorporateBody,
    userSelectedOrgType: CdsOrganisationType,
    userLocation: Option[String] = Some("uk")
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(orgType)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(userSelectedOrgType))
    when(mockRequestSessionData.selectedUserLocation(any[Request[AnyContent]])).thenReturn(userLocation)

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def showReviewForm(
    dataToEdit: String = sic,
    userId: String = defaultUserId,
    orgType: EtmpOrganisationType = CorporateBody,
    userSelectedOrgType: CdsOrganisationType
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(orgType)
    when(mockRequestSessionData.userSelectedOrganisationType(any[Request[AnyContent]]))
      .thenReturn(Some(userSelectedOrgType))
    when(mockSubscriptionBusinessService.getCachedSicCode(any[Request[_]])).thenReturn(dataToEdit)

    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def verifyPrincipalEconomicActivityFieldExistsAndPopulatedCorrectly(page: CdsPage): Unit =
    page.getElementValue(SubscriptionAmendCompanyDetailsPage.sicIdXpath) shouldBe sic

  private def verifyPrincipalEconomicActivityFieldExistsWithNoData(page: CdsPage): Unit =
    page.getElementValue(SubscriptionAmendCompanyDetailsPage.sicIdXpath) shouldBe empty

}
