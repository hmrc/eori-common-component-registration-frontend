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

import common.pages.NinoMatchPage
import common.pages.matching.NameDateOfBirthPage.{fieldLevelErrorDateOfBirth, pageLevelErrorSummaryListXPath}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.NinoController
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfter
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.MatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.match_nino
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.matching.NinoFormBuilder

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NinoControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector   = mock[AuthConnector]
  private val mockAuthAction      = authAction(mockAuthConnector)
  private val mockMatchingService = mock[MatchingService]

  private val matchNinoView = instanceOf[match_nino]

  val controller = new NinoController(mockAuthAction, mcc, matchNinoView, mockMatchingService)

  before {
    Mockito.reset(mockMatchingService)
  }

  val defaultOrganisationType = "individual"

  val FirstNamePage  = "Enter your first name"
  val FirstNameField = "Error: Enter your first name"
  val LastNamePage   = "Enter your last name"
  val LastNameField  = "Error: Enter your last name"
  val NinoPage       = "Enter your National Insurance number"
  val NinoField      = "Error: Enter your National Insurance number"
  val DateOfBirth    = "Date of birth"

  val InvalidNinoPage  = "Enter a National Insurance number in the right format"
  val InvalidNinoField = "Error: Enter a National Insurance number in the right format"

  "loading the page" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(defaultOrganisationType, atarService, Journey.Register)
    )

    "show the form without errors" in {
      showForm(Map()) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe empty
      }
    }
  }

  "first name" should {

    "be mandatory" in {
      submitForm(NinoFormBuilder.asForm + ("first-name" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe FirstNamePage
        page.getElementsText(NinoMatchPage.fieldLevelErrorFirstName) shouldBe FirstNameField
      }
    }

    "be restricted to 35 characters" in {
      submitForm(NinoFormBuilder.asForm + ("first-name" -> oversizedString(35))) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          NinoMatchPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The first name must be 35 characters or less"
        page.getElementsText(
          NinoMatchPage.fieldLevelErrorFirstName
        ) shouldBe "Error: The first name must be 35 characters or less"
      }
    }
  }

  "last name" should {

    "be mandatory" in {
      submitForm(NinoFormBuilder.asForm + ("last-name" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe LastNamePage
        page.getElementsText(NinoMatchPage.fieldLevelErrorLastName) shouldBe LastNameField
      }
    }

    "be restricted to 35 characters" in {
      submitForm(NinoFormBuilder.asForm + ("last-name" -> oversizedString(35))) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          NinoMatchPage.pageLevelErrorSummaryListXPath
        ) shouldBe "The last name must be 35 characters or less"
        page.getElementsText(
          NinoMatchPage.fieldLevelErrorLastName
        ) shouldBe "Error: The last name must be 35 characters or less"
      }
    }
  }

  "date of birth" should {

    "be mandatory" in {
      submitForm(
        NinoFormBuilder.asForm + ("date-of-birth.day" -> "", "date-of-birth.month" -> "",
        "date-of-birth.year"                          -> "")
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe "Enter your date of birth"
        page.getElementsText(NinoMatchPage.fieldLevelErrorDateOfBirth) shouldBe "Error: Enter your date of birth"
      }
    }

    "be a valid date" in {
      submitForm(NinoFormBuilder.asForm + ("date-of-birth.day" -> "32")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe "Enter a day between 1 and 31"
        page.getElementsText(NinoMatchPage.fieldLevelErrorDateOfBirth) shouldBe "Error: Enter a day between 1 and 31"
      }
    }

    "not be in the future " in {
      val tomorrow = LocalDate.now().plusDays(1)
      submitForm(
        NinoFormBuilder.asForm + ("date-of-birth.day" -> tomorrow.getDayOfMonth.toString,
        "date-of-birth.month"                         -> tomorrow.getMonthOfYear.toString,
        "date-of-birth.year"                          -> tomorrow.getYear.toString)
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Date of birth must be in the past"
        page.getElementsText(fieldLevelErrorDateOfBirth) shouldBe "Error: Date of birth must be in the past"
        page.getElementsText("title") should startWith("Error: ")
      }
    }
  }

  "NINO" should {
    "be mandatory" in {
      submitForm(NinoFormBuilder.asForm + ("nino" -> "")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe NinoPage
        page.getElementsText(NinoMatchPage.fieldLevelErrorNino) shouldBe NinoField
      }
    }

    "be valid" in {
      submitForm(NinoFormBuilder.asForm + ("nino" -> "AB123456E")) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe InvalidNinoPage
        page.getElementsText(NinoMatchPage.fieldLevelErrorNino) shouldBe InvalidNinoField
      }
    }
  }

  "submitting a form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.submit(defaultOrganisationType, atarService, Journey.Register)
    )

    "redirect to the confirm page when there's a successful match" in {
      when(
        mockMatchingService.matchIndividualWithNino(
          ArgumentMatchers.eq(NinoFormBuilder.Nino),
          ArgumentMatchers.eq(NinoFormBuilder.asIndividual),
          any()
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(true))

      submitForm(form = NinoFormBuilder.asForm) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe empty

        status(result) shouldBe SEE_OTHER
        result.header.headers("Location") should endWith("/customs-enrolment-services/atar/register/matching/confirm")

        verify(mockMatchingService).matchIndividualWithNino(any(), any(), any())(any[HeaderCarrier])
      }
    }

    "redisplay the nino matching page with the error displayed when there's no match" in {
      when(
        mockMatchingService.matchIndividualWithNino(
          ArgumentMatchers.eq(NinoFormBuilder.Nino),
          ArgumentMatchers.eq(NinoFormBuilder.asIndividual),
          any()
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(false))

      submitForm(form = NinoFormBuilder.asForm) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          NinoMatchPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Your details have not been found. Check that your details are correct and then try again."

        verify(mockMatchingService).matchIndividualWithNino(any(), any(), any())(any[HeaderCarrier])
      }
    }
  }

  def showForm(
    form: Map[String, String],
    organisationType: String = defaultOrganisationType,
    userId: String = defaultUserId
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    val result = controller
      .form(organisationType, atarService, Journey.Register)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

  def submitForm(
    form: Map[String, String],
    organisationType: String = defaultOrganisationType,
    userId: String = defaultUserId
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector)

    val result = controller
      .submit(organisationType, atarService, Journey.Register)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

}
