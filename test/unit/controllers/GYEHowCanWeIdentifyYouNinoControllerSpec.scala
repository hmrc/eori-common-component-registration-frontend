/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.data.EitherT
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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{MatchingResponse, RegisterWithIDResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Individual, MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{NameDobMatchModel, Nino, NinoOrUtr}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionNinoFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, how_can_we_identify_you_nino}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GYEHowCanWeIdentifyYouNinoControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockMatchingService = mock[MatchingService]
  private val mockFrontendDataCache = mock[SessionCache]
  private val mockRequestSessionData = mock[RequestSessionData]
  private val mockSubscriptionNinoFormProvider = mock[SubscriptionNinoFormProvider]
  when(mockSubscriptionNinoFormProvider.subscriptionNinoForm).thenReturn(new SubscriptionNinoFormProvider().subscriptionNinoForm)

  private val errorView = inject[error_template]

  private val sessionCacheService =
    new SessionCacheService(mockFrontendDataCache, mockRequestSessionData, mockMatchingService, errorView)(global)

  private val howCanWeIdentifyYouView = inject[how_can_we_identify_you_nino]

  private val controller = new GYEHowCanWeIdentifyYouNinoController(
    mockAuthAction,
    mcc,
    mockFrontendDataCache,
    howCanWeIdentifyYouView,
    mockRequestSessionData,
    mockMatchingService,
    sessionCacheService,
    mockSubscriptionNinoFormProvider,
    errorView
  )(global)

  "Viewing the form " should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))

    "display you need to use a different for logged in user if country is ROW and individual" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.ThirdCountry))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)

      form() { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          "/customs-registration-services/atar/register/ind-st-use-a-different-service"
        )
      }
    }

    "display howCanWeIdentifyYouView for logged in user" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
      form() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Enter your National Insurance number")
      }
    }
  }

  "Submitting the form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService))

    "redirect to the Confirm page when a nino is matched" in {

      val nino = "AB123456C"
      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)

      when(
        mockFrontendDataCache.saveNinoOrUtrDetails(any())(any[Request[_]])
      ).thenReturn(Future.successful(true))

      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )

      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(
        eitherT[MatchingResponse](
          MatchingResponse(
            RegisterWithIDResponse(
              ResponseCommon(
                "OK",
                Some("002 - No match found"),
                LocalDate.now.atTime(8, 35, 2),
                Some(List(MessagingServiceParam("POSITION", "FAIL")))
              ),
              None
            )
          )
        )
      )

      submitForm(Map("nino" -> nino)) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value shouldBe "/customs-registration-services/atar/register/postcode"
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
      ).thenReturn(EitherT.left(Future { MatchingServiceConnector.matchFailureResponse }))

      submitForm(Map("nino" -> nino)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          RegisterHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Your details have not been found. Check that your details are correct and then try again."
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
      ).thenReturn(EitherT.left(Future { MatchingServiceConnector.downstreamFailureResponse }))

      submitForm(Map("nino" -> nino)) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml("h1") shouldBe messages("cds.error.title")
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
      ).thenReturn(EitherT.left(Future { MatchingServiceConnector.otherErrorHappen }))

      submitForm(Map("nino" -> nino)) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml("h1") shouldBe messages("cds.error.title")
      }
    }

    "Load Nino page with errors" in {

      val nino = ""
      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)

      when(
        mockFrontendDataCache.saveNinoOrUtrDetails(ArgumentMatchers.eq(NinoOrUtr(Some(Nino(nino)))))(any[Request[_]])
      ).thenReturn(Future.successful(true))

      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )
      when(
        mockMatchingService
          .matchIndividualWithNino(ArgumentMatchers.eq(nino), any[Individual], any())(
            any[HeaderCarrier],
            any[Request[_]]
          )
      ).thenReturn(
        eitherT[MatchingResponse](
          MatchingResponse(
            RegisterWithIDResponse(
              ResponseCommon(
                "OK",
                Some("002 - No match found"),
                LocalDate.now.atTime(8, 35, 2),
                Some(List(MessagingServiceParam("POSITION", "FAIL")))
              ),
              None
            )
          )
        )
      )

      submitForm(Map("nino" -> nino)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText("title") should startWith("Error: ")
      }
    }
  }

  def submitForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

  def form(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.form(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
