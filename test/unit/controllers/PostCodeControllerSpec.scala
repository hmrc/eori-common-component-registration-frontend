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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.PostCodeController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.PostCodeController as PostCodeControllerRoutes
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.PostcodeForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.PostcodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.postcode
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.SessionBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PostCodeControllerSpec extends SubscriptionFlowTestSupport with BeforeAndAfterEach with SubscriptionFlowCreateModeTestSupport {

  val defaultOrganisationType = "individual"
  val soleTraderType = "sole-trader"

  override protected val formId: String = "addressDetailsForm"

  val form: Map[String, String] = Map("postcode" -> "TF3 2BX", "addressLine1" -> "addressline 1")
  val invalidForm: Map[String, String] = Map("addressLine1" -> "addressline 1")

  def submitInCreateModeUrl: String = PostCodeControllerRoutes.submit(atarService).url

  private val mockSessionCache = mock[SessionCache]
  private val view = inject[postcode]
  private val mockPostcodeForm = mock[PostcodeForm]

  private val controller = new PostCodeController(mockAuthAction, view, mcc, mockSessionCache, mockPostcodeForm)

  override def beforeEach(): Unit =
    super.beforeEach()

  override protected def afterEach(): Unit =
    super.afterEach()

  "Address Lookup Postcode Controller" should {
    when(mockPostcodeForm.postCodeCreateForm).thenReturn(new PostcodeForm().postCodeCreateForm)

    assertNotLoggedInAndCdsEnrolmentChecksForGetAnEori(mockAuthConnector, controller.createForm(atarService))

    "return 200 (OK)" when {

      "display successfully" in {

        withAuthorisedUser(defaultUserId, mockAuthConnector)

        showCreateForm(atarService) { result =>
          status(result) shouldBe OK
        }
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "form has incorrect values" in {
        withAuthorisedUser(defaultUserId, mockAuthConnector)
        when(
          mockSessionCache.savePostcodeAndLine1Details(
            ArgumentMatchers.eq(PostcodeViewModel("postcode", Some("addressLine1")))
          )(any())
        ).thenReturn(Future.successful(true))

        submitForm(invalidForm, atarService) { result =>
          status(result) shouldBe BAD_REQUEST
        }
      }
    }

    "return 303 (SEE_OTHER) and redirect to results page" when {
      "form is correct" in {
        withAuthorisedUser(defaultUserId, mockAuthConnector)
        when(mockSessionCache.savePostcodeAndLine1Details(any())(any())).thenReturn(Future.successful(true))

        submitForm(form, atarService) { result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result).get shouldBe "/customs-registration-services/atar/register/postcode/lookup"
        }
      }
    }
  }

  private def showCreateForm(service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.createForm(service).apply(SessionBuilder.buildRequestWithSession(defaultUserId)))
  }

  private def submitForm(form: Map[String, String], service: Service)(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(controller.submit(service).apply(SessionBuilder.buildRequestWithSessionAndFormValues(defaultUserId, form)))
  }

}
