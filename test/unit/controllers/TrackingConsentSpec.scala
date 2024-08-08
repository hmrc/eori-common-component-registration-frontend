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
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.GetUtrNumberController
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{MatchingService, SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, how_can_we_identify_you_utr}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrackingConsentSpec extends ControllerSpec with MockitoSugar with AuthActionMock {

  private val mockAuthConnector                    = mock[AuthConnector]
  private val mockAuthAction                       = authAction(mockAuthConnector)
  private val mockMatchingService                  = mock[MatchingService]
  private val mockSubscriptionDetailsHolderService = mock[SubscriptionDetailsService]
  private val howCanWeIdentifyYouView              = instanceOf[how_can_we_identify_you_utr]
  private val errorTemplateView                    = instanceOf[error_template]

  private val controller = new GetUtrNumberController(
    mockAuthAction,
    mockMatchingService,
    mcc,
    howCanWeIdentifyYouView,
    mockSubscriptionDetailsHolderService,
    errorTemplateView
  )

  "Tracking Consent Snippet" should {
    "include the javascript file in the header" in {
      showForm(Map.empty) { result =>
        val page = CdsPage(contentAsString(result))
        page.getElementAttribute("//head/script[1]", "src") should endWith("/tracking-consent/tracking.js")
      }
    }
  }

  def showForm(form: Map[String, String], userId: String = defaultUserId)(test: Future[Result] => Any): Unit = {
    withAuthorisedUser(userId, mockAuthConnector)
    test(controller.form("individual", atarService).apply(SessionBuilder.buildRequestWithSessionAndFormValues(userId, form)))
  }

}
