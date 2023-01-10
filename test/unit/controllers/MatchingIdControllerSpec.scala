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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import play.mvc.Http.Status.SEE_OTHER
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{FeatureFlags, MatchingIdController}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.UserLocationController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder._
import util.builders.{AuthActionMock, SessionBuilder}

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

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockFeatureFlags.matchingEnabled).thenReturn(true)
  }

  override protected def afterEach(): Unit = {
    reset(mockMatchingService)

    super.afterEach()
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
      assertRedirectToUserLocationPage(result, atarService)
      verifyNoInteractions(mockMatchingService)
    }

    "for Journey GetAnEori redirect to Confirm page when a match found with CT UTR only" in {
      withAuthorisedUser(userId, mockAuthConnector, ctUtrId = Some(ctUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(ctUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
          .form(atarService, false)
          .url
      )
    }

    "for Journey GetAnEori redirect to Select user location page when no match found for SA UTR" in {
      withAuthorisedUser(userId, mockAuthConnector, saUtrId = Some(saUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(saUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(false))

      val controller =
        new MatchingIdController(mockAuthAction, mockFeatureFlags, mockMatchingService, mcc)(global)
      val result = await(controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))

      status(result) shouldBe SEE_OTHER
      assertRedirectToUserLocationPage(result, atarService)
    }

    "for Journey GetAnEori redirect to Confirm page when a match found with SA UTR only" in {
      withAuthorisedUser(userId, mockAuthConnector, saUtrId = Some(saUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(saUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
          .form(atarService, false)
          .url
      )
    }

    "for Journey GetAnEori redirect to Confirm page when a match found with a valid PAYE Nino" in {
      withAuthorisedUser(userId, mockAuthConnector, payeNinoId = Some(payeNinoId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Nino(payeNinoId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
          .form(atarService, false)
          .url
      )
    }

    "for Journey GetAnEori use CT UTR when user is registered for CT and SA" in {
      withAuthorisedUser(userId, mockAuthConnector, ctUtrId = Some(ctUtrId), saUtrId = Some(saUtrId))

      when(
        mockMatchingService.matchBusinessWithIdOnly(meq(Utr(ctUtrId)), any[LoggedInUserWithEnrolments])(
          any[HeaderCarrier],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(true))

      val result = controller.matchWithIdOnly(atarService).apply(SessionBuilder.buildRequestWithSession(userId))

      status(result) shouldBe SEE_OTHER
      result.header.headers("Location") should be(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
          .form(atarService, false)
          .url
      )
    }
  }

  private def assertRedirectToUserLocationPage(result: Result, service: Service): Unit =
    redirectLocation(result).get shouldBe UserLocationController.form(service).url

}
