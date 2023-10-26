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

import common.pages.matching.NameDateOfBirthPage
import common.pages.matching.NameDateOfBirthPage._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.NameDobController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_namedob
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder
import util.builders.matching.IndividualIdFormBuilder._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NameDobControllerSpec extends ControllerSpec with BeforeAndAfterEach with SubscriptionFlowTestSupport {
  protected override val formId: String      = NameDateOfBirthPage.formId
  val mockCdsFrontendDataCache: SessionCache = mock[SessionCache]
  private val matchNameDobView               = instanceOf[match_namedob]

  private def nameDobController =
    new NameDobController(mockAuthAction, mcc, matchNameDobView, mockCdsFrontendDataCache)

  val defaultOrganisationType = "individual"
  val soleTraderType          = "sole-trader"

  def maxLengthError(maxLength: Int, field: String): String =
    s"The $field name must be $maxLength characters or less"

  override def beforeEach(): Unit =
    when(mockCdsFrontendDataCache.saveSubscriptionDetails(any[SubscriptionDetails])(any[Request[_]]))
      .thenReturn(Future.successful(true))

  "Viewing the Individual Matching with ID form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      nameDobController.form(defaultOrganisationType, atarService)
    )

    "display the form" in {
      showForm(soleTraderType) { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.getElementsHtml(pageLevelErrorSummaryListXPath) shouldBe empty
        page.getElementsText(firstName) shouldBe empty
        page.getElementsText(lastName) shouldBe empty
      }
    }
  }

  "first name" should {
    "be mandatory" in {
      submitForm(form = ValidRequest ++ Map("first-name" -> ""), defaultOrganisationType) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your first name"
        page.getElementsText(fieldLevelErrorFirstName) shouldBe "Error: Enter your first name"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    s"be restricted to 35 characters" in {
      val firstNameMaxLength = 35
      submitForm(ValidRequest ++ Map("first-name" -> oversizedString(firstNameMaxLength)), defaultOrganisationType) {
        result =>
          status(result) shouldBe BAD_REQUEST
          val page = CdsPage(contentAsString(result))
          page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe maxLengthError(firstNameMaxLength, "first")
          page.getElementsText(fieldLevelErrorFirstName) shouldBe s"Error: ${maxLengthError(firstNameMaxLength, "first")}"
          page.getElementsText("title") should startWith("Error: ")
      }
    }
  }

  "last name" should {

    "be mandatory" in {
      submitForm(ValidRequest ++ Map("last-name" -> ""), defaultOrganisationType) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your last name"
        page.getElementsText(fieldLevelErrorLastName) shouldBe "Error: Enter your last name"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "be restricted to 35 characters" in {
      val lastNameMaxLength = 35
      submitForm(ValidRequest ++ Map("last-name" -> oversizedString(lastNameMaxLength)), soleTraderType) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe maxLengthError(lastNameMaxLength, "last")
        page.getElementsText(fieldLevelErrorLastName) shouldBe s"Error: ${maxLengthError(lastNameMaxLength, "last")}"
        page.getElementsText("title") should startWith("Error: ")
      }
    }
  }

  "date of birth" should {

    "be mandatory" in {
      submitForm(
        ValidRequest ++ Map("date-of-birth.day" -> "", "date-of-birth.month" -> "", "date-of-birth.year" -> ""),
        defaultOrganisationType
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe "Enter your date of birth"
        page.getElementsText(fieldLevelErrorDateOfBirth) shouldBe "Error: Enter your date of birth"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "be a valid date" in {
      submitForm(ValidRequest ++ Map("date-of-birth.day" -> "32"), defaultOrganisationType) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe messages("date.day.error")
        page.getElementsText(fieldLevelErrorDateOfBirth) shouldBe s"Error: ${messages("date.day.error")}"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "not be in the future" in {
      val tomorrow        = LocalDate.now().plusDays(1)
      val futureDateError = "Year must be between 1900 and this year"
      submitForm(
        ValidRequest ++ Map(
          "date-of-birth.day"   -> tomorrow.getDayOfMonth.toString,
          "date-of-birth.month" -> tomorrow.getMonthValue.toString,
          "date-of-birth.year"  -> tomorrow.getYear.toString
        ),
        soleTraderType
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe futureDateError
        page.getElementsText(fieldLevelErrorDateOfBirth) shouldBe s"Error: $futureDateError"
        page.getElementsText("title") should startWith("Error: ")
      }
    }

    "display an appropriate message when letters are entered instead of numbers" in {
      submitForm(
        ValidRequest ++ Map("date-of-birth.day" -> "a", "date-of-birth.month" -> "b", "date-of-birth.year" -> "c"),
        defaultOrganisationType
      ) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))

        page.getElementsText(pageLevelErrorSummaryListXPath) shouldBe s"Date of birth must be a real date"
        page.getElementsText(fieldLevelErrorDateOfBirth) shouldBe s"Error: Date of birth must be a real date"
        page.getElementsText("title") should startWith("Error: ")
      }
    }
  }

  "Submitting the individual form" should {

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      nameDobController.submit(defaultOrganisationType, atarService)
    )

    "be successful when all mandatory fields filled" in {
      submitForm(ValidRequest, defaultOrganisationType) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "redirect to the confirm page when successful" in {
      submitForm(ValidRequest, defaultOrganisationType) { result =>
        status(result) shouldBe SEE_OTHER
        header("Location", result).value should endWith(
          "/customs-registration-services/atar/register/matching/chooseid"
        )
      }
    }
  }

  def showForm(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    val result =
      nameDobController.form(defaultOrganisationType, atarService).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitForm(form: Map[String, String], organisationType: String, userId: String = defaultUserId)(
    test: Future[Result] => Any
  ): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)

    val result = nameDobController
      .submit(organisationType, atarService)
      .apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form))

    test(result)
  }

}
