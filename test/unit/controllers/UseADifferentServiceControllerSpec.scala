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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.UseADifferentServiceController
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.use_a_different_service
import util.ControllerSpec

class UseADifferentServiceControllerSpec extends ControllerSpec with BeforeAndAfterEach {

  private val useADifferentService = inject[use_a_different_service]

  "useADifferentService" should {

    "return OK" in {
      val controller = new UseADifferentServiceController(useADifferentService, mcc)
      val result     = controller.form(atarService)(FakeRequest())
      status(result) shouldBe OK
    }
  }
}
