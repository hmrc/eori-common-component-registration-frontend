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

package unit.controllers

import common.pages.RegistrationCompletePage
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.EnrolmentAlreadyExistsController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{registration_exists, registration_exists_group}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

class EnrolmentAlreadyExistsControllerSpec extends ControllerSpec with AuthActionMock {

  private val registrationExistsView      = instanceOf[registration_exists]
  private val registrationExistsGroupView = instanceOf[registration_exists_group]
  private val mockAuthConnector           = mock[AuthConnector]
  private val mockAuthAction              = authAction(mockAuthConnector)

  val controller =
    new EnrolmentAlreadyExistsController(mockAuthAction, registrationExistsView, registrationExistsGroupView, mcc)

  val paragraphXpath = "//*[@id='para1']"

  "Enrolment already exists controller" should {

    "redirect to the enrolment already exists page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(
          controller.enrolmentAlreadyExists(atarService, Journey.Subscribe).apply(
            SessionBuilder.buildRequestWithSessionAndPath("/atar/", defaultUserId)
          )
        )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title should startWith("There is a problem")
      page.getElementsText(RegistrationCompletePage.pageHeadingXpath) shouldBe "There is a problem"
      page.getElementsText(paragraphXpath) should include(
        "Our records show that this Government Gateway user ID has already been used to subscribe to Advance Tariff Rulings"
      )

    }

    "redirect to the enrolment already exists for group page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(
          controller.enrolmentAlreadyExistsForGroup(atarService, Journey.Subscribe).apply(
            SessionBuilder.buildRequestWithSessionAndPath("/atar/", defaultUserId)
          )
        )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title should startWith("There is a problem")
      page.getElementsText(RegistrationCompletePage.pageHeadingXpath) shouldBe "There is a problem"
      page.getElementsText(paragraphXpath) should include(
        "Your organisation is already subscribed to Advance Tariff Rulings"
      )

    }
  }
}
