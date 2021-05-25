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

import java.util.UUID

import common.pages.subscription.DisclosePersonalDetailsConsentPage
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.DisclosePersonalDetailsConsentController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.YesNo
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.disclose_personal_details_consent
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder
import util.builders.YesNoFormBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DisclosePersonalDetailsConsentControllerSpec
    extends ControllerSpec with SubscriptionFlowSpec with MockitoSugar with BeforeAndAfterEach {

  protected override val formId: String = DisclosePersonalDetailsConsentPage.formId

  protected override val submitInCreateModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.DisclosePersonalDetailsConsentController
      .submit(isInReviewMode = false, atarService, Journey.Register)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.DisclosePersonalDetailsConsentController
      .submit(isInReviewMode = true, atarService, Journey.Register)
      .url

  private val disclosePersonalDetailsConsentView = instanceOf[disclose_personal_details_consent]

  private val problemWithSelectionError = "Tell us if you want to include your name and address on the EORI checker"
  private val yesNoInputName            = "yes-no-answer"

  private val mockRequestSessionData = mock[RequestSessionData]

  private val controller = new DisclosePersonalDetailsConsentController(
    mockAuthAction,
    mockSubscriptionDetailsHolderService,
    mockSubscriptionBusinessService,
    mockRequestSessionData,
    mcc,
    disclosePersonalDetailsConsentView,
    mockSubscriptionFlowManager
  )

  private val subscriptionFlows =
    Table[SubscriptionFlow, String, String, String, String, Boolean, Boolean, Boolean, Boolean](
      (
        "Flow name",
        "Title",
        "Consent Info",
        "Yes Label",
        "No Label",
        "is UK journey",
        "is individual",
        "is partnership",
        "is charity"
      ),
      (
        ThirdCountryIndividualSubscriptionFlow,
        "Do you want to include your name and address on the EORI checker?",
        "HMRC will add your EORI number to a public checker kept by the European Commission. You can also agree to include your name and address. This can help customs and freight agents identify you and process your shipments.",
        "Yes - I want the name and address on the EORI checker",
        "No - Just show my EORI number",
        false,
        true,
        false,
        false
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        "Do you want to include the organisation name and address on the EORI checker?",
        "HMRC will add your GB EORI number to a public checker. You can also include you or your organisation's name and address. This will help customs and freight agents identify you and process your shipments.",
        "Yes - I want the name and address on the EORI checker",
        "No - Just show my EORI number",
        false,
        false,
        false,
        false
      ),
      (
        PartnershipSubscriptionFlow,
        "Do you want to include the partnership name and address on the EORI checker?",
        "HMRC will add your GB EORI number to a public checker. You can also include you or your organisation's name and address. This will help customs and freight agents identify you and process your shipments.",
        "Yes - I want the name and address on the EORI checker",
        "No - Just show my EORI number",
        true,
        false,
        true,
        false
      ),
      (
        OrganisationSubscriptionFlow,
        "Do you want to include the charity name and address on the EORI checker?",
        "HMRC will add your GB EORI number to a public checker. You can also include you or your organisation's name and address. This will help customs and freight agents identify you and process your shipments.",
        "Yes - I want the name and address on the EORI checker",
        "No - Just show my EORI number",
        true,
        false,
        false,
        true
      ),
      (
        OrganisationFlow, // Flow cannot be duplicated in this table and we do not have enough flows, using this to progress with tests
        "Do you want to include the company name and address on the EORI checker?",
        "HMRC will add your GB EORI number to a public checker. You can also include you or your organisation's name and address. This will help customs and freight agents identify you and process your shipments.",
        "Yes - I want the name and address on the EORI checker",
        "No - Just show my EORI number",
        true,
        false,
        false,
        false
      )
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsHolderService.cacheConsentToDisclosePersonalDetails(any[YesNo])(any[HeaderCarrier]))
      .thenReturn(Future.successful {})
    setupMockSubscriptionFlowManager(EoriConsentSubscriptionFlowPage)
  }

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionDetailsHolderService, mockSubscriptionFlowManager, mockAuthConnector, mockRequestSessionData)

    super.afterEach()
  }

  s"Loading the page in Create mode" should {

    forAll(subscriptionFlows) {
      case (
            subscriptionFlow,
            title,
            consentInfo,
            yesLabel,
            noLabel,
            isUkJourney,
            isIndividual,
            isPartnership,
            isCharity
          ) =>
        s"display the form for subscription flow $subscriptionFlow" in {
          showCreateForm(
            subscriptionFlow = subscriptionFlow,
            isUkJourney = isUkJourney,
            isIndividual = isIndividual,
            isPartnership = isPartnership,
            isCharity = isCharity
          ) { result =>
            status(result) shouldBe OK
            val html: String = contentAsString(result)
            html should include("id=\"yes-no-answer-true\"")
            html should include("id=\"yes-no-answer-false\"")
            html should include(title)
          }
        }

        s"display proper labels for subscription flow $subscriptionFlow" in {
          showCreateForm(
            subscriptionFlow = subscriptionFlow,
            isUkJourney = isUkJourney,
            isIndividual = isIndividual,
            isPartnership = isPartnership,
            isCharity = isCharity
          ) { result =>
            status(result) shouldBe OK
            val page = CdsPage(contentAsString(result))
            page.getElementsText(DisclosePersonalDetailsConsentPage.consentInfoXpath) shouldBe consentInfo
            page.getElementsText(DisclosePersonalDetailsConsentPage.yesToDiscloseXpath) shouldBe yesLabel
            page.getElementsText(DisclosePersonalDetailsConsentPage.noToDiscloseXpath) shouldBe noLabel
          }
        }
    }
  }

  s"Loading the page in Review mode" should {

    forAll(subscriptionFlows) {
      case (
            subscriptionFlow,
            title,
            consentInfo,
            yesLabel,
            noLabel,
            isUkJourney,
            isIndividual,
            isPartnership,
            isCharity
          ) =>
        s"display the form for subscription flow $subscriptionFlow" in {
          showReviewForm(
            subscriptionFlow = subscriptionFlow,
            isUkJourney = isUkJourney,
            isIndividual = isIndividual,
            isPartnership = isPartnership,
            isCharity = isCharity
          ) { result =>
            status(result) shouldBe OK
            val html: String = contentAsString(result)
            html should include("id=\"yes-no-answer-true\"")
            html should include("id=\"yes-no-answer-false\"")
            html should include(title)
          }
        }

        s"display proper labels for subscription flow $subscriptionFlow" in {
          showReviewForm(
            subscriptionFlow = subscriptionFlow,
            isUkJourney = isUkJourney,
            isIndividual = isIndividual,
            isPartnership = isPartnership,
            isCharity = isCharity
          ) { result =>
            status(result) shouldBe OK
            val page = CdsPage(contentAsString(result))
            page.getElementsText(DisclosePersonalDetailsConsentPage.consentInfoXpath) shouldBe consentInfo
            page.getElementsText(DisclosePersonalDetailsConsentPage.yesToDiscloseXpath) shouldBe yesLabel
            page.getElementsText(DisclosePersonalDetailsConsentPage.noToDiscloseXpath) shouldBe noLabel
          }
        }
    }
  }

  "Loading the page in create mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.createForm(atarService, Journey.Register)
    )

    "set form action url to submit in create mode" in {
      showCreateForm()(verifyFormActionInCreateMode)
    }

    "display the 'Back' link according the current subscription flow" in {
      showCreateForm()(verifyBackLinkInCreateModeRegister)
    }
  }

  "Loading the page in review mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.reviewForm(atarService, Journey.Register)
    )

    "set form action url to submit in review mode" in {
      showReviewForm()(verifyFormSubmitsInReviewMode)
    }

    "not display the number of steps and display the 'Back' link to review page" in {
      showReviewForm()(verifyNoStepsAndBackLinkInReviewMode)
    }

    "display yes when the user's previous answer of yes is in the cache" in {
      showReviewForm(previouslyAnswered = true) { result =>
        val page = CdsPage(contentAsString(result))
        page.radioButtonIsChecked(DisclosePersonalDetailsConsentPage.noToDiscloseInputXpath) shouldBe false
        page.radioButtonIsChecked(DisclosePersonalDetailsConsentPage.yesToDiscloseInputXpath) shouldBe true
      }
    }

    "display no when the user's previous answer of no is in the cache" in {
      showReviewForm(previouslyAnswered = false) { result =>
        val page = CdsPage(contentAsString(result))
        page.radioButtonIsChecked(DisclosePersonalDetailsConsentPage.noToDiscloseInputXpath) shouldBe true
        page.radioButtonIsChecked(DisclosePersonalDetailsConsentPage.yesToDiscloseInputXpath) shouldBe false
      }
    }

    "display the correct text for the continue button" in {
      showReviewForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementValue(
          DisclosePersonalDetailsConsentPage.continueButtonXpath
        ) shouldBe ContinueButtonTextInReviewMode
      }
    }
  }

  "The Yes No Radio Button " should {
    "display a relevant error if no option is chosen" in {
      submitForm(ValidRequest - yesNoInputName) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          DisclosePersonalDetailsConsentPage.pageLevelErrorSummaryListXPath
        ) shouldBe problemWithSelectionError
        page.getElementsText(
          DisclosePersonalDetailsConsentPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: $problemWithSelectionError"
      }
    }

    "display a relevant error if an invalid answer option is selected" in {
      val invalidOption = UUID.randomUUID.toString
      submitForm(ValidRequest + (yesNoInputName -> invalidOption)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          DisclosePersonalDetailsConsentPage.pageLevelErrorSummaryListXPath
        ) shouldBe problemWithSelectionError
        page.getElementsText(
          DisclosePersonalDetailsConsentPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: $problemWithSelectionError"
      }
    }
  }

  "Submitting in Review Mode" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(isInReviewMode = true, atarService, Journey.Register)
    )

    "allow resubmission in review mode when details are invalid" in {
      submitFormInReviewMode(ValidRequest - yesNoInputName)(verifyFormSubmitsInReviewMode)
    }

    "redirect to review page when details are valid" in {
      submitFormInReviewMode(ValidRequest)(verifyRedirectToReviewPage(Journey.Register))
    }
  }

  "Submitting in Create Mode" should {

    "allow resubmission in create mode when details are invalid" in {
      submitFormInCreateMode(ValidRequest - yesNoInputName)(verifyFormActionInCreateMode)
    }

    "redirect to next page when details are valid" in {
      submitFormInCreateMode(ValidRequest)(verifyRedirectToNextPageInCreateMode)
    }
  }

  private def showCreateForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    userId: String = defaultUserId,
    journey: Journey.Value = Journey.Register,
    isUkJourney: Boolean = true,
    isIndividual: Boolean = false,
    isPartnership: Boolean = false,
    isCharity: Boolean = false
  )(test: (Future[Result]) => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)
    when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(isUkJourney)
    when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(isIndividual)
    when(mockRequestSessionData.isPartnership(any())).thenReturn(isPartnership)
    when(mockRequestSessionData.isCharity(any())).thenReturn(isCharity)

    test(controller.createForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def showReviewForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    previouslyAnswered: Boolean = true,
    userId: String = defaultUserId,
    journey: Journey.Value = Journey.Register,
    isUkJourney: Boolean = true,
    isIndividual: Boolean = false,
    isPartnership: Boolean = false,
    isCharity: Boolean = false
  )(test: (Future[Result]) => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionBusinessService.getCachedPersonalDataDisclosureConsent(any[HeaderCarrier]))
      .thenReturn(previouslyAnswered)
    when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(isUkJourney)
    when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(isIndividual)
    when(mockRequestSessionData.isPartnership(any())).thenReturn(isPartnership)
    when(mockRequestSessionData.isCharity(any())).thenReturn(isCharity)

    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]])).thenReturn(subscriptionFlow)

    test(controller.reviewForm(atarService, journey).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitFormInCreateMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = false)(test)

  private def submitFormInReviewMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = true)(test)

  private def submitForm(
    form: Map[String, String],
    isInReviewMode: Boolean = false,
    userId: String = defaultUserId,
    journey: Journey.Value = Journey.Register
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]]))
      .thenReturn(OrganisationSubscriptionFlow)
    test(
      controller
        .submit(isInReviewMode, atarService, journey)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

}
