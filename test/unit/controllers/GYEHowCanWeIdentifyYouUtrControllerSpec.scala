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
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.GYEHowCanWeIdentifyYouUtrController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, NameDobMatchModel, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  DataUnavailableException,
  SessionCache,
  SessionCacheService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, how_can_we_identify_you_utr}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GYEHowCanWeIdentifyYouUtrControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector     = mock[AuthConnector]
  private val mockAuthAction        = authAction(mockAuthConnector)
  private val mockMatchingService   = mock[MatchingService]
  private val mockFrontendDataCache = mock[SessionCache]
  private val mockOrgTypeLookup     = mock[OrgTypeLookup]
  private val sessionCacheService   = new SessionCacheService(mockFrontendDataCache)
  private val errorView             = instanceOf[error_template]

  private val howCanWeIdentifyYouView = instanceOf[how_can_we_identify_you_utr]

  private val controller = new GYEHowCanWeIdentifyYouUtrController(
    mockAuthAction,
    mockMatchingService,
    mcc,
    howCanWeIdentifyYouView,
    mockOrgTypeLookup,
    sessionCacheService,
    errorView
  )

  "Viewing the form " should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))

    "display howCanWeIdentifyYouView for logged in user" in {

      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(CorporateBody))

      withAuthorisedUser(defaultUserId, mockAuthConnector)
      form() { result =>
//        status(result) shouldBe OK //  Previous usual behavior DDCYLS-5614
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/ind-st-use-a-different-service")
      }
    }
  }

  "Submitting the form " should {

//    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService)) //  Previous usual behavior DDCYLS-5614
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))

    "redirect to the Confirm page when a UTR is matched" in {

      val utr = "2108834503"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT(()))

      submitForm(Map("utr" -> utr)) {
        result =>
          status(result) shouldBe SEE_OTHER
//          header("Location", result).value shouldBe "/customs-registration-services/atar/register/matching/confirm" //  Previous usual behavior DDCYLS-5614
          header(
            "Location",
            result
          ).value shouldBe "/customs-registration-services/atar/register/ind-st-use-a-different-service"
      }
    }

    "give a page level error when a UTR is not matched" in {
      val utr = "2108834503"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.matchFailureResponse))

      submitForm(Map("utr" -> utr)) {
        result =>
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
        //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe BAD_REQUEST
//          val page = CdsPage(contentAsString(result))
//          page.getElementsText(
//            RegisterHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
//          ) shouldBe "Your details have not been found. Check that your details are correct and then try again."
      }
    }
    "give a error-template page when a downstreamFailureResponse happens" in {
      val utr = "2108834503"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.downstreamFailureResponse))

      submitForm(Map("utr" -> utr)) {
        result =>
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
        //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe OK
//          val page = CdsPage(contentAsString(result))
//          page.getElementsHtml("h1") shouldBe messages("cds.error.title")
      }
    }

    "give a error-template page when a any other error occurred" in {
      val utr = "2108834503"
      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(eitherT[Unit](MatchingServiceConnector.otherErrorHappen))

      submitForm(Map("utr" -> utr)) {
        result =>
          status(result) shouldBe SEE_OTHER
        //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe INTERNAL_SERVER_ERROR
//          val page = CdsPage(contentAsString(result))
//          page.getElementsHtml("h1") shouldBe messages("cds.error.title")
      }
    }

    "display error when no input" in {

      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      submitForm(Map("utr" -> "")) {
        result =>
          status(result) shouldBe SEE_OTHER
          header("Location", result).value should endWith("register/ind-st-use-a-different-service")
        //  Previous usual behavior DDCYLS-5614
//          status(result) shouldBe BAD_REQUEST
//          val page = CdsPage(contentAsString(result))
//          page.getElementsText(RegisterHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath) shouldBe messages(
//            "cds.matching-error.business-details.utr.isEmpty"
//          )
      }
    }

    //  Previous usual behavior DDCYLS-5614
//    "throw exception when no NameDob present in cache" in {
//
//      val utr = "2108834503"
//      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
//        Future.successful(SubscriptionDetails(nameDobDetails = None))
//      )
//
//      withAuthorisedUser(defaultUserId, mockAuthConnector)
//      val caught = intercept[DataUnavailableException] {
//        await(
//          controller.submit(atarService).apply(
//            SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, Map("utr" -> utr))
//          )
//        )
//      }
//
//      caught.message should startWith("NameDob is not cached in data")
//    }
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
