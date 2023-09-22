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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.VatReturnController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionBusinessService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{vat_return_total, we_cannot_confirm_your_identity}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatReturnControllerSpec extends ControllerSpec with AuthActionMock with BeforeAndAfterEach {

  private val mockVatReturnTotalView          = instanceOf[vat_return_total]
  private val mockWeCannotConfirmYourIdentity = instanceOf[we_cannot_confirm_your_identity]
  private val mockSubscriptionBusinessService = mock[SubscriptionBusinessService]

  val mockAuthConnector = mock[AuthConnector]
  val mockAuthAction    = authAction(mockAuthConnector)

  private val controller = new VatReturnController(
    mockAuthAction,
    mockSubscriptionBusinessService,
    mcc,
    mockVatReturnTotalView,
    mockWeCannotConfirmYourIdentity
  )

  private val vatControlListResponse = VatControlListResponse(
    postcode = Some("SE28 1AA"),
    dateOfReg = Some("2017-01-01"),
    lastNetDue = Some(10000.02d),
    lastReturnMonthPeriod = Option("MAR")
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSubscriptionBusinessService.getCachedVatControlListResponse(any[Request[_]])).thenReturn(
      Future.successful(Some(vatControlListResponse))
    )

  }

  "VAT Return amount input Controller" should {
    "return OK when accessing page though createForm method" in {
      createForm() { result =>
        status(result) shouldBe OK
      }
    }
  }

  "Submitting Vat amount" should {

    "be successful when submitted with valid input of 2dp and data matches API response" in {
      val validReturnTotal: Map[String, String] = Map("vat-return-total" -> "10000.02")
      submitForm(validReturnTotal) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "redirect to cannot verify your details when valid input supplied but not matching API response" in {
      val validReturnTotal: Map[String, String] = Map("vat-return-total" -> "100.02")
      submitForm(validReturnTotal) { result =>
        status(result) shouldBe SEE_OTHER
      }
    }

    "return to the same location with bad request when submitting invalid request" in {
      val invalidVatAmountInputSequence: Seq[Map[String, String]] =
        Seq(
          Map("vat-return-total" -> "100"),
          Map("vat-return-total" -> "100.0"),
          Map("vat-return-total" -> "hundred"),
          Map("vat-return-total" -> "hello 20.100"),
          Map("vat-return-total" -> "20.100 hello"),
          Map("vat-return-total" -> ".25"),
          Map("vat-return-total" -> "2.10000"),
          Map("vat-return-total" -> "23.23.23")
        )

      invalidVatAmountInputSequence.foreach { invalidVatInput =>
        submitForm(invalidVatInput) { result =>
          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }
  "redirectToCannotConfirmIdentity" should {
    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(
      mockAuthConnector,
      controller.redirectToCannotConfirmIdentity(atarService)
    )

    "display redirectToCannotConfirmIdentity" in {
      redirectToCannotConfirmIdentity() {
        result =>
          status(result) shouldBe OK
          CdsPage(contentAsString(result)).title() should startWith("We cannot verify your VAT details")
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

  private def redirectToCannotConfirmIdentity(userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.redirectToCannotConfirmIdentity(atarService).apply(SessionBuilder.buildRequestWithSession(userId)))
  }

}
