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

import java.util.UUID

import common.pages.registration.VatRegisteredUkPage
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.VatRegisteredUkController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.vat_registered_uk
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.{AuthActionMock, SessionBuilder}
import util.builders.YesNoFormBuilder._

import scala.concurrent.Future

class VatRegisteredUkControllerSpec extends ControllerSpec with AuthActionMock {

  private val yesNoInputName         = "yes-no-answer"
  private val answerYes              = true.toString
  private val answerNo               = false.toString
  private val expectedYesRedirectUrl = s"https://www.tax.service.gov.uk/shortforms/form/EORIVAT?details=&vat=yes"
  private val expectedNoRedirectUrl  = s"https://www.tax.service.gov.uk/shortforms/form/EORINonVATImport?details=&vat=no"

  private val vatRegisteredUkView = instanceOf[vat_registered_uk]

  val controller = new VatRegisteredUkController(vatRegisteredUkView, mcc)

  "Accessing the page" should {

    "allow unauthenticated users to access the yes no answer form" in {
      showForm() { result =>
        status(result) shouldBe OK
        CdsPage(contentAsString(result)).title should startWith("Is your organisation VAT registered in the UK?")
      }
    }
  }

  "submitting the form" should {

    "ensure an option has been selected" in {
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          VatRegisteredUkPage.pageLevelErrorSummaryListXPath
        ) shouldBe VatRegisteredUkPage.problemWithSelectionError
        page.getElementsText(
          VatRegisteredUkPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: ${VatRegisteredUkPage.problemWithSelectionError}"
      }
    }

    "ensure a valid option has been selected" in {
      val invalidOption = UUID.randomUUID.toString
      submitForm(ValidRequest + (yesNoInputName -> invalidOption)) { result =>
        status(result) shouldBe BAD_REQUEST
        val page = CdsPage(contentAsString(result))
        page.getElementsText(
          VatRegisteredUkPage.pageLevelErrorSummaryListXPath
        ) shouldBe VatRegisteredUkPage.problemWithSelectionError
        page.getElementsText(
          VatRegisteredUkPage.fieldLevelErrorYesNoAnswer
        ) shouldBe s"Error: ${VatRegisteredUkPage.problemWithSelectionError}"
      }
    }

    "redirect to VAT KANA form when 'yes' is selected" in {
      submitForm(ValidRequest + (yesNoInputName -> answerYes)) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith(expectedYesRedirectUrl)
      }
    }

    "redirect to non-VAT KANA form when 'no' is selected" in {
      submitForm(ValidRequest + (yesNoInputName -> answerNo)) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) shouldBe expectedNoRedirectUrl
      }
    }
  }

  def showForm()(test: Future[Result] => Any) {
    test(controller.form(atarService).apply(request = SessionBuilder.buildRequestWithSessionNoUserAndToken))
  }

  def submitForm(form: Map[String, String])(test: Future[Result] => Any) {
    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithFormValues(form)))
  }

}
