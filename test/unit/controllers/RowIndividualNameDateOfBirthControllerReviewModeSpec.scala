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

import common.pages.matching.{
  IndividualNameAndDateOfBirthPage,
  ThirdCountryIndividualNameAndDateOfBirthPage,
  ThirdCountrySoleTraderNameAndDateOfBirthPage
}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Prop
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.Checkers
import play.api.data.Form
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.RowIndividualNameDateOfBirthController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.RowIndividualNameDateOfBirthController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.row_individual_name_dob
import util.ControllerSpec
import util.builders.AuthActionMock
import util.scalacheck.TestDataGenerators

import java.time.LocalDate
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class RowIndividualNameDateOfBirthControllerReviewModeSpec
    extends ControllerSpec with Checkers with TestDataGenerators with BeforeAndAfterEach with ScalaFutures
    with AuthActionMock {

  class ControllerFixture(organisationType: String, form: Form[IndividualNameAndDateOfBirth])
      extends AbstractControllerFixture[RowIndividualNameDateOfBirthController] {
    val mockRegistrationInfo           = mock[IndividualRegistrationInfo]
    val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

    private val rowIndividualNameDob = instanceOf[row_individual_name_dob]
    private val mockAuthAction       = authAction(mockAuthConnector)

    override val controller = new RowIndividualNameDateOfBirthController(
      mockAuthAction,
      mockSubscriptionDetailsService,
      mcc,
      rowIndividualNameDob
    )(global)

    protected def show(с: RowIndividualNameDateOfBirthController): Action[AnyContent] =
      с.reviewForm(organisationType, atarService)

    protected def submit(c: RowIndividualNameDateOfBirthController): Action[AnyContent] =
//      c.submit(true, organisationType, atarService) //  Previous usual behavior DDCYLS-5614
      c.form(organisationType, atarService)

    def formData(thirdCountryIndividual: IndividualNameAndDateOfBirth): Map[String, String] =
      form.mapping.unbind(thirdCountryIndividual)

  }

  abstract class IndividualNameAndDateOfBirthBehaviour(
    webPage: IndividualNameAndDateOfBirthPage,
    form: Form[IndividualNameAndDateOfBirth],
    val validFormModelGens: IndividualGens[LocalDate]
  ) {

    protected val organisationType: String = webPage.organisationType

    "loading the page in review mode" should {

      withControllerFixture { controllerFixture =>
        assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
          controllerFixture.mockAuthConnector,
          controllerFixture.controller.reviewForm(organisationType, atarService)
        )
      }

      "show the form in review mode without errors, the input fields are prepopulated from the cache" in withControllerFixture {
        controllerFixture =>
          import controllerFixture._
          when(mockSubscriptionDetailsService.cachedNameDobDetails(any[Request[_]]))
            .thenReturn(Future.successful(Some(NameDobMatchModel("firstName", "lastName", LocalDate.of(1980, 3, 31)))))

          controllerFixture.showForm { result =>
//            status(result) shouldBe OK //  Previous usual behavior DDCYLS-5614
            status(result) shouldBe SEE_OTHER
            val page = CdsPage(contentAsString(result))
            page.getElementsText(webPage.pageLevelErrorSummaryListXPath) shouldBe empty

            //  Previous usual behavior(uncomment below tests) DDCYLS-5614
//            assertPresentOnPage(webPage.givenNameElement)
//            assertPresentOnPage(webPage.familyNameElement)
//            assertPresentOnPage(webPage.dateOfBirthElement)
//            page.getElementAttributeAction(webPage.formElement) shouldBe RowIndividualNameDateOfBirthController
//              .reviewForm(organisationType, atarService)
//              .url
//
//            page.getElementValue(webPage.givenNameElement) shouldBe "firstName"
//            page.getElementValue(webPage.familyNameElement) shouldBe "lastName"
//            page.getElementValue(webPage.dobDayElement) shouldBe "31"
//            page.getElementValue(webPage.dobMonthElement) shouldBe "3"
//            page.getElementValue(webPage.dobYearElement) shouldBe "1980"
          }
      }

      "should redirect to sign out page if cachedNameDobDetails not found" in withControllerFixture {
        controllerFixture =>
          import controllerFixture._
          when(mockSubscriptionDetailsService.cachedNameDobDetails(any[Request[_]])).thenReturn(Future.successful(None))

          controllerFixture.showForm { result =>
            status(result) shouldBe SEE_OTHER
//            result.futureValue.header.headers(LOCATION) shouldBe "/customs-registration-services/atar/register/sign-out" //  Previous usual behavior DDCYLS-5614
            result.futureValue.header.headers(
              LOCATION
            ) shouldBe "/customs-registration-services/atar/register/ind-st-use-a-different-service"
          }
      }
    }

    "submitting a valid form when review mode is true" should {

      withControllerFixture { controllerFixture =>
        assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
          controllerFixture.mockAuthConnector,
//          controllerFixture.controller.submit(true, organisationType, atarService) //  Previous usual behavior DDCYLS-5614
          controllerFixture.controller.reviewForm(organisationType, atarService)
        )
      }

      "redirect to 'review' page" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          when(mockSubscriptionDetailsService.cacheNameDobDetails(any[NameDobMatchModel])(any[Request[_]]))
            .thenReturn(Future.successful(()))

          submitForm(formData(individualNameAndDateOfBirth)) { result =>
            CdsPage(contentAsString(result)).getElementsHtml(webPage.pageLevelErrorSummaryListXPath) shouldBe empty
            status(result) shouldBe SEE_OTHER
            result.futureValue.header.headers(
              LOCATION
//            ) shouldBe "/customs-registration-services/atar/register/matching/review-determine" //  Previous usual behavior DDCYLS-5614
            ) shouldBe "/customs-registration-services/atar/register/ind-st-use-a-different-service"
//            verify(mockSubscriptionDetailsService).cacheNameDobDetails(any())(any()) //  Previous usual behavior(uncomment) DDCYLS-5614
          }
      }
    }

    protected def withControllerFixture[TestResult](body: ControllerFixture => TestResult): TestResult =
      body(new ControllerFixture(organisationType, form))

    protected def testControllerWithModel(
      formModelGens: IndividualGens[LocalDate]
    )(test: (ControllerFixture, IndividualNameAndDateOfBirth) => Unit): Unit =
      check(Prop.forAllNoShrink(individualNameAndDateOfBirthGenerator(formModelGens)) { individualNameAndDateOfBirth =>
        withControllerFixture { controllerFixture =>
          test.apply(controllerFixture, individualNameAndDateOfBirth)
          Prop.proved
        }
      })

  }

  abstract class ThirdCountryIndividualBehaviour(webPage: IndividualNameAndDateOfBirthPage)
      extends IndividualNameAndDateOfBirthBehaviour(
        webPage,
        form = MatchingForms.thirdCountryIndividualNameDateOfBirthForm,
        validFormModelGens = individualNameAndDateOfBirthGens()
      ) {}

  case object ThirdCountrySoleTraderBehavior
      extends ThirdCountryIndividualBehaviour(ThirdCountrySoleTraderNameAndDateOfBirthPage)

  case object ThirdCountryIndividualBehavior
      extends ThirdCountryIndividualBehaviour(ThirdCountryIndividualNameAndDateOfBirthPage)

  "The third country sole trader case" when (behave like ThirdCountrySoleTraderBehavior)

  "The third country individual case" when (behave like ThirdCountryIndividualBehavior)
}
