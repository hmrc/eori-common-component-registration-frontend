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

import common.pages.matching.DoYouHaveNinoPage._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.GetNinoController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{IdMatchModel, NameDobMatchModel, Nino}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_nino
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.NinoFormBuilder
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetNinoControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockMatchingService            = mock[MatchingService]
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  private val matchNinoRowIndividualView = instanceOf[how_can_we_identify_you_nino]

  private val doYouHaveNinoController = new GetNinoController(
    mockAuthAction,
    mockMatchingService,
    mcc,
    matchNinoRowIndividualView,
    mockSubscriptionDetailsService
  )

  private val notMatchedError =
    "Your details have not been found. Check that your details are correct and then try again."

  override def beforeEach: Unit =
    reset(mockMatchingService)

  val validNino                         = Nino(NinoFormBuilder.Nino)
  val yesNinoSubmitData                 = Map("nino" -> NinoFormBuilder.Nino)
  val yesNinoNotProvidedSubmitData      = Map("nino" -> "")
  val yesNinoWrongFormatSubmitData      = Map("nino" -> "ABZ")
  val mandatoryNinoFields: IdMatchModel = subscriptionNinoForm.bind(yesNinoSubmitData).value.get

  "Viewing the NINO Individual/Sole trader Rest of World Matching form" should {

    "display the form" in {
      displayForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(fieldLevelErrorNino) shouldBe empty
      }
    }

  }

  "Submitting the form" should {
    "redirect to 'These are the details we have about you' page when Y is selected and given NINO is matched" in {
      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[HeaderCarrier])).thenReturn(
        Future.successful(Some(NameDobMatchModel("First name", None, "Last name", new LocalDate(2015, 10, 15))))
      )
      when(mockMatchingService.matchIndividualWithId(any[Nino], any[Individual], any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      submitForm(yesNinoSubmitData) { result =>
        await(result)
        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("register/matching/confirm")
        val expectedIndividual = Individual.withLocalDate("First name", None, "Last name", new LocalDate(2015, 10, 15))
        verify(mockMatchingService).matchIndividualWithId(meq(validNino), meq(expectedIndividual), any())(
          any[HeaderCarrier]
        )
      }
    }

    "keep the user on the same page with proper message when NINO was not recognized" in {
      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[HeaderCarrier])).thenReturn(
        Future.successful(Some(NameDobMatchModel("First name", None, "Last name", new LocalDate(2015, 10, 15))))
      )
      when(mockMatchingService.matchIndividualWithId(any[Nino], any[Individual], any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(false))

      submitForm(yesNinoSubmitData) { result =>
        await(result)
        val page = CdsPage(contentAsString(result))
        status(result) shouldBe BAD_REQUEST
        val expectedIndividual = Individual.withLocalDate("First name", None, "Last name", new LocalDate(2015, 10, 15))
        verify(mockMatchingService).matchIndividualWithId(meq(validNino), meq(expectedIndividual), any())(
          any[HeaderCarrier]
        )

        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe notMatchedError
      }
    }

    "keep the user on the same page with error message when NINO was not recognized for no name in cache" in {
      when(mockSubscriptionDetailsService.cachedNameDobDetails(any[HeaderCarrier])).thenReturn(Future.successful(None))
      when(mockMatchingService.matchIndividualWithId(any[Nino], any[Individual], any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(false))

      submitForm(yesNinoSubmitData) { result =>
        await(result)
        val page = CdsPage(contentAsString(result))
        status(result) shouldBe BAD_REQUEST
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe notMatchedError
      }
    }

    "nino" should {
      "be mandatory" in {
        submitForm(yesNinoNotProvidedSubmitData) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your National Insurance number"
          page.getElementsText(fieldLevelErrorNino) shouldBe "Error: Enter your National Insurance number"
        }
      }

      "be valid" in {
        submitForm(yesNinoWrongFormatSubmitData) { result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(
            pageLevelErrorSummaryListXPath
          ) shouldBe "The National Insurance number must be 9 characters"
          page.getElementText(fieldLevelErrorNino) shouldBe "Error: The National Insurance number must be 9 characters"
        }
      }
    }
  }

  private def displayForm()(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      doYouHaveNinoController
        .displayForm(atarService, Journey.Register)
        .apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    )
  }

  private def submitForm(form: Map[String, String])(test: Future[Result] => Any) {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      doYouHaveNinoController
        .submit(atarService, Journey.Register)
        .apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form))
    )
  }

}
