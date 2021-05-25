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

import common.pages.subscription.ConfirmIndividualTypePage._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.{
  ConfirmIndividualTypeController,
  SubscriptionFlowManager
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionPage
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.confirm_individual_type
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ConfirmIndividualTypeControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector           = mock[AuthConnector]
  private val mockAuthAction              = authAction(mockAuthConnector)
  private val mockRequestSessionData      = mock[RequestSessionData]
  private val mockSubscriptionFlowManager = mock[SubscriptionFlowManager]
  private val confirmIndividualTypeView   = instanceOf[confirm_individual_type]

  private val controller = new ConfirmIndividualTypeController(
    mockAuthAction,
    mockRequestSessionData,
    mockSubscriptionFlowManager,
    confirmIndividualTypeView,
    mcc
  )

  private val mockSubscriptionPage = mock[SubscriptionPage]
  private val mockSession          = mock[Session]
  private val anotherMockSession   = mock[Session]
  private val mockRequestHeader    = mock[RequestHeader]
  private val mockFlowStart        = (mockSubscriptionPage, anotherMockSession)

  private val testSessionData              = Map[String, String]("some_session_key" -> "some_session_value")
  private val testSubscriptionStartPageUrl = "some_page_url"

  private val ErrorSelectSoleTraderOrIndividual = "Select sole trader or individual"

  private val selectedIndividualType = CdsOrganisationType.Individual
  private val validRequestData       = Map("individual-type" -> selectedIndividualType.id)

  before {
    reset(mockRequestSessionData)
  }

  "Viewing the selection form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(atarService, Journey.Register)
    )

    "show the page without errors" in showForm { result =>
      status(result) shouldBe OK
      val page = CdsPage(contentAsString(result))
      page.elementIsPresent(pageLevelErrorSummaryListXPath) shouldBe false
      assertRadioButtonIsPresent(page, soleTraderLabelXpath, "Sole trader", "sole-trader")
      assertRadioButtonIsPresent(page, individualLabelXpath, "Individual", "individual")
      page.radioButtonIsUnchecked(optionSoleTraderXpath) shouldBe true
      page.radioButtonIsUnchecked(optionIndividualXpath) shouldBe true

      page.getElementAttributeHref(backLinkXPath) shouldBe previousPageUrl
      page.formAction(
        formId
      ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.ConfirmIndividualTypeController
        .submit(atarService, Journey.Register)
        .url
    }

    "clear any selected organisation type stored in the session" in {
      showForm { result =>
        val awaitedResult = await(result)
        verify(mockRequestSessionData).sessionWithoutOrganisationType(ArgumentMatchers.any[Request[AnyContent]])
        awaitedResult.session(mockRequestHeader).data should contain(testSessionData.head)
      }
    }
  }

  "Radio button selection" should {

    "be mandatory" in submitForm(validRequestData - "individual-type") { result =>
      status(result) shouldBe BAD_REQUEST
      val page = CdsPage(contentAsString(result))
      page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe ErrorSelectSoleTraderOrIndividual
      page.getElementsText(fieldLevelErrorIndividualTypeXPath) shouldBe s"Error: $ErrorSelectSoleTraderOrIndividual"
      page.getElementsText("title") should startWith("Error: ")
    }

    "reject wrong options" in submitForm(validRequestData + ("individual-type" -> "invalid")) { result =>
      status(result) shouldBe BAD_REQUEST
      val page = CdsPage(contentAsString(result))
      page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe ErrorSelectSoleTraderOrIndividual
      page.getElementsText(fieldLevelErrorIndividualTypeXPath) shouldBe s"Error: $ErrorSelectSoleTraderOrIndividual"
      page.getElementsText("title") should startWith("Error: ")
    }
  }

  "Submitting the correct form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(atarService, Journey.Register)
    )

    "redirect to subscription flow first page with updated session" in {
      submitForm(validRequestData) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe testSubscriptionStartPageUrl
        verify(mockSubscriptionFlowManager).startSubscriptionFlow(
          ArgumentMatchers.any[Option[SubscriptionPage]],
          ArgumentMatchers.eq(selectedIndividualType),
          ArgumentMatchers.eq(atarService),
          ArgumentMatchers.eq(Journey.Register)
        )(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[Request[AnyContent]])
        verify(mockRequestSessionData).sessionWithOrganisationTypeAdded(
          ArgumentMatchers.eq(anotherMockSession),
          ArgumentMatchers.eq(selectedIndividualType)
        )
        testSessionData foreach (
          newSessionValue => result.session(mockRequestHeader).data should contain(newSessionValue)
        )
      }
    }
  }

  private def showForm(test: Future[Result] => Any) {
    val aUserId = defaultUserId
    withAuthorisedUser(aUserId, mockAuthConnector)

    when(mockSession.data).thenReturn(testSessionData)
    when(mockRequestSessionData.sessionWithoutOrganisationType(ArgumentMatchers.any[Request[AnyContent]]))
      .thenReturn(mockSession)

    val result = controller.form(atarService, Journey.Register).apply(SessionBuilder.buildRequestWithSession(aUserId))
    test(result)
  }

  private def submitForm(form: Map[String, String])(test: Future[Result] => Any) {
    val aUserId = defaultUserId
    withAuthorisedUser(aUserId, mockAuthConnector)

    when(mockSubscriptionPage.url(atarService)).thenReturn(testSubscriptionStartPageUrl)
    when(mockSession.data).thenReturn(testSessionData)
    when(
      mockRequestSessionData
        .sessionWithOrganisationTypeAdded(ArgumentMatchers.any[Session], ArgumentMatchers.any[CdsOrganisationType])
    ).thenReturn(mockSession)

    when(
      mockSubscriptionFlowManager.startSubscriptionFlow(
        ArgumentMatchers.any[Option[SubscriptionPage]],
        cdsOrganisationType = ArgumentMatchers.eq(selectedIndividualType),
        ArgumentMatchers.any[Service],
        ArgumentMatchers.any[Journey.Value]
      )(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[Request[AnyContent]])
    ).thenReturn(Future.successful(mockFlowStart))

    val result =
      controller.submit(atarService, Journey.Register).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(aUserId, form)
      )
    test(result)
  }

  private def assertRadioButtonIsPresent(
    page: CdsPage,
    labelXpath: String,
    expectedText: String,
    expectedValue: String
  ): Unit = {
    page.getElementText(labelXpath) should be(expectedText)
    page.getElementValueForLabel(labelXpath) should be(expectedValue)
  }

}
