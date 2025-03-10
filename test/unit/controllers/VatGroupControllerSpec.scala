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

import common.pages.registration.VatGroupPage
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.EmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{routes, VatGroupController}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_group
import util.ControllerSpec
import util.builders.YesNoFormBuilder.{invalidRequest, validRequest}
import util.builders.{AuthActionMock, SessionBuilder}

import java.util.UUID
import scala.concurrent.Future

class VatGroupControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val yesNoInputName         = "yes-no-answer"
  private val answerYes              = true.toString
  private val answerNo               = false.toString
  private val expectedYesRedirectUrl = routes.VatGroupsCannotRegisterUsingThisServiceController.form(atarService).url
  private val expectedNoRedirectUrl  = EmailController.form(atarService).url

  private val vatGroupView = inject[vat_group]

  private val controller =
    new VatGroupController(mcc, vatGroupView)

  "Accessing the page" should {

    "allow unauthenticated users to access the yes no answer form" in {
      showForm() { result =>
        status(result) shouldBe OK
        CdsPage(contentAsString(result)).title() should startWith(VatGroupPage.title)
      }
    }
  }

  "submitting the form" should {

    "ensure an option has been selected" in {
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          VatGroupPage.pageLevelErrorSummaryListXPath
        ) shouldBe VatGroupPage.problemWithSelectionError
        page.getElementsText(
          VatGroupPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: ${VatGroupPage.problemWithSelectionError}"
      }
    }

    "ensure a valid option has been selected" in {
      val invalidOption = UUID.randomUUID.toString
      submitForm(validRequest + (yesNoInputName -> invalidOption)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          VatGroupPage.pageLevelErrorSummaryListXPath
        ) shouldBe VatGroupPage.problemWithSelectionError
        page.getElementsText(
          VatGroupPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: ${VatGroupPage.problemWithSelectionError}"
      }
    }

    "redirect to Cannot Register Using This Service when 'yes' " in {
      submitForm(validRequest + (yesNoInputName -> answerYes)) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith(expectedYesRedirectUrl)
      }
    }

    "redirect to EmailController.form when 'no' " in {
      submitForm(validRequest + (yesNoInputName -> answerNo)) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value shouldBe expectedNoRedirectUrl
      }
    }

  }

  def showForm()(test: Future[Result] => Any): Unit =
    test(controller.createForm(atarService).apply(request = SessionBuilder.buildRequestWithSessionNoUserAndToken()))

  def submitForm(form: Map[String, String])(test: Future[Result] => Any): Unit =
    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithFormValues(form)))

}
