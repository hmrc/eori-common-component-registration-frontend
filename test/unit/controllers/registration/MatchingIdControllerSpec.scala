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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import play.mvc.Http.Status.SEE_OTHER
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.FeatureFlags
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.MatchingIdController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.UserLocationController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.MatchingService
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class MatchingIdControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector   = mock[AuthConnector]
  private val mockAuthAction      = authAction(mockAuthConnector)
  private val mockFeatureFlags    = mock[FeatureFlags]
  private val mockMatchingService = mock[MatchingService]

  private val userId: String     = "someUserId"
  private val ctUtrId: String    = "ct-utr-Id"
  private val saUtrId: String    = "sa-utr-Id"
  private val payeNinoId: String = "AB123456C"

  private val controller =
    new MatchingIdController(mockAuthAction, mockFeatureFlags, mockMatchingService, mcc)(global)

  override def beforeEach: Unit = {
    reset(mockMatchingService)
    when(mockFeatureFlags.matchingEnabled).thenReturn(true)
  }

  "MatchingIdController for GetAnEori Journey" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.matchWithIdOnly(atarService))

    "for Journey GetAnEori redirect to Select user location page when Government Gateway Account has no enrollments" in {
      withAuthorisedUser(userId, mockAuthConnector)

      val controller =
        new MatchingIdController(mockAuthAction, mockFeatureFlags, mockMatchingService, mcc)(global)
      val result: Result =
        await(controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))

      status(result) shouldBe SEE_OTHER
      assertRedirectToUserLocationPage(result, atarService, Journey.Register)
      verifyZeroInteractions(mockMatchingService)
    }

    "for Journey GetAnEori redirect to Confirm page when a match found with CT UTR only" in {
      withAuthorisedUser(userId, mockAuthConnector, ctUtrId = Some(ctUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(ctUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.ConfirmContactDetailsController
          .form(atarService, Journey.Register)
          .url
      )
    }

    "for Journey GetAnEori redirect to Select user location page when no match found for SA UTR" in {
      withAuthorisedUser(userId, mockAuthConnector, saUtrId = Some(saUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(saUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(false))

      val controller =
        new MatchingIdController(mockAuthAction, mockFeatureFlags, mockMatchingService, mcc)(global)
      val result = await(controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))

      status(result) shouldBe SEE_OTHER
      assertRedirectToUserLocationPage(result, atarService, Journey.Register)
    }

    "for Journey GetAnEori redirect to Confirm page when a match found with SA UTR only" in {
      withAuthorisedUser(userId, mockAuthConnector, saUtrId = Some(saUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(saUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.ConfirmContactDetailsController
          .form(atarService, Journey.Register)
          .url
      )
    }

    "for Journey GetAnEori redirect to Confirm page when a match found with a valid PAYE Nino" in {
      withAuthorisedUser(userId, mockAuthConnector, payeNinoId = Some(payeNinoId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Nino(payeNinoId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.ConfirmContactDetailsController
          .form(atarService, Journey.Register)
          .url
      )
    }

    "for Journey GetAnEori use CT UTR when user is registered for CT and SA" in {
      withAuthorisedUser(userId, mockAuthConnector, ctUtrId = Some(ctUtrId), saUtrId = Some(saUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(ctUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.ConfirmContactDetailsController
          .form(atarService, Journey.Register)
          .url
      )
    }
  }

  "MatchingIdController for Subscribe Journey" should {

    "redirect to GG login when request is not authenticated with redirect to based in UK" in {
      AuthBuilder.withNotLoggedInUser(mockAuthConnector)

      val result = controller
        .matchWithIdOnlyForExistingReg(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPathNoUserAndBasedInUkNotSelected(
            method = "GET",
            path = "/customs-enrolment-services/atar/subscribe"
          )
        )
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should include(
        "/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A6750%2Fcustoms-enrolment-services%2Fatar%2Fsubscribe&origin=eori-common-component-frontend"
      )
    }

    "redirect to GG login when request is not authenticated with redirect to Subscribe when the user selects yes on based in uk" in {
      AuthBuilder.withNotLoggedInUser(mockAuthConnector)

      val result = controller
        .matchWithIdOnlyForExistingReg(atarService)
        .apply(
          SessionBuilder
            .buildRequestWithSessionAndPathNoUser(method = "GET", path = "/customs-enrolment-services/atar/subscribe")
        )
      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should include(
        "/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A6750%2Fcustoms-enrolment-services%2Fatar%2Fsubscribe&origin=eori-common-component-frontend"
      )
    }

    "redirect to Select Location Type page for selected journey type Subscribe " in {
      withAuthorisedUser(userId, mockAuthConnector, ctUtrId = Some(ctUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(ctUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(false))

      val controller =
        new MatchingIdController(mockAuthAction, mockFeatureFlags, mockMatchingService, mcc)(global)
      val result =
        await(
          controller.matchWithIdOnlyForExistingReg(atarService).apply(SessionBuilder.buildRequestWithSession(userId))
        )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/customs-enrolment-services/atar/subscribe/matching/what-is-your-eori"
    }
  }

  private def assertRedirectToUserLocationPage(result: Result, service: Service, journey: Journey.Value): Unit =
    redirectLocation(result).get shouldBe UserLocationController.form(service, journey).url

}
