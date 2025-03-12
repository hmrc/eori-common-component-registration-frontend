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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.AddressInvalidController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{address_invalid_individual, address_invalid_organisation}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AddressInvalidControllerSpec extends ControllerSpec with AuthActionMock {

  private val mockAuthConnector       = mock[AuthConnector]
  private val mockAuthAction          = authAction(mockAuthConnector)
  private val mockRequestSessionData  = mock[RequestSessionData]
  private val mockSessionCacheService = inject[SessionCacheService]

  private val addressInvalidOrgView = inject[address_invalid_organisation]
  private val addressInvalidIndView = inject[address_invalid_individual]

  val controller = new AddressInvalidController(
    mockAuthAction,
    mockSessionCacheService,
    mockRequestSessionData,
    addressInvalidOrgView,
    addressInvalidIndView,
    mcc
  )(global)

  "Address Invalid page" should {
    "display contact HMRC page for individual" in {
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(true)
      createForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Contact HMRC")
      }
    }
    "display contact Companies House page for organisation" in {
      when(mockRequestSessionData.isIndividualOrSoleTrader(any())).thenReturn(false)
      createForm() { result =>
        status(result) shouldBe OK
        val page = CdsPage(contentAsString(result))
        page.title() should startWith("Contact Companies House")
      }
    }
  }

  private def createForm()(test: Future[Result] => Any) = {
    withAuthorisedUser(defaultUserId, mockAuthConnector)
    test(
      controller
        .page(atarService)
        .apply(SessionBuilder.buildRequestWithSession(defaultUserId))
    )
  }

}
