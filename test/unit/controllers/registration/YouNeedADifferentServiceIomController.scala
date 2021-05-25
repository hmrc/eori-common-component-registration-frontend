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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.YouNeedADifferentServiceIomController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.you_need_different_service_iom
import util.ControllerSpec

class YouNeedADifferentServiceIomControllerSpec extends ControllerSpec with BeforeAndAfterEach {

  private val youNeedDifferenceServiceIomPage = mock[you_need_different_service_iom]

  private val controller = new YouNeedADifferentServiceIomController(youNeedDifferenceServiceIomPage, mcc)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(youNeedDifferenceServiceIomPage.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit =
    reset(youNeedDifferenceServiceIomPage)

  "YouNeedADifferentServiceIomController on method form" should {

    "return youNeedDifferentServiceIom page" in {

      val result = controller.form(atarService, Journey.Subscribe)(FakeRequest())

      status(result) shouldBe OK
    }
  }
}
