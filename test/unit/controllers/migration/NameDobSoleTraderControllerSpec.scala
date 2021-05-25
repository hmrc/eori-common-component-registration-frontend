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

package unit.controllers.migration

import java.time.Year

import common.pages.migration.NameDobSoleTraderPage
import common.pages.migration.NameDobSoleTraderPage._
import common.pages.registration.DoYouHaveAnEoriPage.pageLevelErrorSummaryListXPath
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.NameDobSoleTraderController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.enter_your_details
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import unit.controllers.subscription.SubscriptionFlowSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NameDobSoleTraderControllerSpec extends SubscriptionFlowSpec with BeforeAndAfterEach {

  protected override val mockSubscriptionFlowManager: SubscriptionFlowManager = mock[SubscriptionFlowManager]
  protected override val formId: String                                       = NameDobSoleTraderPage.formId

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameDobSoleTraderController
      .submit(isInReviewMode = false, atarService, Journey.Subscribe)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.NameDobSoleTraderController
      .submit(isInReviewMode = true, atarService, Journey.Subscribe)
      .url

  private val mockRequestSessionData   = mock[RequestSessionData]
  private val mockRegistrationDetails  = mock[RegistrationDetails](RETURNS_DEEP_STUBS)
  private val mockCdsFrontendDataCache = mock[SessionCache]
  private val enterYourDetails         = instanceOf[enter_your_details]

  private val controller = new NameDobSoleTraderController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockRequestSessionData,
    mockCdsFrontendDataCache,
    mockSubscriptionFlowManager,
    mcc,
    enterYourDetails,
    mockSubscriptionDetailsHolderService
  )

  private val emulatedFailure = new UnsupportedOperationException("Emulation of service call failure")

  private val stringContaining36Characters      = "Abcdef ghi-jklm - nopqrstuvwxyz ABCD"
  private val stringContainingInvalidCharacters = "John Doe%"

  override def beforeEach: Unit = {
    reset(
      mockSubscriptionBusinessService,
      mockCdsFrontendDataCache,
      mockSubscriptionFlowManager,
      mockSubscriptionDetailsHolderService
    )
    when(mockSubscriptionBusinessService.cachedSubscriptionNameDobViewModel(any[HeaderCarrier])).thenReturn(None)
    when(mockSubscriptionBusinessService.getCachedSubscriptionNameDobViewModel(any[HeaderCarrier]))
      .thenReturn(Future.successful(NameDobSoleTraderPage.filledValues))

    when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.SoleTrader))

    registerSaveNameDobDetailsMockSuccess()
    mockFunctionWithRegistrationDetails(mockRegistrationDetails)

    setupMockSubscriptionFlowManager(NameDobDetailsSubscriptionFlowPage)
  }

  val subscriptionFlows: TableFor2[SubscriptionFlow, String] =
    Table[SubscriptionFlow, String](("Flow name", "Label"), (SoleTraderFlow, "Enter your details"))

  val formModes = Table(
    ("formMode", "showFormFunction"),
    ("create", (flow: SubscriptionFlow) => showCreateForm(flow)(_)),
    ("review", (flow: SubscriptionFlow) => showReviewForm(flow)(_))
  )

  forAll(formModes) { (formMode, showFormFunction) =>
    s"The name / dob when viewing the $formMode form" should {

      forAll(subscriptionFlows) {
        case (subscriptionFlow, expectedLabel) =>
          s"display appropriate label in subscription flow $subscriptionFlow" in {
            showFormFunction(subscriptionFlow) { result =>
              val page = CdsPage(contentAsString(result))
              page.getElementsText(pageTitleXPath) shouldBe expectedLabel
            }
          }
      }

      "display name / dob correctly when all fields are populated" in {
        when(mockSubscriptionBusinessService.cachedSubscriptionNameDobViewModel(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(NameDobSoleTraderPage.filledValues)))

        showFormFunction(SoleTraderFlow) { result =>
          val page = CdsPage(contentAsString(result))

          val expectedFirstName = s"${NameDobSoleTraderPage.filledValues.firstName}"
          val expectedLastName  = s"${NameDobSoleTraderPage.filledValues.lastName}"
          val expectedDob       = s"${NameDobSoleTraderPage.filledValues.dateOfBirth}"

          page.getElementValue(firstNameFieldXPath) shouldBe expectedFirstName
          page.getElementValue(lastNameFieldXPath) shouldBe expectedLastName
          getDobFromPage(page) shouldBe expectedDob
        }
      }
    }
  }

  "Viewing the create form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.createForm(atarService, Journey.Subscribe)
    )

    "display back link correctly" in {
      showCreateForm()(verifyBackLinkInCreateModeRegister)
    }

    "display the correct text for the continue button" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(continueButtonXpath) shouldBe ContinueButtonTextInCreateMode
      }
    }

    "fill fields with details if stored in cache" in {
      when(mockSubscriptionBusinessService.cachedSubscriptionNameDobViewModel(any[HeaderCarrier]))
        .thenReturn(Some(NameDobSoleTraderPage.filledValues))
      showCreateForm() { result =>
        val page              = CdsPage(contentAsString(result))
        val expectedFirstName = s"${NameDobSoleTraderPage.filledValues.firstName}"
        val expectedLastName  = s"${NameDobSoleTraderPage.filledValues.lastName}"
        val expectedDob       = s"${NameDobSoleTraderPage.filledValues.dateOfBirth}"

        page.getElementValue(firstNameFieldXPath) shouldBe expectedFirstName
        page.getElementValue(lastNameFieldXPath) shouldBe expectedLastName
        getDobFromPage(page) shouldBe expectedDob
      }
    }

    "leave fields empty if details weren't found in cache" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(firstNameFieldXPath) shouldBe 'empty
        page.getElementValue(lastNameFieldXPath) shouldBe 'empty
        page.getElementValue(dateOfBirthYearFieldXPath) shouldBe 'empty
        page.getElementValue(dateOfBirthMonthFieldXPath) shouldBe 'empty
        page.getElementValue(dateOfBirthDayFieldXPath) shouldBe 'empty
      }
    }
  }

  "Viewing the review form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.reviewForm(atarService, Journey.Subscribe)
    )

    "display relevant data in form fields when subscription details exist in the cache" in {
      when(mockSubscriptionBusinessService.getCachedSubscriptionNameDobViewModel(any()))
        .thenReturn(NameDobSoleTraderPage.filledValues)

      showReviewForm() { result =>
        val page              = CdsPage(contentAsString(result))
        val expectedFirstName = s"${NameDobSoleTraderPage.filledValues.firstName}"
        val expectedLastName  = s"${NameDobSoleTraderPage.filledValues.lastName}"
        val expectedDob       = s"${NameDobSoleTraderPage.filledValues.dateOfBirth}"

        page.getElementValue(firstNameFieldXPath) shouldBe expectedFirstName
        page.getElementValue(lastNameFieldXPath) shouldBe expectedLastName
        getDobFromPage(page) shouldBe expectedDob
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

  "Submitting the form in Create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService, Journey.Subscribe)
    )

    "save the details" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap) { result =>
        await(result)
        verify(mockSubscriptionDetailsHolderService).cacheNameDobDetails(meq(NameDobSoleTraderPage.filledValues))(
          any[HeaderCarrier]
        )
      }
    }

    "validation error when first name is not submitted" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (firstNameFieldId -> "")) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your first name"
        page.getElementsText(firstNameFieldLevelErrorXPath) shouldBe "Error: Enter your first name"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when first name is not submitted for Row" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (firstNameFieldId -> ""), location = "eu") { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your given name"
        page.getElementsText(firstNameFieldLevelErrorXPath) shouldBe "Error: Enter your given name"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when first name more than 35 characters" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (firstNameFieldName -> stringContaining36Characters)) {
        result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The first name must be 35 characters or less"
          page.getElementsText(
            firstNameFieldLevelErrorXPath
          ) shouldBe "Error: The first name must be 35 characters or less"
          page.getElementsText("title") should startWith("Error: ")
          verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when first name more than 35 characters for Row" in {
      submitFormInCreateMode(
        createFormAllFieldsNameDobMap + (firstNameFieldName -> stringContaining36Characters),
        location = "eu"
      ) {
        result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The given name must be 35 characters or less"
          page.getElementsText(
            firstNameFieldLevelErrorXPath
          ) shouldBe "Error: The given name must be 35 characters or less"
          page.getElementsText("title") should startWith("Error: ")
          verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when first name has invalid characters" in {
      submitFormInCreateMode(
        createFormAllFieldsNameDobMap + (firstNameFieldName -> stringContainingInvalidCharacters)
      ) {
        result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter a first name without invalid characters"
          page.getElementsText(
            firstNameFieldLevelErrorXPath
          ) shouldBe "Error: Enter a first name without invalid characters"
          page.getElementsText("title") should startWith("Error: ")
          verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when last name is not submitted" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (lastNameFieldName -> "")) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your last name"
        page.getElementsText(lastNameFieldLevelErrorXPath) shouldBe "Error: Enter your last name"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when last name is not submitted for Row" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (lastNameFieldName -> ""), location = "eu") { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your family name"
        page.getElementsText(lastNameFieldLevelErrorXPath) shouldBe "Error: Enter your family name"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when last name more than 35 characters" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (lastNameFieldName -> stringContaining36Characters)) {
        result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "The last name must be 35 characters or less"
          page.getElementsText(
            lastNameFieldLevelErrorXPath
          ) shouldBe "Error: The last name must be 35 characters or less"
          page.getElementsText("title") should startWith("Error: ")
          verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when last name has invalid characters" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (lastNameFieldName -> stringContainingInvalidCharacters)) {
        result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter a last name without invalid characters"
          page.getElementsText(
            lastNameFieldLevelErrorXPath
          ) shouldBe "Error: Enter a last name without invalid characters"
          page.getElementsText("title") should startWith("Error: ")
          verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when last name has invalid characters for Row" in {
      submitFormInCreateMode(
        createFormAllFieldsNameDobMap + (lastNameFieldName -> stringContainingInvalidCharacters),
        location = "eu"
      ) {
        result =>
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter a family name without invalid characters"
          page.getElementsText(
            lastNameFieldLevelErrorXPath
          ) shouldBe "Error: Enter a family name without invalid characters"
          page.getElementsText("title") should startWith("Error: ")
          verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when DAY of birth is not submitted" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (dobDayFieldName -> "")) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your date of birth"
        page.getElementsText(dobFieldLevelErrorXPath) shouldBe "Error: Enter your date of birth"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when MONTH of birth is not submitted" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (dobMonthFieldName -> "")) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your date of birth"
        page.getElementsText(dobFieldLevelErrorXPath) shouldBe "Error: Enter your date of birth"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when YEAR of birth is not submitted" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap + (dobYearFieldName -> "")) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your date of birth"
        page.getElementsText(dobFieldLevelErrorXPath) shouldBe "Error: Enter your date of birth"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when date of birth is too early" in {
      submitFormInCreateMode(createFormAllFieldsNameDobTooEarlyMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          pageLevelErrorSummaryListXPath
        ) shouldBe s"Enter a year between 1900 and ${Year.now.getValue}"
        page.getElementsText(
          dobFieldLevelErrorXPath
        ) shouldBe s"Error: Enter a year between 1900 and ${Year.now.getValue}"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when date of birth is tomorrow" in {
      submitFormInCreateMode(createFormAllFieldsNameDobTomorrowMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Date of birth must be in the past"
        page.getElementsText(dobFieldLevelErrorXPath) shouldBe "Error: Date of birth must be in the past"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "validation error when YEAR of birth is next year" in {
      submitFormInCreateMode(createFormAllFieldsNameDobNextYearMap) { result =>
        val page = CdsPage(contentAsString(result))

        page.getElementsText(
          pageLevelErrorSummaryListXPath
        ) shouldBe s"Enter a year between 1900 and ${Year.now.getValue}"
        page.getElementsText(
          dobFieldLevelErrorXPath
        ) shouldBe s"Error: Enter a year between 1900 and ${Year.now.getValue}"
        page.getElementsText("title") should startWith("Error: ")
        verifyZeroInteractions(mockSubscriptionBusinessService)
      }
    }

    "display page level errors when nothing is entered" in {
      submitFormInCreateMode(createEmptyFormNameDobMap) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          pageLevelErrorSummaryListXPath
        ) shouldBe "Enter your first name Enter your last name Enter your date of birth"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "fail when system fails to create details" in {
      registerSaveNameDobDetailsMockFailure(emulatedFailure)

      val caught = intercept[RuntimeException] {
        submitFormInCreateMode(createFormAllFieldsNameDobMap) { result =>
          await(result)
        }
      }

      caught shouldBe emulatedFailure
    }

    "allow resubmission in create mode when details are invalid" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap - dobDayFieldName)(verifyFormActionInCreateMode)
    }

    "redirect to next page when details are valid" in {
      submitFormInCreateMode(createFormAllFieldsNameDobMap)(verifyRedirectToNextPageInCreateMode)
    }
  }

  "submitting the form in review mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.submit(isInReviewMode = true, atarService, Journey.Subscribe)
    )

    "allow resubmission in review mode when details are invalid" in {
      submitFormInReviewMode(createFormAllFieldsNameDobMap - dobDayFieldName)(verifyFormSubmitsInReviewMode)
    }

    "redirect to the review page when details are valid" in {
      submitFormInReviewMode(createFormAllFieldsNameDobMap)(verifyRedirectToReviewPage(Journey.Subscribe))
    }
  }

  val createFormAllFieldsNameDobMap: Map[String, String] = Map(
    firstNameFieldName  -> "Test First Name",
    lastNameFieldName   -> "Test Last Name",
    middleNameFieldName -> "Test Middle Name",
    dobDayFieldName     -> "03",
    dobMonthFieldName   -> "09",
    dobYearFieldName    -> "1983"
  )

  val createFormAllFieldsNameDobTooEarlyMap: Map[String, String] = Map(
    firstNameFieldName  -> "Test First Name",
    lastNameFieldName   -> "Test Last Name",
    middleNameFieldName -> "Test Middle Name",
    dobDayFieldName     -> "03",
    dobMonthFieldName   -> "09",
    dobYearFieldName    -> "1800"
  )

  val createEmptyFormNameDobMap: Map[String, String] = Map(
    firstNameFieldName -> "",
    lastNameFieldName  -> "",
    dobDayFieldName    -> "",
    dobMonthFieldName  -> "",
    dobYearFieldName   -> ""
  )

  def createFormAllFieldsNameDobNextYearMap: Map[String, String] = {
    val todayPlusOneYear = LocalDate.now().plusYears(1)

    Map(
      firstNameFieldName -> "Test First Name",
      lastNameFieldName  -> "Test Last Name",
      dobDayFieldName    -> "03",
      dobMonthFieldName  -> "09",
      dobYearFieldName   -> todayPlusOneYear.toString("YYYY")
    )
  }

  def createFormAllFieldsNameDobTomorrowMap: Map[String, String] = {
    val todayPlusOneDay = LocalDate.now().plusDays(1)
    Map(
      firstNameFieldName -> "Test First Name",
      lastNameFieldName  -> "Test Last Name",
      dobDayFieldName    -> todayPlusOneDay.toString("dd"),
      dobMonthFieldName  -> todayPlusOneDay.toString("MM"),
      dobYearFieldName   -> todayPlusOneDay.toString("YYYY")
    )
  }

  private def mockFunctionWithRegistrationDetails(registrationDetails: RegistrationDetails) {
    when(mockCdsFrontendDataCache.registrationDetails(any[HeaderCarrier]))
      .thenReturn(registrationDetails)
  }

  private def submitFormInCreateMode(
    form: Map[String, String],
    userId: String = defaultUserId,
    location: String = "uk"
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(location))

    val result = controller.submit(isInReviewMode = false, atarService, Journey.Subscribe)(
      SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
    )
    test(result)
  }

  private def submitFormInReviewMode(form: Map[String, String], userId: String = defaultUserId)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)

    val result = controller.submit(isInReviewMode = true, atarService, Journey.Subscribe)(
      SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
    )
    test(result)
  }

  private def showCreateForm(subscriptionFlow: SubscriptionFlow = SoleTraderFlow)(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)

    val result =
      controller.createForm(atarService, Journey.Subscribe).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    test(result)
  }

  private def showReviewForm(subscriptionFlow: SubscriptionFlow = SoleTraderFlow)(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)

    when(mockRequestSessionData.userSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)
    when(mockSubscriptionBusinessService.getCachedSubscriptionNameDobViewModel(any[HeaderCarrier]))
      .thenReturn(Future.successful(NameDobSoleTraderPage.filledValues))

    val result =
      controller.reviewForm(atarService, Journey.Subscribe).apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    test(result)
  }

  private def registerSaveNameDobDetailsMockSuccess() {
    when(mockSubscriptionDetailsHolderService.cacheNameDobDetails(any[NameDobMatchModel])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
  }

  private def registerSaveNameDobDetailsMockFailure(exception: Throwable) {
    when(mockSubscriptionDetailsHolderService.cacheNameDobDetails(any[NameDobMatchModel])(any[HeaderCarrier]))
      .thenReturn(Future.failed(exception))
  }

  private def getDobFromPage(page: CdsPage): String =
    (("0000" + page.getElementValue(dateOfBirthYearFieldXPath)) takeRight 4) + '-' + (("00" + page.getElementValue(
      dateOfBirthMonthFieldXPath
    )) takeRight 2) + '-' + (("00" + page.getElementValue(dateOfBirthDayFieldXPath)) takeRight 2)

}
