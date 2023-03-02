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

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import uk.gov.hmrc.auth.core.AuthConnector
import play.api.mvc.Result
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.VatVerificationOptionController
import util.ControllerSpec
import util.builders.{AuthActionMock, SessionBuilder}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import util.builders.AuthBuilder.withAuthorisedUser
import play.api.test.Helpers.{LOCATION, _}
import unit.controllers.VatVerificationOptionBuilder._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatVerificationOptionControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val vatVerificationOptionView = instanceOf[vat_verification_option]
  private val mockAuthConnector         = mock[AuthConnector]
  private val mockAuthAction            = authAction(mockAuthConnector)

  private val controller = new VatVerificationOptionController(mockAuthAction, mcc, vatVerificationOptionView)

  "VAT Verification Option Controller" should {
    "return OK when accessing page through createForm method" in {
      createForm() { result =>
        status(result) shouldBe OK
      }
    }
  }

  "Submitting VAT verification option" should {
    "return to the same location with bad request when submitting invalid request" in {
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to VAT details page for 'date' option" in {
      submitForm(validRequestDate) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/your-uk-vat-details")
      }
    }

    "redirect to cannot verify details page for 'amount' option" in {
      submitForm(validRequestAmount) { result =>
        status(result) shouldBe SEE_OTHER
        result.header.headers(LOCATION) should endWith("/your-uk-vat-details-return")
      }
    }
  }

  private def createForm()(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.createForm(atarService).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String])(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.submit(atarService).apply(SessionBuilder.buildRequestWithFormValues(form)))
  }

}

object VatVerificationOptionBuilder {
  private val dateOrAmountInput = "vat-verification-option"
  private val answerDate        = true.toString
  private val answerAmount      = false.toString
  private val invalidOption     = ""

  val validRequestDate: Map[String, String]   = Map(dateOrAmountInput -> answerDate)
  val validRequestAmount: Map[String, String] = Map(dateOrAmountInput -> answerAmount)
  val invalidRequest: Map[String, String]     = Map(dateOrAmountInput -> invalidOption)
}
