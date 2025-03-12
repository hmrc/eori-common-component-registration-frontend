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

import common.pages.RegistrationCompletePage
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Request
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{Assistant, AuthConnector, Enrolment}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.EnrolmentAlreadyExistsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.Eori
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{enrolment_exists_group_standalone, enrolment_exists_user_standalone, registration_exists, registration_exists_group}
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class EnrolmentAlreadyExistsControllerSpec extends ControllerSpec with AuthActionMock {

  private val registrationExistsView = inject[registration_exists]
  private val registrationExistsGroupView = inject[registration_exists_group]
  private val enrolmentExistsStandaloneView = inject[enrolment_exists_user_standalone]
  private val enrolmentExistsGroupStandaloneView = inject[enrolment_exists_group_standalone]
  private val mockAuthConnector = mock[AuthConnector]
  private val mockSessionCache = mock[SessionCache]
  private val mockAppConfig = mock[AppConfig]
  private val mockAuthAction = authAction(mockAuthConnector)

  val controller =
    new EnrolmentAlreadyExistsController(
      mockAuthAction,
      mockSessionCache,
      mockAppConfig,
      registrationExistsView,
      registrationExistsGroupView,
      enrolmentExistsStandaloneView,
      enrolmentExistsGroupStandaloneView,
      mcc
    )(global)

  val paragraphXpath = "//*[@id='para1']"

  private val eori = Eori("GB123456789012")

  "Enrolment already exists controller" should {

    "redirect to the enrolment already exists page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        controller
          .enrolmentAlreadyExists(atarService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndPath("/atar/", defaultUserId)
          )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title() should startWith(messages("cds.enrolment.already.exists.heading"))
      page.getElementsText(RegistrationCompletePage.pageHeadingXpath) shouldBe messages(
        "cds.enrolment.already.exists.heading"
      )
      page.getElementsText(paragraphXpath) should include(messages("cds.enrolment.already.exists.para1"))

    }

    "redirect to enrolment already exists standalone if its standalone journey" in {
      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockAppConfig.standaloneServiceCode).thenReturn("eori-only")
      val result =
        controller
          .enrolmentAlreadyExists(eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )
      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") should endWith("/eori-only/register/cds-enrolment-exists")

    }

    "display enrolment already exists page for admin user on standalone journey" in {

      withAuthorisedUser(
        defaultUserId,
        mockAuthConnector,
        otherEnrolments = Set(Enrolment("HMRC-CUS-ORG").withIdentifier("EORINumber", eori.id))
      )

      val result =
        controller
          .enrolmentAlreadyExistsStandalone(eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title() should startWith("Your business or organisation already has an EORI number")
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
        controller
          .enrolmentAlreadyExistsStandalone(eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title() should startWith("You already have an EORI number")
      page.getElementsText(RegistrationCompletePage.pageHeadingXpath) shouldBe "You already have an EORI number"

    }

    "redirect to the enrolment already exists for group page" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)

      val result =
        controller
          .enrolmentAlreadyExistsForGroup(atarService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndPath("/atar/", defaultUserId)
          )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title() should startWith(messages("cds.enrolment.already.exists.heading"))
      page.getElementsText(RegistrationCompletePage.pageHeadingXpath) shouldBe messages(
        "cds.enrolment.already.exists.heading"
      )
      page.getElementsText(paragraphXpath) should include(
        "Your organisation is already subscribed to Advance Tariff Rulings"
      )

    }

    "redirect to the enrolment already exists for group page for standalone journey" in {

      withAuthorisedUser(defaultUserId, mockAuthConnector)
      when(mockSessionCache.eori(any[Request[_]]))
        .thenReturn(Future.successful(Some("testEori")))
      val result =
        controller
          .enrolmentAlreadyExistsForGroupStandalone(eoriOnlyService)
          .apply(
            SessionBuilder.buildRequestWithSessionAndPath("/eori-only/", defaultUserId)
          )

      status(result) shouldBe OK

      val page = CdsPage(contentAsString(result))

      page.title() should startWith("Your business or organisation already has an EORI number")
      page.getElementsText(
        RegistrationCompletePage.pageHeadingXpath
      ) shouldBe "Your business or organisation already has an EORI number"

    }
  }
}
