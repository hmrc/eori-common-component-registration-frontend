/*
 * Copyright 2022 HM Revenue & Customs
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
import java.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.GYEHowCanWeIdentifyYouUtrController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{NameDobMatchModel, Utr}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.how_can_we_identify_you_utr
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GYEHowCanWeIdentifyYouUtrControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector     = mock[AuthConnector]
  private val mockAuthAction        = authAction(mockAuthConnector)
  private val mockMatchingService   = mock[MatchingService]
  private val mockFrontendDataCache = mock[SessionCache]

  private val howCanWeIdentifyYouView = instanceOf[how_can_we_identify_you_utr]

  private val controller = new GYEHowCanWeIdentifyYouUtrController(
    mockAuthAction,
    mockMatchingService,
    mcc,
    howCanWeIdentifyYouView,
    mockFrontendDataCache
  )

  "Viewing the form " should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.form(atarService))
  }

  "Submitting the form " should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.submit(atarService))

    "redirect to the Confirm page when a UTR is matched" in {

      val utr = "2108834503"
      when(mockFrontendDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(
        Future.successful(
          SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", None, "user", LocalDate.now)))
        )
      )
      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(true))

      submitForm(Map("utr" -> utr)) {
        result =>
          status(result) shouldBe SEE_OTHER
          result.header.headers("Location") shouldBe "/customs-registration-services/atar/register/matching/confirm"
      }
    }

    "give a page level error when a UTR is not matched" in {
      val utr = "2108834503"
      when(mockFrontendDataCache.subscriptionDetails(any[HeaderCarrier])).thenReturn(
        Future.successful(
          SubscriptionDetails(nameDobDetails = Some(NameDobMatchModel("test", None, "user", LocalDate.now)))
        )
      )
      when(
        mockMatchingService
          .matchIndividualWithId(ArgumentMatchers.eq(Utr(utr)), any[Individual], any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(false))

      submitForm(Map("utr" -> utr)) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(
            RegisterHowCanWeIdentifyYouPage.pageLevelErrorSummaryListXPath
          ) shouldBe "Your details have not been found. Check that your details are correct and then try again."
      }
    }
  }

  def submitForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

}
