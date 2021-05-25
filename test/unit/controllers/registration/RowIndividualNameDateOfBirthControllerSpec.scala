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

import common.pages.matching.{
  IndividualNameAndDateOfBirthPage,
  ThirdCountryIndividualNameAndDateOfBirthPage,
  ThirdCountrySoleTraderNameAndDateOfBirthPage
}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalacheck.Prop
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.Checkers
import play.api.data.Form
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.RowIndividualNameDateOfBirthController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{IndividualNameAndDateOfBirth, NameDobMatchModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.row_individual_name_dob
import uk.gov.hmrc.http.HeaderCarrier
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthActionMock
import util.scalacheck.TestDataGenerators
import util.scalacheck.TestDataGenerators.Implicits._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RowIndividualNameDateOfBirthControllerSpec
    extends ControllerSpec with Checkers with TestDataGenerators with BeforeAndAfterEach with ScalaFutures
    with AuthActionMock {

  class ControllerFixture(organisationType: String, form: Form[IndividualNameAndDateOfBirth])
      extends AbstractControllerFixture[RowIndividualNameDateOfBirthController] {
    val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

    private val rowIndividualNameDob = instanceOf[row_individual_name_dob]
    private val mockAuthAction       = authAction(mockAuthConnector)

    override val controller = new RowIndividualNameDateOfBirthController(
      mockAuthAction,
      mockSubscriptionDetailsService,
      mcc,
      rowIndividualNameDob
    )(global)

    def saveRegistrationDetailsMockSuccess() {
      when(mockSubscriptionDetailsService.cacheNameDobDetails(any[NameDobMatchModel])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
    }

    def registerIndividualMockFailure(exception: Throwable) {
      when(mockSubscriptionDetailsService.cacheNameDobDetails(any[NameDobMatchModel])(any[HeaderCarrier]))
        .thenReturn(Future.failed(exception))
    }

    protected def show(с: RowIndividualNameDateOfBirthController): Action[AnyContent] =
      с.form(organisationType, atarService, Journey.Register)

    protected def submit(c: RowIndividualNameDateOfBirthController): Action[AnyContent] =
      c.submit(false, organisationType, atarService, Journey.Register)

    def formData(thirdCountryIndividual: IndividualNameAndDateOfBirth): Map[String, String] =
      form.mapping.unbind(thirdCountryIndividual)

  }

  val emulatedFailure = new UnsupportedOperationException("Emulation of service call failure")

  abstract class IndividualNameAndDateOfBirthBehaviour(
    webPage: IndividualNameAndDateOfBirthPage,
    form: Form[IndividualNameAndDateOfBirth],
    val validFormModelGens: IndividualGens[LocalDate]
  ) {

    protected val organisationType: String = webPage.organisationType

    "loading the page" should {

      withControllerFixture { controllerFixture =>
        assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
          controllerFixture.mockAuthConnector,
          controllerFixture.controller.form(organisationType, atarService, Journey.Register)
        )
      }

      "show the form without errors when user hasn't been registered yet" in withControllerFixture {
        controllerFixture =>
          controllerFixture.showForm { result =>
            status(result) shouldBe OK
            val page = CdsPage(contentAsString(result))
            page.getElementsText(webPage.pageLevelErrorSummaryListXPath) shouldBe empty

            val assertPresentOnPage = controllerFixture.assertPresentOnPage(page) _

            assertPresentOnPage(webPage.givenNameElement)
            assertPresentOnPage(webPage.middleNameElement)
            assertPresentOnPage(webPage.familyNameElement)
            assertPresentOnPage(webPage.dateOfBirthElement)
            page.getElementAttributeAction(
              webPage.formElement
            ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.RowIndividualNameDateOfBirthController
              .form(organisationType, atarService, Journey.Register)
              .url
          }
      }
    }

    "submitting a valid form when review mode is false" should {

      withControllerFixture { controllerFixture =>
        assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
          controllerFixture.mockAuthConnector,
          controllerFixture.controller.submit(false, organisationType, atarService, Journey.Register)
        )
      }

      "redirect to 'do you have a utr number' page" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          saveRegistrationDetailsMockSuccess()

          submitForm(formData(individualNameAndDateOfBirth)) { result =>
            CdsPage(contentAsString(result)).getElementsHtml(webPage.pageLevelErrorSummaryListXPath) shouldBe empty
            status(result) shouldBe SEE_OTHER
            result.futureValue.header.headers(
              LOCATION
            ) shouldBe s"/customs-enrolment-services/atar/register/matching/utr/$organisationType"
            verify(mockSubscriptionDetailsService).cacheNameDobDetails(any())(any())
          }
      }

      "wait until the registration request is completed" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          registerIndividualMockFailure(emulatedFailure)

          val caught = intercept[RuntimeException] {
            submitForm(formData(individualNameAndDateOfBirth)) { result =>
              await(result)
            }
          }
          caught shouldBe emulatedFailure
      }
    }

    "given name" should {
      import webPage.{fieldLevelErrorGivenName, givenNameField, GivenName}

      "be mandatory" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(formData(individualNameAndDateOfBirth) + (givenNameField -> ""), webPage)(
            givenNameField,
            fieldLevelErrorGivenName,
            "Enter your given name"
          )
      }

      "not be empty" in testControllerWithModel(validFormModelGens.copy(firstNameGen = emptyString)) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(formData(individualNameAndDateOfBirth), webPage)(
            givenNameField,
            fieldLevelErrorGivenName,
            "Enter your given name"
          )
      }

      "not allow invalid characters" in testControllerWithModel(validFormModelGens.copy(firstNameGen = emptyString)) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(formData(individualNameAndDateOfBirth) + (givenNameField -> "!!!!!!''''!!!"), webPage)(
            givenNameField,
            fieldLevelErrorGivenName,
            "Enter a given name without invalid characters"
          )
      }

      "be restricted to 35 characters" in testControllerWithModel(
        validFormModelGens.copy(firstNameGen = oversizedNameGenerator())
      ) { (controllerFixture, individualNameAndDateOfBirth) =>
        import controllerFixture._
        assertInvalidField(formData(individualNameAndDateOfBirth), webPage)(
          GivenName,
          fieldLevelErrorGivenName,
          "The given name must be 35 characters or less"
        )
      }
    }

    "middle name" should {
      import webPage.{fieldLevelErrorMiddleName, middleNameField, MiddleName}

      passOptionalFieldCheck()(emptyFieldModelGens = validFormModelGens.copy(middleNameGen = emptyString.asOption))

      "not allow invalid characters" in testControllerWithModel(
        validFormModelGens.copy(middleNameGen = emptyString.asOption)
      ) { (controllerFixture, individualNameAndDateOfBirth) =>
        import controllerFixture._
        assertInvalidField(formData(individualNameAndDateOfBirth) + (middleNameField -> "!!!!!!''''!!!"), webPage)(
          middleNameField,
          fieldLevelErrorMiddleName,
          "Enter a middle name without invalid characters"
        )
      }

      "be restricted to 35 characters" in testControllerWithModel(
        validFormModelGens.copy(middleNameGen = oversizedNameGenerator().asMandatoryOption)
      ) { (controllerFixture, individualNameAndDateOfBirth) =>
        import controllerFixture._
        assertInvalidField(formData(individualNameAndDateOfBirth), webPage)(
          MiddleName,
          fieldLevelErrorMiddleName,
          "The middle name must be 35 characters or less"
        )
      }
    }

    "family name" should {
      import webPage.{familyNameField, fieldLevelErrorFamilyName, FamilyName}

      "be mandatory" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(formData(individualNameAndDateOfBirth) + (familyNameField -> ""), webPage)(
            familyNameField,
            fieldLevelErrorFamilyName,
            "Enter your family name"
          )
      }

      "not be empty" in testControllerWithModel(validFormModelGens.copy(lastNameGen = emptyString)) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(formData(individualNameAndDateOfBirth), webPage)(
            familyNameField,
            fieldLevelErrorFamilyName,
            "Enter your family name"
          )
      }

      "not allow invalid characters" in testControllerWithModel(
        validFormModelGens.copy(middleNameGen = emptyString.asOption)
      ) { (controllerFixture, individualNameAndDateOfBirth) =>
        import controllerFixture._
        assertInvalidField(formData(individualNameAndDateOfBirth) + (familyNameField -> "!!!!!!''''!!!"), webPage)(
          familyNameField,
          fieldLevelErrorFamilyName,
          "Enter a family name without invalid characters"
        )
      }

      "be restricted to 35 characters" in testControllerWithModel(
        validFormModelGens.copy(lastNameGen = oversizedNameGenerator())
      ) { (controllerFixture, individualNameAndDateOfBirth) =>
        import controllerFixture._
        assertInvalidField(formData(individualNameAndDateOfBirth), webPage)(
          FamilyName,
          fieldLevelErrorFamilyName,
          "The family name must be 35 characters or less"
        )
      }
    }

    "date of birth" should {
      import webPage._

      "be mandatory" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(
            formData(individualNameAndDateOfBirth).filterKeys(!webPage.dateOfBirthFields.contains(_)),
            webPage
          )(DateOfBirth, fieldLevelErrorDateOfBirth, "Enter your date of birth")
      }

      "not be empty" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          val emptyDateFields: Map[String, String] = webPage.dateOfBirthFields.map(_ -> "").toMap
          assertInvalidField(formData(individualNameAndDateOfBirth) ++ emptyDateFields, webPage)(
            DateOfBirth,
            fieldLevelErrorDateOfBirth,
            "Enter your date of birth"
          )
      }

      "be a valid date" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          assertInvalidField(formData(individualNameAndDateOfBirth) + (dateOfBirthDayField -> "32"), webPage)(
            DateOfBirth,
            fieldLevelErrorDateOfBirth,
            "Enter a day between 1 and 31"
          )
      }

      "not be in the future " in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          val tomorrow   = LocalDate.now().plusDays(1)
          val FutureDate = "Date of birth must be in the past"
          import controllerFixture._
          assertInvalidField(
            formData(individualNameAndDateOfBirth) + (dateOfBirthDayField -> tomorrow.getDayOfMonth.toString,
            dateOfBirthMonthField                                         -> tomorrow.getMonthOfYear.toString,
            dateOfBirthYearField                                          -> tomorrow.getYear.toString),
            webPage
          )(DateOfBirth, fieldLevelErrorDateOfBirth, FutureDate)
      }

      "reject letters entered instead of numbers" in testControllerWithModel(validFormModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          val lettersDateFields: Map[String, String] = webPage.dateOfBirthFields.zip(List("1", "May", "2000")).toMap
          assertInvalidField(formData(individualNameAndDateOfBirth) ++ lettersDateFields, webPage)(
            DateOfBirth,
            fieldLevelErrorDateOfBirth,
            "Enter a month between 1 and 12"
          )
      }
    }

    protected def withControllerFixture[TestResult](body: ControllerFixture => TestResult): TestResult =
      body(new ControllerFixture(organisationType, form))

    protected def passOptionalFieldCheck()(
      formModelGens: IndividualGens[LocalDate] = validFormModelGens,
      emptyFieldModelGens: IndividualGens[LocalDate]
    ): Unit = {
      "be optional" in testControllerWithModel(formModelGens) { (controllerFixture, individualNameAndDateOfBirth) =>
        import controllerFixture._
        saveRegistrationDetailsMockSuccess()

        submitForm(formData(individualNameAndDateOfBirth)) { result =>
          status(result) shouldBe SEE_OTHER
          CdsPage(contentAsString(result)).getElementsHtml(webPage.pageLevelErrorSummaryListXPath) shouldBe empty
        }
      }

      "accept empty string" in testControllerWithModel(emptyFieldModelGens) {
        (controllerFixture, individualNameAndDateOfBirth) =>
          import controllerFixture._
          saveRegistrationDetailsMockSuccess()

          submitForm(formData(individualNameAndDateOfBirth)) { result =>
            status(result) shouldBe SEE_OTHER
            CdsPage(contentAsString(result)).getElementsHtml(webPage.pageLevelErrorSummaryListXPath) shouldBe empty
          }
      }
    }

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
