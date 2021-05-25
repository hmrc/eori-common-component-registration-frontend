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

package unit.views.registration

import common.pages.registration.UserLocationPageOrganisation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.UserLocationController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationDisplayService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  EnrolmentStoreProxyService,
  SubscriptionStatusService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.user_location
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{
  sub01_outcome_processing,
  sub01_outcome_rejected
}
import unit.controllers.CdsPage
import util.ControllerSpec
import util.builders.AuthBuilder.withAuthorisedUser
import util.builders.{AuthActionMock, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserLocationFormViewSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector              = mock[AuthConnector]
  private val mockAuthAction                 = authAction(mockAuthConnector)
  private val mockRequestSessionData         = mock[RequestSessionData]
  private val mockSessionCache               = mock[SessionCache]
  private val mockSave4LaterService          = mock[Save4LaterService]
  private val mockSubscriptionStatusService  = mock[SubscriptionStatusService]
  private val mockRegistrationDisplayService = mock[RegistrationDisplayService]
  private val mockSave4LaterConnector        = mock[Save4LaterConnector]
  private val mockEnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val userLocationView = instanceOf[user_location]

  private val sub01OutcomeProcessing = instanceOf[sub01_outcome_processing]

  private val errorTemplate = instanceOf[error_template]

  private val sub01OutcomeRejected = instanceOf[sub01_outcome_rejected]

  private val controller = new UserLocationController(
    mockAuthAction,
    mockRequestSessionData,
    mockSave4LaterService,
    mockSubscriptionStatusService,
    mockSessionCache,
    mockRegistrationDisplayService,
    mcc,
    userLocationView,
    sub01OutcomeProcessing,
    sub01OutcomeRejected,
    errorTemplate
  )

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    when(mockSave4LaterConnector.get(any(), any())(any(), any()))
      .thenReturn(Future.successful(None))
    when(
      mockEnrolmentStoreProxyService
        .enrolmentForGroup(any(), any())(any())
    ).thenReturn(Future.successful(None))
  }

  "User location page" should {

    val expectedTitleOrganisation = "Where is your organisation established?"

    s"display title as '$expectedTitleOrganisation for entity with type organisation" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith(expectedTitleOrganisation)
      }
    }

    val expectedTitleIndividual = "Where are you based?"

    s"display title as '$expectedTitleIndividual for entity with type individual" in {
      showForm(affinityGroup = AffinityGroup.Individual) { result =>
        val page = CdsPage(contentAsString(result))
        page.title() should startWith(expectedTitleIndividual)
      }
    }

    "submit result when user chooses to continue" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page
          .formAction(
            "user-location-form"
          ) shouldBe uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.UserLocationController
          .submit(atarService, Journey.Register)
          .url
      }
    }

    "display correct location on registration journey" in {
      showForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.elementIsPresent(UserLocationPageOrganisation.locationUkField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationUkField) should be("uk")
        page.elementIsPresent(UserLocationPageOrganisation.locationIomField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationIomField) should be("iom")
        page.elementIsPresent(UserLocationPageOrganisation.locationIslandsField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationIslandsField) should be("islands")
        page.elementIsPresent(UserLocationPageOrganisation.locationEuField) should be(false)
        page.elementIsPresent(UserLocationPageOrganisation.locationThirdCountryField) should be(false)
        page.elementIsPresent(UserLocationPageOrganisation.locationThirdCountryIncEuField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationThirdCountryIncEuField) should be(
          "third-country-inc-eu"
        )
      }
    }

    "display correct location on migration journey" in {
      showMigrationForm() { result =>
        val page = CdsPage(contentAsString(result))
        page.elementIsPresent(UserLocationPageOrganisation.locationUkField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationUkField) should be("uk")
        page.elementIsPresent(UserLocationPageOrganisation.locationIslandsField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationIslandsField) should be("islands")
        page.elementIsPresent(UserLocationPageOrganisation.locationEuField) should be(false)
        page.elementIsPresent(UserLocationPageOrganisation.locationThirdCountryField) should be(false)
        page.elementIsPresent(UserLocationPageOrganisation.locationThirdCountryIncEuField) should be(true)
        page.getElementValue(UserLocationPageOrganisation.locationThirdCountryIncEuField) should be(
          "third-country-inc-eu"
        )
      }
    }
  }

  private def showForm(userId: String = defaultUserId, affinityGroup: AffinityGroup = AffinityGroup.Organisation)(
    test: Future[Result] => Any
  ) {
    withAuthorisedUser(userId, mockAuthConnector, userAffinityGroup = affinityGroup)

    val result = controller
      .form(atarService, Journey.Register)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  private def showMigrationForm(
    userId: String = defaultUserId,
    affinityGroup: AffinityGroup = AffinityGroup.Organisation
  )(test: Future[Result] => Any) {
    withAuthorisedUser(userId, mockAuthConnector, userAffinityGroup = affinityGroup)

    val result = controller
      .form(atarService, Journey.Subscribe)
      .apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
