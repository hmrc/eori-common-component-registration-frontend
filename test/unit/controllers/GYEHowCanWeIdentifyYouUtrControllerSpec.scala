/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.GYEHowCanWeIdentifyYouUtrController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{MatchingResponse, RegisterWithIDResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Individual, MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CorporateBody, NameDobMatchModel, NinoOrUtr, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionUtrFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
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

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockMatchingService = mock[MatchingService]
  private val mockFrontendDataCache = mock[SessionCache]
  private val mockOrgTypeLookup = mock[OrgTypeLookup]
  private val mockRequestSessionData = mock[RequestSessionData]
  private val errorView = inject[error_template]
  private val mockSubscriptionUtrFormProvider = mock[SubscriptionUtrFormProvider]
  when(mockSubscriptionUtrFormProvider.subscriptionUtrForm).thenReturn(new SubscriptionUtrFormProvider().subscriptionUtrForm)

  private val sessionCacheService =
    new SessionCacheService(mockFrontendDataCache, mockRequestSessionData, mockMatchingService, errorView)(global)

  private val howCanWeIdentifyYouView = inject[how_can_we_identify_you_utr]

  private val controller = new GYEHowCanWeIdentifyYouUtrController(
    mockAuthAction,
    mcc,
    howCanWeIdentifyYouView,
    mockOrgTypeLookup,
    mockFrontendDataCache,
    mockRequestSessionData,
    sessionCacheService,
    mockMatchingService,
    mockSubscriptionUtrFormProvider
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
      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
      when(mockOrgTypeLookup.etmpOrgType(any[Request[AnyContent]])).thenReturn(Future.successful(CorporateBody))

      withAuthorisedUser(defaultUserId, mockAuthConnector)
      form() { result =>
        status(result) shouldBe OK
      }
    }
  }

  "Submitting the form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService))

    "redirect to the Confirm page when a UTR is matched" in {

      val utr = "2108834503"

      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)

      when(
        mockFrontendDataCache.saveNinoOrUtrDetails(ArgumentMatchers.eq(NinoOrUtr(Some(Utr(utr)))))(any[Request[_]])
      ).thenReturn(Future.successful(true))

      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )

      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(
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

      submitForm(Map("utr" -> utr)) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value shouldBe "/customs-registration-services/atar/register/postcode"
      }
    }

    "Load UTR page with errors" in {

      val utr = "210883"

      when(mockRequestSessionData.selectedUserLocation(any())).thenReturn(Some(UserLocation.Uk))
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)

      when(
        mockFrontendDataCache.saveNinoOrUtrDetails(ArgumentMatchers.eq(NinoOrUtr(Some(Utr(utr)))))(any[Request[_]])
      ).thenReturn(Future.successful(true))

      when(mockFrontendDataCache.subscriptionDetails(any[Request[_]])).thenReturn(
        Future.successful(SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", "user", LocalDate.now))))
      )

      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(
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

      submitForm(Map("utr" -> utr)) { result =>
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
