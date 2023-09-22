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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{LOCATION, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.VatVerificationOptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import unit.controllers.VatVerificationOptionBuilder._
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatVerificationOptionControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val vatVerificationOptionView      = instanceOf[vat_verification_option]
  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockSubscriptionDetailsService = mock[SubscriptionDetailsService]

  private val controller =
    new VatVerificationOptionController(mockAuthAction, mcc, mockSubscriptionDetailsService, vatVerificationOptionView)

  "VAT Verification Option Controller" should {
    "return OK when accessing page through createForm method" in {
      createForm() { result =>
        status(result) shouldBe OK
      }
    }
  }

  "Submitting VAT verification option" should {
    when(mockSubscriptionDetailsService.cacheVatVerificationOption(any())(any[Request[_]])).thenReturn(
      Future.successful((): Unit)
    )
    "return to the same location with bad request when submitting invalid request" in {
      submitForm(invalidRequest) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to VAT details page for 'date' option" in {
      submitForm(validRequestDate) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("/your-uk-vat-details-date")
      }
    }

    "redirect to cannot verify details page for 'amount' option" in {
      submitForm(validRequestAmount) { result =>
        status(result) shouldBe SEE_OTHER
        header(LOCATION, result).value should endWith("/your-uk-vat-details-return")
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
