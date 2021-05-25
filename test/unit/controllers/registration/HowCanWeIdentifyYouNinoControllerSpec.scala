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

import common.pages.SubscribeHowCanWeIdentifyYouPage
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.HowCanWeIdentifyYouNinoController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  AddressDetailsSubscriptionFlowPage,
  HowCanWeIdentifyYouSubscriptionFlowPage,
  SubscriptionFlowInfo
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_nino
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HowCanWeIdentifyYouNinoControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                    = mock[AuthConnector]
  private val mockAuthAction                       = authAction(mockAuthConnector)
  private val mockSubscriptionBusinessService      = mock[SubscriptionBusinessService]
  private val mockSubscriptionFlowManager          = mock[SubscriptionFlowManager]
  private val mockRequestSessionData               = mock[RequestSessionData]
  private val mockSubscriptionDetailsHolderService = mock[SubscriptionDetailsService]

  private val howCanWeIdentifyYouView = instanceOf[how_can_we_identify_you_nino]

  private val controller = new HowCanWeIdentifyYouNinoController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mockSubscriptionFlowManager,
    mockRequestSessionData,
    mcc,
    howCanWeIdentifyYouView,
    mockSubscriptionDetailsHolderService
  )

  override def beforeEach() {
    super.beforeEach()

    Mockito.reset(mockSubscriptionDetailsHolderService)

    when(mockSubscriptionDetailsHolderService.cacheCustomsId(any[CustomsId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))
    when(mockSubscriptionBusinessService.getCachedCustomsId(any[HeaderCarrier]))
      .thenReturn(Future.successful(None))

    when(
      mockSubscriptionFlowManager.stepInformation(ArgumentMatchers.eq(HowCanWeIdentifyYouSubscriptionFlowPage))(
        any[Request[AnyContent]]
      )
    ).thenReturn(SubscriptionFlowInfo(3, 5, AddressDetailsSubscriptionFlowPage))
  }

  "Loading the page" should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.createForm(atarService, Journey.Subscribe)
    )

    "show the form without errors" in {
      showForm(Map.empty) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath) shouldBe empty
      }
    }
  }

  "Submitting the form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForSubscribe(
      mockAuthConnector,
      controller.submit(isInReviewMode = false, atarService, Journey.Subscribe)
    )

    "give a page level error when nino not provided" in {
      submitForm(Map("nino" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Enter your National Insurance number"
      }
    }

    "give a page and field level error when a nino of the wrong length is provided" in {
      submitForm(Map("nino" -> "TOOSHORT")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The National Insurance number must be 9 characters"
        page.getElementsText(
          SubscribeHowCanWeIdentifyYouPage.fieldLevelErrorNino
        ) shouldBe "Error: The National Insurance number must be 9 characters"
      }
    }

    "give a page and field level error when an invalid nino is provided" in {
      submitForm(Map("nino" -> "123456789")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Enter a National Insurance number in the right format"
        page.getElementsText(
          SubscribeHowCanWeIdentifyYouPage.fieldLevelErrorNino
        ) shouldBe "Error: Enter a National Insurance number in the right format"
      }
    }

    "redirect to the 'Enter your business address' page when a valid nino is provided" in {
      submitForm(Map("nino" -> "AB123456C")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/address"
      }
    }

    "allow a NINO with spaces and lower case" in {
      submitForm(Map("nino" -> "ab 12 34 56 c")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/address"
      }
      verify(mockSubscriptionDetailsHolderService).cacheCustomsId(meq(Nino("AB123456C")))(any())
    }

    "redirect to 'Check your details' page when valid Nino is provided" in {
      submitFormInReviewMode(Map("nino" -> "AB123456C")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(
          "Location"
        ) shouldBe "/customs-enrolment-services/atar/subscribe/matching/review-determine"
      }
    }

    "redirect to 'Address Lookup' page" when {

      "user is during UK journey" in {

        when(mockRequestSessionData.isUKJourney(any())).thenReturn(true)

        submitForm(Map("nino" -> "AB123456C")) { result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/address-postcode"
        }
      }
    }
  }

  def showForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller.createForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

  def submitForm(form: Map[String, String], userId: String = defaultUserId, isInReviewMode: Boolean = false)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(isInReviewMode, atarService, Journey.Subscribe)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  def submitFormInReviewMode(form: Map[String, String], userId: String = defaultUserId, isInReviewMode: Boolean = true)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller
        .submit(isInReviewMode, atarService, Journey.Subscribe)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    )
  }

  def reviewForm(form: Map[String, String], userId: String = defaultUserId, customsId: CustomsId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionBusinessService.getCachedCustomsId(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(customsId)))

    test(
      controller.reviewForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

}
