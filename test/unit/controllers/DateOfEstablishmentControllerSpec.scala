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

import common.pages.subscription.SubscriptionContactDetailsPage._
import common.pages.subscription.{
  SubscriptionDateOfBirthPage,
  SubscriptionDateOfEstablishmentPage,
  SubscriptionPartnershipDateOfEstablishmentPage
}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.DateOfEstablishmentController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  DateOfEstablishmentSubscriptionFlowPage,
  SubscriptionDetails
}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.FlowError.FlowNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.date_of_establishment
import uk.gov.hmrc.http.HeaderCarrier
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DateOfEstablishmentControllerSpec
    extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport
    with SubscriptionFlowReviewModeTestSupport {

  protected override val formId: String = SubscriptionDateOfBirthPage.formId

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DateOfEstablishmentController
      .submit(isInReviewMode = false, atarService)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DateOfEstablishmentController
      .submit(isInReviewMode = true, atarService)
      .url

  private val mockOrgTypeLookup       = mock[OrgTypeLookup]
  private val mockRequestSessionData  = mock[RequestSessionData]
  private val mockSessionCacheService = instanceOf[SessionCacheService]

  private val dateOfEstablishmentView = instanceOf[date_of_establishment]

  private val controller = new DateOfEstablishmentController(
    mockAuthAction,
    mockSubscriptionFlowManager,
    mockSubscriptionBusinessService,
    mockSubscriptionDetailsService,
    mockRequestSessionData,
    mcc,
    dateOfEstablishmentView,
    mockOrgTypeLookup,
    mockSessionCacheService
  )(global)

  private val DateOfEstablishmentString = "1962-05-12"
  private val DateOfEstablishment       = LocalDate.parse(DateOfEstablishmentString)

  private val ValidRequest = Map(
    "date-of-establishment.day"   -> DateOfEstablishment.getDayOfMonth.toString,
    "date-of-establishment.month" -> DateOfEstablishment.getMonthValue.toString,
    "date-of-establishment.year"  -> DateOfEstablishment.getYear.toString
  )

  val existingSubscriptionDetailsHolder: SubscriptionDetails = SubscriptionDetails()

  private val DateOfEstablishmentMissingErrorPage     = "Enter your date of establishment"
  private val DateOfEstablishmentMissingErrorField    = "Error: Enter your date of establishment"
  private val DateOfEstablishmentInvalidDayErrorPage  = messages("date.day.error")
  private val DateOfEstablishmentInvalidDayErrorField = s"Error: $DateOfEstablishmentInvalidDayErrorPage"
  private val DateOfEstablishmentInFutureErrorPage    = "Year must be between 1000 and this year"
  private val DateOfEstablishmentInFutureErrorField   = "Error: Year must be between 1000 and this year"

  override protected def beforeEach(): Unit = {
    reset(mockSubscriptionFlowManager)
    reset(mockSubscriptionBusinessService)
    reset(mockSubscriptionDetailsService)
    setupMockSubscriptionFlowManager(DateOfEstablishmentSubscriptionFlowPage)
    when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(CorporateBody))
  }

  val formModes: TableFor2[String, Map[String, String] => (Future[Result] => Any) => Unit] = Table(
    ("formMode", "submitFormFunction"),
    ("create", (form: Map[String, String]) => submitFormInCreateMode(form) _),
    ("review", (form: Map[String, String]) => submitFormInReviewMode(form) _)
  )

  "Loading the page in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "display the form" in {
      showCreateForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.elementIsPresent(SubscriptionDateOfEstablishmentPage.dayOfDateFieldXpath) shouldBe true
        page.elementIsPresent(SubscriptionDateOfEstablishmentPage.monthOfDateFieldXpath) shouldBe true
        page.elementIsPresent(SubscriptionDateOfEstablishmentPage.yearOfDateFieldXpath) shouldBe true
      }
    }

    "set form action url to submit in create mode" in {
      showCreateForm()(verifyFormActionInCreateMode)
    }

    "display the number of 'Back' link according the current subscription flow" in {
      showCreateForm()(verifyBackLinkInCreateModeRegister)
    }

    "have all the required input fields without data if not cached previously" in {
      showCreateForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(SubscriptionDateOfEstablishmentPage.dayOfDateFieldXpath) shouldBe Symbol("empty")
        page.getElementValue(SubscriptionDateOfEstablishmentPage.monthOfDateFieldXpath) shouldBe Symbol("empty")
        page.getElementValue(SubscriptionDateOfEstablishmentPage.yearOfDateFieldXpath) shouldBe Symbol("empty")
      }
    }

    "have all the required input fields with data if cached previously" in {
      showCreateForm(cachedDate = Some(DateOfEstablishment)) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(
          SubscriptionDateOfEstablishmentPage.dayOfDateFieldXpath
        ) shouldBe DateOfEstablishment.getDayOfMonth.toString
        page.getElementValue(
          SubscriptionDateOfEstablishmentPage.monthOfDateFieldXpath
        ) shouldBe DateOfEstablishment.getMonthValue.toString
        page.getElementValue(
          SubscriptionDateOfEstablishmentPage.yearOfDateFieldXpath
        ) shouldBe DateOfEstablishment.getYear.toString
      }
    }

    "use partnership in title and heading for Partnership Org Type" in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(Partnership))
      showCreateForm(cachedDate = Some(DateOfEstablishment)) { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("When was the partnership established?")
        page.getElementsText(
          SubscriptionPartnershipDateOfEstablishmentPage.dateOfEstablishmentHeadingXPath
        ) shouldBe "When was the partnership established?"
      }
    }

    "use partnership in title and heading for Limited Liability Partnership Org Type" in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(LLP))
      showCreateForm(cachedDate = Some(DateOfEstablishment)) { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("When was the partnership established?")
        page.getElementsText(
          SubscriptionDateOfEstablishmentPage.dateOfEstablishmentHeadingXPath
        ) shouldBe "When was the partnership established?"
      }
    }

    "use business in Date of Establishment title and heading for non-partnership Org Type" in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]]))
        .thenReturn(Future.successful(UnincorporatedBody))
      showCreateForm(cachedDate = Some(DateOfEstablishment)) { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("When was the organisation established?")
        page.getElementsText(
          SubscriptionDateOfEstablishmentPage.dateOfEstablishmentHeadingXPath
        ) shouldBe "When was the organisation established?"
      }
    }

    "use business in Date of Establishment text and organisation in title and heading for Company Org Type" in {
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(CorporateBody))
      showCreateForm(cachedDate = Some(DateOfEstablishment)) { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("When was the company established?")
        page.getElementsText(
          SubscriptionDateOfEstablishmentPage.dateOfEstablishmentHeadingXPath
        ) shouldBe "When was the company established?"
      }
    }
  }

  "Loading the page in review mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewForm(atarService))

    "display the form" in {
      showReviewForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.elementIsPresent(SubscriptionDateOfEstablishmentPage.dayOfDateFieldXpath) shouldBe true
        page.elementIsPresent(SubscriptionDateOfEstablishmentPage.monthOfDateFieldXpath) shouldBe true
        page.elementIsPresent(SubscriptionDateOfEstablishmentPage.yearOfDateFieldXpath) shouldBe true
      }
    }

    "set form action url to submit in review mode" in {
      showReviewForm()(verifyFormSubmitsInReviewMode)
    }

    "not display the 'Back' link to review page" in {
      showReviewForm()(verifyBackLinkInReviewMode)
    }

    "display relevant data in form fields when subscription details exist in the cache" in {
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(
          SubscriptionDateOfEstablishmentPage.dayOfDateFieldXpath
        ) shouldBe DateOfEstablishment.getDayOfMonth.toString
        page.getElementValue(
          SubscriptionDateOfEstablishmentPage.monthOfDateFieldXpath
        ) shouldBe DateOfEstablishment.getMonthValue.toString
        page.getElementValue(
          SubscriptionDateOfEstablishmentPage.yearOfDateFieldXpath
        ) shouldBe DateOfEstablishment.getYear.toString
      }
    }

    "display the correct text for the continue button" in {
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementText(continueButtonXpath) shouldBe ContinueButtonTextInReviewMode
      }
    }

  }

  forAll(formModes) { (formMode, submitFormFunction) =>
    s"date of establishment in $formMode mode" should {

      "be mandatory" in {
        submitFormFunction(
          ValidRequest ++ Map(
            "date-of-establishment.day"   -> "",
            "date-of-establishment.month" -> "",
            "date-of-establishment.year"  -> ""
          )
        ) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(
            SubscriptionDateOfEstablishmentPage.pageLevelErrorSummaryListXPath
          ) shouldBe DateOfEstablishmentMissingErrorPage
          page.getElementsText(
            SubscriptionDateOfEstablishmentPage.dateOfEstablishmentErrorXpath
          ) shouldBe DateOfEstablishmentMissingErrorField
        }
      }

      "be a valid date" in {
        submitFormFunction(ValidRequest + ("date-of-establishment.day" -> "32")) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(
            SubscriptionDateOfEstablishmentPage.pageLevelErrorSummaryListXPath
          ) shouldBe DateOfEstablishmentInvalidDayErrorPage
          page.getElementsText(
            SubscriptionDateOfEstablishmentPage.dateOfEstablishmentErrorXpath
          ) shouldBe DateOfEstablishmentInvalidDayErrorField
        }
      }

      "not be a future date" in {
        val tomorrow = LocalDate.now().plusDays(1)
        submitFormFunction(
          ValidRequest ++ Map(
            "date-of-establishment.day"   -> tomorrow.getDayOfMonth.toString,
            "date-of-establishment.month" -> tomorrow.getMonthValue.toString,
            "date-of-establishment.year"  -> tomorrow.getYear.toString
          )
        ) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(
            SubscriptionDateOfEstablishmentPage.pageLevelErrorSummaryListXPath
          ) shouldBe DateOfEstablishmentInFutureErrorPage
          page.getElementsText(
            SubscriptionDateOfEstablishmentPage.dateOfEstablishmentErrorXpath
          ) shouldBe DateOfEstablishmentInFutureErrorField
        }
      }
    }

    s"Submitting the form in $formMode mode" should {

      "capture date of birth entered by user" in {
        submitFormInCreateMode(ValidRequest) { result =>
          await(result)
          verify(mockSubscriptionDetailsService).cacheDateEstablished(meq(DateOfEstablishment))(any[Request[_]])
        }
      }
    }
  }

  "Submitting the invalid form in create mode" should {

    "allow resubmission in create mode" in {
      submitFormInCreateMode(ValidRequest - "date-of-establishment.day")(verifyFormActionInCreateMode)
    }
  }

  "Submitting the valid form in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService)
    )

    "redirect to next page in subscription flow" in {
      submitFormInCreateMode(ValidRequest)(verifyRedirectToNextPageInCreateMode)
    }

    "redirect to start of the application" in {
      when(mockSubscriptionFlowManager.stepInformation(any())(any[Request[AnyContent]], any[HeaderCarrier]))
        .thenReturn(Left(FlowNotFound()))

      submitFormInCreateMode(ValidRequest) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe "/customs-registration-services/atar/register"
      }
    }
  }

  "Submitting the valid form in review mode" should {

    "redirect to the review page" in {
      submitFormInReviewMode(ValidRequest)(verifyRedirectToReviewPage())
    }
  }

  "Submitting the invalid form in review mode" should {

    "allow resubmission in review mode" in {
      submitFormInReviewMode(ValidRequest - "date-of-establishment.day")(verifyFormSubmitsInReviewMode)
    }
  }

  private def showCreateForm(userId: String = defaultUserId, cachedDate: Option[LocalDate] = None)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionBusinessService.maybeCachedDateEstablished(any[Request[_]]))
      .thenReturn(Future.successful(cachedDate))

    val result = controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showReviewForm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionBusinessService.getCachedDateEstablished(any[Request[_]])).thenReturn(
      Future.successful(DateOfEstablishment)
    )

    val result = controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def submitFormInCreateMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = false)(test)

  private def submitFormInReviewMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = true)(test)

  private def submitForm(form: Map[String, String], isInReviewMode: Boolean, userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionDetailsService.cacheDateEstablished(any[LocalDate])(any[Request[_]]))
      .thenReturn(Future.successful(()))
    val result = controller
      .submit(isInReviewMode, atarService)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

}
