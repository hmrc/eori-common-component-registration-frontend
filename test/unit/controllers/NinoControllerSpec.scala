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

import common.pages.NinoMatchPage
import common.pages.matching.NameDateOfBirthPage.{fieldLevelErrorDateOfBirth, pageLevelErrorSummaryListXPath}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfter
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.NinoController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{MatchingResponse, RegisterWithIDResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCacheService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, match_nino}
import uk.gov.hmrc.http.HeaderCarrier
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.matching.NinoFormBuilder
import util.builders.{AuthActionMock, SessionBuilder}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NinoControllerSpec extends ControllerSpec with BeforeAndAfter with AuthActionMock {

  private val mockAuthConnector = mock[AuthConnector]
  private val mockAuthAction = authAction(mockAuthConnector)
  private val mockMatchingService = mock[MatchingService]
  private val errorView = inject[error_template]
  private val mockSessionCacheService = inject[SessionCacheService]

  private val matchNinoView = inject[match_nino]

  val controller =
    new NinoController(mockAuthAction, mcc, matchNinoView, mockMatchingService, errorView, mockSessionCacheService)(
      global
    )

  before {
    Mockito.reset(mockMatchingService)
  }

  val defaultOrganisationType = "individual"

  val FirstNamePage = "Enter your first name"
  val FirstNameField = "Error: Enter your first name"
  val LastNamePage = "Enter your last name"
  val LastNameField = "Error: Enter your last name"
  val NinoPage = "Enter your National Insurance number"
  val NinoField = "Error: Enter your National Insurance number"
  val DateOfBirth = "Date of birth"

  val InvalidNinoPage = "This is not a real National Insurance number. Enter a real National Insurance number."
  val InvalidNinoField = "Error: This is not a real National Insurance number. Enter a real National Insurance number."

  "loading the page" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.form(defaultOrganisationType, atarService)
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
        NinoFormBuilder.asForm ++ Map(
          "date-of-birth.day"   -> "",
          "date-of-birth.month" -> "",
          "date-of-birth.year"  -> ""
        )
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
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe messages("date.day.error")
        page.getElementsText(NinoMatchPage.fieldLevelErrorDateOfBirth) shouldBe s"Error: ${messages("date.day.error")}"
      }
    }

    "not be in the future " in {
      val tomorrow = LocalDate.now().plusDays(1)
      submitForm(
        NinoFormBuilder.asForm ++ Map(
          "date-of-birth.day"   -> tomorrow.getDayOfMonth.toString,
          "date-of-birth.month" -> tomorrow.getMonthValue.toString,
          "date-of-birth.year"  -> tomorrow.getYear.toString
        )
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Year must be between 1900 and this year"
        page.getElementsText(fieldLevelErrorDateOfBirth) shouldBe "Error: Year must be between 1900 and this year"
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
      controller.submit(defaultOrganisationType, atarService)
    )

    "redirect to the confirm page when there's a successful match" in {
      when(
        mockMatchingService.matchIndividualWithNino(
          ArgumentMatchers.eq(NinoFormBuilder.Nino),
          ArgumentMatchers.eq(NinoFormBuilder.asIndividual),
          any()
        )(any[HeaderCarrier], any[Request[_]])
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

      submitForm(form = NinoFormBuilder.asForm) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementsText(NinoMatchPage.pageLevelErrorSummaryListXPath) shouldBe empty

        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith("/customs-registration-services/atar/register/matching/confirm")

        verify(mockMatchingService).matchIndividualWithNino(any(), any(), any())(any[HeaderCarrier], any[Request[_]])
      }
    }

    "redisplay the nino matching page with the error displayed when there's no match" in {
      when(
        mockMatchingService.matchIndividualWithNino(
          ArgumentMatchers.eq(NinoFormBuilder.Nino),
          ArgumentMatchers.eq(NinoFormBuilder.asIndividual),
          any()
        )(any[HeaderCarrier], any[Request[_]])
      ).thenReturn(eitherT[MatchingResponse](MatchingServiceConnector.matchFailureResponse))

      submitForm(form = NinoFormBuilder.asForm) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          NinoMatchPage.pageLevelErrorSummaryListXPath
        ) shouldBe "Your details have not been found. Check that your details are correct and then try again."

        verify(mockMatchingService).matchIndividualWithNino(any(), any(), any())(any[HeaderCarrier], any[Request[_]])
      }
    }

    "redirect to error-template page when downstreamFailureResponse occurred" in {
      when(
        mockMatchingService.matchIndividualWithNino(
          ArgumentMatchers.eq(NinoFormBuilder.Nino),
          ArgumentMatchers.eq(NinoFormBuilder.asIndividual),
          any()
        )(any[HeaderCarrier], any[Request[_]])
      ).thenReturn(eitherT[MatchingResponse](MatchingServiceConnector.downstreamFailureResponse))

      submitForm(form = NinoFormBuilder.asForm) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml("h1") shouldBe messages("cds.error.title")

        verify(mockMatchingService).matchIndividualWithNino(any(), any(), any())(any[HeaderCarrier], any[Request[_]])
      }
    }

    "redirect to error-template page when other errors occurred" in {
      when(
        mockMatchingService.matchIndividualWithNino(
          ArgumentMatchers.eq(NinoFormBuilder.Nino),
          ArgumentMatchers.eq(NinoFormBuilder.asIndividual),
          any()
        )(any[HeaderCarrier], any[Request[_]])
      ).thenReturn(eitherT[MatchingResponse](MatchingServiceConnector.otherErrorHappen))

      submitForm(form = NinoFormBuilder.asForm) { result =>
        status(result) shouldBe INTERNAL_SERVER_ERROR
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml("h1") shouldBe messages("cds.error.title")

        verify(mockMatchingService).matchIndividualWithNino(any(), any(), any())(any[HeaderCarrier], any[Request[_]])
      }
    }

  }

  def showForm(
    form: Map[String, String],
    organisationType: String = defaultOrganisationType,
    userId: String = defaultUserId
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    val result = controller
      .form(organisationType, atarService)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

  def submitForm(
    form: Map[String, String],
    organisationType: String = defaultOrganisationType,
    userId: String = defaultUserId
  )(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    val result = controller
      .submit(organisationType, atarService)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))
    test(result)
  }

}
