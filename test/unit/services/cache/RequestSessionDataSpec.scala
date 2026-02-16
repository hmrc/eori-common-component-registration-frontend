/*
 * Copyright 2026 HM Revenue & Customs
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

package unit.services.cache

import base.UnitSpec
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Session}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.OrganisationSubscriptionFlow
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError.DataNotFound
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.http.HeaderCarrier

class RequestSessionDataSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  implicit private val hc: HeaderCarrier = HeaderCarrier()
  private val mockAudit = mock[Auditor]
  private val requestSessionData = new RequestSessionData(mockAudit)
  implicit private val mockRequest: Request[AnyContent] = mock[Request[AnyContent]]
  private val existingSessionValues = Map("someExistingValue" -> "value")
  private val existingSession: Session = Session(existingSessionValues)
  private val mockOrganisationType = mock[CdsOrganisationType]
  private val testOrganisationTypeId = "arbitrary_organisation_type"

  private val existingSessionBeforeStartAgain = Map(
    "selected-organisation-type"   -> "Org Type",
    "subscription-flow"            -> "sub-flow",
    "uri-before-subscription-flow" -> "uri-before-sub-flow"
  )

  override def beforeEach(): Unit = {
    when(mockRequest.session).thenReturn(existingSession)
    when(mockOrganisationType.id).thenReturn(testOrganisationTypeId)
  }

  "RequestSessionData" should {
    "add correct flow name in request cache" in {
      val newSession = requestSessionData.storeUserSubscriptionFlow(OrganisationSubscriptionFlow, "")
      newSession shouldBe Session(
        existingSessionValues ++ Map(
          "subscription-flow"            -> OrganisationSubscriptionFlow.name,
          "uri-before-subscription-flow" -> ""
        )
      )
    }

    "return correct flow cached" in {
      when(mockRequest.session).thenReturn(Session(Map("subscription-flow" -> OrganisationSubscriptionFlow.name)))
      requestSessionData.userSubscriptionFlow.map(flow => flow shouldBe OrganisationSubscriptionFlow)
    }

    "throw exception when flow is not cached" in {
      when(mockRequest.session).thenReturn(Session())
      val result = requestSessionData.userSubscriptionFlow
      result shouldBe a[Left[DataNotFound, _]]

    }

    "add organisation type to request cache" in {
      val newSession = requestSessionData.sessionWithOrganisationTypeAdded(mockOrganisationType)
      newSession shouldBe Session(existingSessionValues + ("selected-organisation-type" -> testOrganisationTypeId))
    }

    "add organisation type to session" in {
      val newSession = requestSessionData.sessionWithOrganisationTypeAdded(existingSession, mockOrganisationType)
      newSession shouldBe Session(existingSessionValues + ("selected-organisation-type" -> testOrganisationTypeId))
    }

    "return session with organisation type removed when organisation type was present in session" in {
      when(mockRequest.session).thenReturn(Session(Map("selected-organisation-type" -> testOrganisationTypeId)))
      val newSession = requestSessionData.sessionWithoutOrganisationType
      newSession.data should not contain ("selected-organisation-type" -> testOrganisationTypeId)
    }

    "return session with organisation type missing when organisation type was not already present in session" in {
      when(mockRequest.session).thenReturn(Session())
      val newSession = requestSessionData.sessionWithoutOrganisationType
      newSession.data should not contain ("selected-organisation-type" -> testOrganisationTypeId)
    }
    "return session third country" in {
      when(mockRequest.session).thenReturn(Session(Map("selected-user-location" -> "isle-of-man")))
      requestSessionData.selectedUserLocationWithIslands shouldBe Some(UserLocation.Iom)
    }
    "return some third country for islands" in {
      when(mockRequest.session).thenReturn(Session(Map("selected-user-location" -> "islands")))
      requestSessionData.selectedUserLocation shouldBe Some(UserLocation.ThirdCountry)
    }
    "return some third country for eu" in {
      when(mockRequest.session).thenReturn(Session(Map("selected-user-location" -> "eu")))
      requestSessionData.selectedUserLocation shouldBe Some(UserLocation.ThirdCountry)
    }
    "add user location to session" in {
      val newSession = requestSessionData.sessionWithUserLocationAdded("eu")
      newSession shouldBe Session(existingSessionValues + ("selected-user-location" -> "eu"))
    }
    "add user location to existing session" in {
      val newSession = requestSessionData.existingSessionWithUserLocationAdded(existingSession, "eu")
      newSession shouldBe Session(existingSessionValues + ("selected-user-location" -> "eu"))
    }
    "return session without organisation-type, subscription-flow and uri-before-sub-flow" in {
      when(mockRequest.session).thenReturn(Session(existingSessionBeforeStartAgain))
      requestSessionData.sessionForStartAgain.data should contain.noneOf(
        "selected-organisation-type",
        "subscription-flow",
        "uri-before-subscription-flow"
      )

    }
  }
}
