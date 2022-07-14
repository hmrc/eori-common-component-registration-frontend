/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.{Assistant, AuthConnector, Enrolment}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.EnrolmentAlreadyExistsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.Eori
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{
  enrolment_exists_user_standalone,
  registration_exists,
  registration_exists_group
}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

class EnrolmentAlreadyExistsControllerSpec extends ControllerSpec with AuthActionMock {

  private val registrationExistsView        = instanceOf[registration_exists]
  private val registrationExistsGroupView   = instanceOf[registration_exists_group]
  private val enrolmentExistsStandaloneView = instanceOf[enrolment_exists_user_standalone]
  private val mockAuthConnector             = mock[AuthConnector]
  private val mockAuthAction                = authAction(mockAuthConnector)

  val controller =
    new EnrolmentAlreadyExistsController(
      mockAuthAction,
      registrationExistsView,
      registrationExistsGroupView,
      enrolmentExistsStandaloneView,
      mcc
    )

  val paragraphXpath = "//*[@id='para1']"

  private val eori = Eori("GB123456789012")

  "Enrolment already exists controller" should {

    "redirect to the enrolment already exists page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(
          controller.enrolmentAlreadyExists(atarService).apply(
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

    "display enrolment already exists page for admin user on standalone journey" in {

      withAuthorisedUser(
        defaultUserId,
        mockAuthConnector,
        otherEnrolments = Set(Enrolment("HMRC-CUS-ORG").withIdentifier("EORINumber", eori.id))
      )

      val result =
        await(
          controller.enrolmentAlreadyExistsStandalone(eoriOnlyService).apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )
        )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title should startWith("Your business or organisation already has an EORI number")
      page.getElementsText(
        RegistrationCompletePage.pageHeadingXpath
      ) shouldBe "Your business or organisation already has an EORI number"

    }

    "display enrolment already exists page for standard user on standalone journey" in {

      withAuthorisedUser(
        defaultUserId,
        mockAuthConnector,
        userCredentialRole = Some(Assistant),
        otherEnrolments = Set(Enrolment("HMRC-CUS-ORG").withIdentifier("EORINumber", eori.id))
      )

      val result =
        await(
          controller.enrolmentAlreadyExistsStandalone(eoriOnlyService).apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )
        )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title should startWith("You already have an EORI number")
      page.getElementsText(RegistrationCompletePage.pageHeadingXpath) shouldBe "You already have an EORI number"

    }

    "throw exception if EORI Number is not available against any enrolment in standalone journey" in {
      withAuthorisedUser(
        defaultUserId,
        mockAuthConnector,
        userCredentialRole = Some(Assistant),
        otherEnrolments = Set(Enrolment("HMRC-CUS-ORG"))
      )
      intercept[IllegalStateException] {
        await(
          controller.enrolmentAlreadyExistsStandalone(eoriOnlyService).apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )
        )
      }
    }

    "redirect to the enrolment already exists for group page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        await(
          controller.enrolmentAlreadyExistsForGroup(atarService).apply(
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
