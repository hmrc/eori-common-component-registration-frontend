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

import java.util.UUID
import common.pages.subscription.DisclosePersonalDetailsConsentPage
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.DisclosePersonalDetailsConsentController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.DisclosePersonalDetailsConsentViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.disclose_personal_details_consent
import uk.gov.hmrc.http.HeaderCarrier
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
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DisclosePersonalDetailsConsentController
      .submit(isInReviewMode = false, atarService)
      .url

  protected override val submitInReviewModeUrl: String =
    uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DisclosePersonalDetailsConsentController
      .submit(isInReviewMode = true, atarService)
      .url

  private val disclosePersonalDetailsConsentView = instanceOf[disclose_personal_details_consent]

  private val mockDisclosePersonalDetailsConsentViewModel = mock[DisclosePersonalDetailsConsentViewModel]

  private val problemWithSelectionError =
    "Select yes to show your name and address on the Check an EORI number service."

  private val yesNoInputName = "yes-no-answer"

  private val mockRequestSessionData = mock[RequestSessionData]

  private val controller = new DisclosePersonalDetailsConsentController(
    mockAuthAction,
    mockSubscriptionDetailsService,
    mockSubscriptionBusinessService,
    mockRequestSessionData,
    mcc,
    disclosePersonalDetailsConsentView,
    mockSubscriptionFlowManager,
    mockDisclosePersonalDetailsConsentViewModel
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
        "‘Check an EORI number’ service",
        "mocked paragraph 2",
        "Yes",
        "No",
        false,
        true,
        false,
        false
      ),
      (
        ThirdCountryOrganisationSubscriptionFlow,
        "‘Check an EORI number’ service",
        "mocked paragraph 2",
        "Yes",
        "No",
        false,
        false,
        false,
        false
      ),
      (
        PartnershipSubscriptionFlow,
        "‘Check an EORI number’ service",
        "mocked paragraph 2",
        "Yes",
        "No",
        true,
        false,
        true,
        false
      ),
      (
        OrganisationSubscriptionFlow,
        "‘Check an EORI number’ service",
        "mocked paragraph 2",
        "Yes",
        "No",
        true,
        false,
        false,
        true
      ),
      (
        SoleTraderSubscriptionFlow,
        "‘Check an EORI number’ service",
        "mocked paragraph 2",
        "Yes",
        "No",
        true,
        true,
        false,
        false
      )
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionDetailsService.cacheConsentToDisclosePersonalDetails(any[YesNo])(any[Request[_]]))
      .thenReturn(Future.successful {})
    setupMockSubscriptionFlowManager(EoriConsentSubscriptionFlowPage)

    when(mockDisclosePersonalDetailsConsentViewModel.textPara2()(any[Messages], any[Request[AnyContent]])).thenReturn(
      "mocked paragraph 2"
    )
  }

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionDetailsService)
    reset(mockSubscriptionFlowManager)
    reset(mockAuthConnector)
    reset(mockRequestSessionData)

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
            page.getElementsText(DisclosePersonalDetailsConsentPage.consentInfoPara2Xpath) should include(consentInfo)
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
        val orgType =
          if (isIndividual) CdsOrganisationType.Individual
          else if (isPartnership) CdsOrganisationType.Partnership
          else if (isCharity) CdsOrganisationType.CharityPublicBodyNotForProfit
          else if (!isUkJourney) CdsOrganisationType.ThirdCountryOrganisation
          else CdsOrganisationType.Company
        when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(orgType))

        s"display the form for subscription flow $subscriptionFlow" in {
          showReviewForm(
            subscriptionFlow = subscriptionFlow,
            isUkJourney = isUkJourney,
            isIndividual = isIndividual,
            isPartnership = isPartnership,
            isCharity = isCharity
          ) { result =>
            val orgType =
              if (isIndividual) CdsOrganisationType.Individual
              else if (isPartnership) CdsOrganisationType.Partnership
              else if (isCharity) CdsOrganisationType.CharityPublicBodyNotForProfit
              else if (!isUkJourney) CdsOrganisationType.ThirdCountryOrganisation
              else CdsOrganisationType.Company
            when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(orgType))
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
            val orgType =
              if (isIndividual) CdsOrganisationType.Individual
              else if (isPartnership) CdsOrganisationType.Partnership
              else if (isCharity) CdsOrganisationType.CharityPublicBodyNotForProfit
              else if (!isUkJourney) CdsOrganisationType.ThirdCountryOrganisation
              else CdsOrganisationType.Company
            when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(orgType))

            status(result) shouldBe OK
            val page = CdsPage(contentAsString(result))
            page.getElementsText(DisclosePersonalDetailsConsentPage.consentInfoPara2Xpath) should include(consentInfo)
            page.getElementsText(DisclosePersonalDetailsConsentPage.yesToDiscloseXpath) shouldBe yesLabel
            page.getElementsText(DisclosePersonalDetailsConsentPage.noToDiscloseXpath) shouldBe noLabel
          }
        }
    }

    "Loading the page in create mode" should {

      assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

      "set form action url to submit in create mode" in {
        showCreateForm()(verifyFormActionInCreateMode)
      }

      "display the 'Back' link according the current subscription flow" in {
        showCreateForm()(verifyBackLinkInCreateModeRegister)
      }
    }

    "Loading the page in review mode" should {

      assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.reviewForm(atarService))

      "set form action url to submit in review mode" in {
        showReviewForm()(verifyFormSubmitsInReviewMode)
      }

      "not display the number of steps and display the 'Back' link to review page" in {
        showReviewForm()(verifyNoStepsAndBackLinkInReviewMode)
      }

      "display yes when the user's previous answer of yes is in the cache" in {
        showReviewForm(previouslyAnswered = true) { result =>
          val page = CdsPage(contentAsString(result))
          page.radioButtonChecked(DisclosePersonalDetailsConsentPage.noToDiscloseInputXpath) shouldBe false
          page.radioButtonChecked(DisclosePersonalDetailsConsentPage.yesToDiscloseInputXpath) shouldBe true
        }
      }

      "display no when the user's previous answer of no is in the cache" in {
        showReviewForm(previouslyAnswered = false) { result =>
          val page = CdsPage(contentAsString(result))
          page.radioButtonChecked(DisclosePersonalDetailsConsentPage.noToDiscloseInputXpath) shouldBe true
          page.radioButtonChecked(DisclosePersonalDetailsConsentPage.yesToDiscloseInputXpath) shouldBe false
        }
      }

      "display the correct text for the continue button" in {
        showReviewForm() { result =>
          val page = CdsPage(contentAsString(result))
          page.getElementText(
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
        controller.submit(isInReviewMode = true, atarService)
      )

      "allow resubmission in review mode when details are invalid" in {
        submitFormInReviewMode(ValidRequest - yesNoInputName)(verifyFormSubmitsInReviewMode)
      }

      "redirect to review page when details are valid" in {
        submitFormInReviewMode(ValidRequest)(verifyRedirectToReviewPage())
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
  }

  private def showCreateForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    userId: String = defaultUserId,
    isUkJourney: Boolean = true,
    isIndividual: Boolean = false,
    isPartnership: Boolean = false,
    isCharity: Boolean = false
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(subscriptionFlow)
    )
    when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(isUkJourney)
    when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(isIndividual)
    when(mockRequestSessionData.isPartnershipOrLLP(any())).thenReturn(isPartnership)
    when(mockRequestSessionData.isCharity(any())).thenReturn(isCharity)

    val orgType =
      if (isIndividual) CdsOrganisationType.Individual
      else if (isPartnership) CdsOrganisationType.Partnership
      else if (isCharity) CdsOrganisationType.CharityPublicBodyNotForProfit
      else if (!isUkJourney) CdsOrganisationType.ThirdCountryOrganisation
      else CdsOrganisationType.Company
    when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(orgType))

    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def showReviewForm(
    subscriptionFlow: SubscriptionFlow = OrganisationSubscriptionFlow,
    previouslyAnswered: Boolean = true,
    userId: String = defaultUserId,
    isUkJourney: Boolean = true,
    isIndividual: Boolean = false,
    isPartnership: Boolean = false,
    isCharity: Boolean = false
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    when(mockSubscriptionBusinessService.getCachedPersonalDataDisclosureConsent(any[Request[_]]))
      .thenReturn(previouslyAnswered)
    when(mockRequestSessionData.isRegistrationUKJourney(any())).thenReturn(isUkJourney)
    when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(isIndividual)
    when(mockRequestSessionData.isPartnershipOrLLP(any())).thenReturn(isPartnership)
    when(mockRequestSessionData.isCharity(any())).thenReturn(isCharity)

    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier])).thenReturn(
      Right(subscriptionFlow)
    )

    test(controller.reviewForm(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

  private def submitFormInCreateMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = false)(test)

  private def submitFormInReviewMode(form: Map[String, String])(test: Future[Result] => Any): Unit =
    submitForm(form, isInReviewMode = true)(test)

  private def submitForm(form: Map[String, String], isInReviewMode: Boolean = false, userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionFlowManager.currentSubscriptionFlow(any[Request[AnyContent]], any[HeaderCarrier]))
      .thenReturn(Right(OrganisationSubscriptionFlow))

    when(mockRequestSessionData.userSelectedOrganisationType(any())).thenReturn(Some(CdsOrganisationType.Company))

    test(
      controller
        .submit(isInReviewMode, atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

}
