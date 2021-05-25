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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.HowCanWeIdentifyYouController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HowCanWeIdentifyYouControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector                    = mock[AuthConnector]
  private val mockAuthAction                       = authAction(mockAuthConnector)
  private val mockSubscriptionBusinessService      = mock[SubscriptionBusinessService]
  private val mockSubscriptionDetailsHolderService = mock[SubscriptionDetailsService]

  private val howCanWeIdentifyYouView = instanceOf[how_can_we_identify_you]

  private val controller = new HowCanWeIdentifyYouController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mcc,
    howCanWeIdentifyYouView,
    mockSubscriptionDetailsHolderService
  )

  override def beforeEach() {
    super.beforeEach()

    Mockito.reset(mockSubscriptionDetailsHolderService)

    when(mockSubscriptionDetailsHolderService.cacheNinoOrUtrChoice(any[NinoOrUtrChoice])(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    when(mockSubscriptionBusinessService.getCachedNinoOrUtrChoice(any[HeaderCarrier])).thenReturn(
      Future.successful(None)
    )
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

    "give a page level error when neither radio button is selected" in {
      submitForm(Map("ninoOrUtrRadio" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Select how we can identify you"
        page.getElementsText(SubscribeHowCanWeIdentifyYouPage.fieldLevelErrorUtr) shouldBe empty
        page.getElementsText(SubscribeHowCanWeIdentifyYouPage.fieldLevelErrorNino) shouldBe empty
      }
    }

    "redirect to the 'Enter your nino' page when nino is selected" in {
      submitForm(Map("ninoOrUtrRadio" -> "nino")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/chooseid/nino"
      }
    }

    "redirect to the 'Enter your utr' page when utr is selected" in {
      submitForm(Map("ninoOrUtrRadio" -> "utr")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/chooseid/utr"
      }
    }

    "in review mode redirect to 'Enter nino' page when nino selected" in {
      submitFormInReviewMode(Map("ninoOrUtrRadio" -> "nino")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/chooseid/nino/review"
      }
    }

    "in review mode redirect to 'Enter utr' page when utr selected" in {
      submitFormInReviewMode(Map("ninoOrUtrRadio" -> "utr")) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "/customs-enrolment-services/atar/subscribe/chooseid/utr/review"
      }
    }

    "redirect to 'What information can we use to confirm your identity' page when Utr is needed to be changed" in {
      reviewForm(Map("utr" -> "2108834503", "ninoOrUtrRadio" -> "utr")) { result =>
        status(result) shouldBe OK
      }
    }

    "redirect to 'What information can we use to confirm your identity' page when Nino is needed to be changed" in {
      reviewForm(Map("nino" -> "SM2810293A", "ninoOrUtrRadio" -> "nino")) { result =>
        status(result) shouldBe OK
      }
    }

  }

  def showForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionBusinessService.getCachedCustomsId(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(Utr("id"))))
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

  def reviewForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(
      controller.reviewForm(atarService, Journey.Subscribe).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)
      )
    )
  }

}
