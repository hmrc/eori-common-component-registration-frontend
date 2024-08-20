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

import common.pages.matching.DoYouHaveNinoPage._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.DoYouHaveNinoController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, NameDobMatchModel, NinoMatchModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_nino_row_individual
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DoYouHaveNinoControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  private val matchNinoRowIndividualView = instanceOf[match_nino_row_individual]

  private val doYouHaveNinoController = new DoYouHaveNinoController(
    mockAuthAction,
    mockRequestSessionData,
    mcc,
    matchNinoRowIndividualView,
    mockSubscriptionDetailsService
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    withAuthorisedUser(defaultUserId, mockAuthConnector)
    when(mockSubscriptionDetailsService.cacheNinoMatch(any())(any[Request[_]])).thenReturn(Future.successful(()))
    when(mockSubscriptionDetailsService.updateSubscriptionDetailsIndividual(any())).thenReturn(
      Future.successful((): Unit)
    )
  }

  override protected def afterEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockRequestSessionData)
    reset(mockSubscriptionDetailsService)

    super.afterEach()
  }

  val yesNinoSubmitData: Map[String, String] = Map("have-nino" -> "true")
  val noNinoSubmitData: Map[String, String]  = Map("have-nino" -> "false")

  "Viewing the NINO Individual/Sole trader Rest of World Matching form" should {

    "display the form" in {

      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(Future.successful(None))

      displayForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(fieldLevelErrorNino) shouldBe empty
      }
    }

    "display the form with cached nino" in {

      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(
        Future.successful(Some(NinoMatchModel(Some(true), Some("12345"))))
      )

      displayForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(fieldLevelErrorNino) shouldBe empty
      }
    }

    "ensure the labels are correct" in {

      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(Future.successful(None))

      displayForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(yesLabel) shouldBe "Yes"
        page.elementIsPresent(yesRadioButton) shouldBe true

        page.getElementsText(noLabel) shouldBe "No"
        page.elementIsPresent(noRadioButton) shouldBe true

        page.getElementsText(fieldLevelErrorNino) shouldBe empty
      }
    }

  }

  "Submitting the form" should {

    "redirect to 'Get Nino' page when Y is selected" in {

      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[Request[_]])).thenReturn(
        Future.successful(Some(NameDobMatchModel("First name", "Last name", LocalDate.of(2015, 10, 15))))
      )
      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(Future.successful(None))

      submitForm(yesNinoSubmitData) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/matching/row/get-nino")
      }
    }

    "redirect to 'Enter your address' page when N is selected" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any()))
        .thenReturn(Some(CdsOrganisationType.ThirdCountrySoleTrader))
      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cacheNinoMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(noNinoSubmitData) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/matching/address/third-country-sole-trader")
      }
    }

    "redirect to 'Enter your address' page when N is selected and cached nino option is false" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any()))
        .thenReturn(Some(CdsOrganisationType.ThirdCountrySoleTrader))
      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(
        Future.successful(Some(NinoMatchModel(Some(false))))
      )
      when(mockSubscriptionDetailsService.cacheNinoMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(noNinoSubmitData) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/matching/address/third-country-sole-trader")
      }
    }

    "display error when form empty" in {

      submitForm(Map("have-nino" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText("//div[@class='govuk-error-summary__body']") should include(
          "Select yes if you have a National Insurance number"
        )
      }
    }

    "redirect the user to the start of the journey when N is selected and no org cached" in {

      when(mockRequestSessionData.userSelectedOrganisationType(any()))
        .thenReturn(None)
      when(mockSubscriptionDetailsService.cachedNinoMatch(any())).thenReturn(Future.successful(None))
      when(mockSubscriptionDetailsService.cacheNinoMatch(any())(any())).thenReturn(Future.successful((): Unit))

      submitForm(form = noNinoSubmitData) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("register/check-user")
      }
    }
  }

  private def displayForm()(test: Future[Result] => Any): Unit =
    test(
      doYouHaveNinoController
        .displayForm(atarService)
        .apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    )

  private def submitForm(form: Map[String, String])(test: Future[Result] => Any): Unit =
    test(
      doYouHaveNinoController
        .submit(atarService)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form))
    )

}
