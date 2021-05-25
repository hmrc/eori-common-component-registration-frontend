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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, JsString}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.IsleOfManController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.VatRegisteredUkController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.VatGroupController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.isle_of_man
import util.ControllerSpec
import util.builders.AuthActionMock

class IsleOfManControllerSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val isleOfManView = mock[isle_of_man]

  private val controller = new IsleOfManController(isleOfManView, mcc)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(isleOfManView.apply(any(), any[Service])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(isleOfManView)

    super.afterEach()
  }

  "Isle Of Man Controller" should {

    "return 200 (OK)" when {

      "form method is invoked" in {

        val result = controller.form(atarService)(FakeRequest("GET", ""))

        status(result) shouldBe OK
      }
    }

    "return BAD_REQUEST (400)" when {

      "user doesn't answer on the question" in {

        val emptyForm = JsObject(Map("yes-no-answer" -> JsString("")))

        val result = controller.submit(atarService)(FakeRequest().withJsonBody(emptyForm))

        status(result) shouldBe BAD_REQUEST
      }
    }

    "return SEE_OTHER (303) and redirect to VAT Register UK page" when {

      "user answer Yes" in {

        val form = JsObject(Map("yes-no-answer" -> JsString("true")))

        val result = controller.submit(atarService)(FakeRequest().withJsonBody(form))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(VatRegisteredUkController.form(atarService).url)
      }
    }

    "return SEE_OTHER (303) and redirect to VAT Group page" when {

      "user answer No" in {

        val form = JsObject(Map("yes-no-answer" -> JsString("false")))

        val result = controller.submit(atarService)(FakeRequest().withJsonBody(form))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(VatGroupController.createForm(atarService, Journey.Register).url)
      }
    }
  }
}
