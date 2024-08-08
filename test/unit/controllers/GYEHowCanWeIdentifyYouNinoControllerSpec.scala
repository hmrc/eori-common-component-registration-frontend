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

import common.pages.RegisterHowCanWeIdentifyYouPage
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.GYEHowCanWeIdentifyYouNinoController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.NameDobMatchModel
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  DataUnavailableException,
  SessionCache,
  SessionCacheService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, how_can_we_identify_you_nino}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GYEHowCanWeIdentifyYouNinoControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector     = mock[AuthConnector]
  private val mockAuthAction        = authAction(mockAuthConnector)
  private val mockMatchingService   = mock[MatchingService]
  private val mockFrontendDataCache = mock[SessionCache]
  private val sessionCacheService   = new SessionCacheService(mockFrontendDataCache)
  private val errorView             = instanceOf[error_template]

  private val howCanWeIdentifyYouView = instanceOf[how_can_we_identify_you_nino]

  private val controller = new GYEHowCanWeIdentifyYouNinoController(
    mockAuthAction,
    mockMatchingService,
    mcc,
    howCanWeIdentifyYouView,
    sessionCacheService,
    errorView
  )

  "Viewing the form " should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))

    "display howCanWeIdentifyYouView for logged in user" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      form() { result =>
        //  Previous usual behavior DDCYLS-5614
//        status(result) shouldBe OK
//        val page = CdsPage(contentAsString(result))
//        page.title() should startWith("Enter your National Insurance number")
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }
  }

  "Submitting the form " should {

//    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService)) //  Previous usual behavior DDCYLS-5614
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))

    "redirect to the Confirm page when a nino is matched" in {

      val nino = "AB123456C"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT(()))

      submitForm(Map("nino" -> nino)) {
        result =>
          status(result) shouldBe SEE_OTHER
          //  Previous usual behavior DDCYLS-5614
//          header("Location", result).value shouldBe "/customs-registration-services/atar/register/matching/confirm"
          header(
            "Location",
            result
          ).value shouldBe "/customs-registration-services/atar/register/ind-st-use-a-different-service"
      }
    }

    "give a page level error when a nino is not matched" in {
      val nino = "AB123456C"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.matchFailureResponse))

      submitForm(Map("nino" -> nino)) {
        result =>
          //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe BAD_REQUEST
//          val page = CdsPage(contentAsString(result))
//          page.getElementsText(
//            RegisterHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
//          ) shouldBe "Your details have not been found. Check that your details are correct and then try again."
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }

    "redirect to error_template when downstreamFailureResponse" in {
      val nino = "AB123456C"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.downstreamFailureResponse))

      submitForm(Map("nino" -> nino)) {
        result =>
          //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe OK
//          val page = CdsPage(contentAsString(result))
//          page.getElementsHtml("h1") shouldBe messages("cds.error.title")
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }

    "redirect to error_template when any other error is case " in {
      val nino = "AB123456C"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.otherErrorHappen))

      submitForm(Map("nino" -> nino)) {
        result =>
          //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe INTERNAL_SERVER_ERROR
//          val page = CdsPage(contentAsString(result))
//          page.getElementsHtml("h1") shouldBe messages("cds.error.title")
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }

    "display error when no input" in {

      val nino = "AB123456C"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT(()))

      submitForm(Map("nino" -> "")) {
        result =>
          //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe BAD_REQUEST
//          val page = CdsPage(contentAsString(result))
//          page.getElementsText(
//            RegisterHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
//          ) shouldBe "Enter your National Insurance number"
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }

    "throw exception when no NameDob present in cache" in {

      val nino = "AB123456C"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = None))
      )

      withAuthorisedUser(defaultUserId, mockAuthConnector)
      //  Previous usual behavior DDCYLS-5614
//      val caught = intercept[DataUnavailableException] {
//        await(
////          controller.submit(atarService).apply( //  Previous usual behavior DDCYLS-5614
//          controller.form(atarService).apply(
//            SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, Map("nino" -> nino))
//          )
//        )
//      }
//
//      caught.message should startWith("NameDob is not cached in data")

      val result = controller.form(atarService).apply(
        SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, Map("nino" -> nino))
      )

      status(result) shouldBe SEE_OTHER
      header("Location", result).value should endWith("register/ind-st-use-a-different-service")
    }
  }

  def submitForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
//    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))) //  Previous usual behavior DDCYLS-5614
    test(controller.form(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

  def form(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.form(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
