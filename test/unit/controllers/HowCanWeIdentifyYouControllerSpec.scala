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

import common.pages.SubscribeHowCanWeIdentifyYouPage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.HowCanWeIdentifyYouController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.how_can_we_identify_you
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

  override def beforeEach(): Unit = {
    super.beforeEach()

    Mockito.reset(mockSubscriptionDetailsHolderService)

    when(mockSubscriptionDetailsHolderService.cacheNinoOrUtrChoice(any[NinoOrUtrChoice])(any[Request[_]]))
      .thenReturn(Future.successful(()))

    when(mockSubscriptionBusinessService.getCachedNinoOrUtrChoice(any[Request[_]])).thenReturn(Future.successful(None))
  }

  "Loading the page" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "show the form without errors" in {
      showForm(Map.empty) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      //  Previous usual behavior DDCYLS-5614
//        status(result) shouldBe OK
//        val page = CdsPage(contentAsString(result))
//        page.getElementsText(SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath) shouldBe empty
      }
    }
  }

  "Submitting the form" should {

//    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService)) //  Previous usual behavior DDCYLS-5614
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "give a page level error when neither radio button is selected" in {
      submitForm(Map("ninoOrUtrRadio" -> "")) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      //  Previous usual behavior DDCYLS-5614
//        status(result) shouldBe BAD_REQUEST
//        val page = CdsPage(contentAsString(result))
//        page.getElementsText(
//          SubscribeHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
//        ) shouldBe "Select how we can identify you"
//        page.getElementsText(SubscribeHowCanWeIdentifyYouPage.fieldLevelErrorUtr) shouldBe empty
//        page.getElementsText(SubscribeHowCanWeIdentifyYouPage.fieldLevelErrorNino) shouldBe empty
      }
    }

    "redirect to the 'Enter your nino' page when nino is selected" in {
      submitForm(Map("ninoOrUtrRadio" -> "nino")) { result =>
        status(result) shouldBe SEE_OTHER
//        header("Location", result).value shouldBe "/customs-registration-services/atar/register/matching/chooseid/nino" //  Previous usual behavior DDCYLS-5614
        header(
          "Location",
          result
        ).value shouldBe "/customs-registration-services/atar/register/ind-st-use-a-different-service"
      }
    }

    "redirect to the 'Enter your utr' page when utr is selected" in {
      submitForm(Map("ninoOrUtrRadio" -> "utr")) { result =>
        status(result) shouldBe SEE_OTHER
//        header("Location", result).value shouldBe "/customs-registration-services/atar/register/matching/chooseid/utr" //  Previous usual behavior DDCYLS-5614
        header(
          "Location",
          result
        ).value shouldBe "/customs-registration-services/atar/register/ind-st-use-a-different-service"
      }
    }

    "throw exception on selecting an invalid data" in {
      //  Previous usual behavior DDCYLS-5614
//      intercept[IllegalArgumentException] {
//        submitForm(Map("ninoOrUtrRadio" -> "xyz")) { result =>
//          status(result) shouldBe SEE_OTHER
//        }
//      }
      submitForm(Map("ninoOrUtrRadio" -> "xyz")) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }
  }

  def showForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    when(mockSubscriptionBusinessService.getCachedCustomsId(any[Request[_]]))
      .thenReturn(Future.successful(Some(Utr("id"))))
    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

  def submitForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
//    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))) //  Previous usual behavior DDCYLS-5614
    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

}
