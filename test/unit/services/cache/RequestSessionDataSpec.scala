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

package unit.services.cache

import base.UnitSpec
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request, Session}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  IndividualFlow,
  OrganisationFlow,
  OrganisationSubscriptionFlow,
  SoleTraderFlow,
  ThirdCountryIndividualSubscriptionFlow
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData

class RequestSessionDataSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val requestSessionData       = new RequestSessionData()
  private implicit val mockRequest     = mock[Request[AnyContent]]
  private val existingSessionValues    = Map("someExistingValue" -> "value")
  private val existingSession: Session = Session(existingSessionValues)
  private val mockOrganisationType     = mock[CdsOrganisationType]
  private val testOrganisationTypeId   = "arbitrary_organisation_type"

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
        existingSessionValues + ("subscription-flow" -> OrganisationSubscriptionFlow.name, "uri-before-subscription-flow" -> "")
      )
    }

    "return correct flow cached" in {
      when(mockRequest.session).thenReturn(Session(Map("subscription-flow" -> OrganisationSubscriptionFlow.name)))
      requestSessionData.userSubscriptionFlow shouldBe OrganisationSubscriptionFlow
    }

    "throw exception when flow is not cached" in {
      when(mockRequest.session).thenReturn(Session())
      val caught = intercept[IllegalStateException](requestSessionData.userSubscriptionFlow)
      caught.getMessage shouldBe "Subscription flow is not cached"
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

    "return session with unmatched user missing when unmatchedUser was not already present in session" in {
      when(mockRequest.session).thenReturn(Session())
      val newSession = requestSessionData.sessionWithUnMatchedUser(false)
      newSession.data should contain("unmatched-user" -> "false")

      requestSessionData.mayBeUnMatchedUser shouldBe None
    }
    "return session with unmatched user missing when unmatchedUser type was set  in session" in {
      when(mockRequest.session).thenReturn(Session())
      val newSession = requestSessionData.sessionWithUnMatchedUser(true)
      newSession.data should contain("unmatched-user" -> "true")
    }
    "return session third country" in {
      when(mockRequest.session).thenReturn(Session(Map("selected-user-location" -> "iom")))
      requestSessionData.selectedUserLocationWithIslands shouldBe Some("iom")
    }
    "return session without organisation-type, subscription-flow and uri-before-sub-flow" in {
      when(mockRequest.session).thenReturn(Session(existingSessionBeforeStartAgain))
      requestSessionData.sessionForStartAgain.data should contain noneOf (
        "selected-organisation-type",
        "subscription-flow",
        "uri-before-subscription-flow"
      )
    }

    "return true for isUKJourney method" when {

      "user is during organisation UK subscription journey" in {

        when(mockRequest.session).thenReturn(Session(Map("subscription-flow" -> OrganisationFlow.name)))

        requestSessionData.isUKJourney shouldBe true
      }

      "user is during sole trader UK subscription journey" in {

        when(mockRequest.session).thenReturn(Session(Map("subscription-flow" -> SoleTraderFlow.name)))

        requestSessionData.isUKJourney shouldBe true
      }

      "user is during individual UK subscription journey" in {

        when(mockRequest.session).thenReturn(Session(Map("subscription-flow" -> IndividualFlow.name)))

        requestSessionData.isUKJourney shouldBe true
      }
    }

    "return false for isUKJourney method" when {

      "user is on different journey" in {

        when(mockRequest.session).thenReturn(
          Session(Map("subscription-flow" -> ThirdCountryIndividualSubscriptionFlow.name))
        )

        requestSessionData.isUKJourney shouldBe false
      }
    }
  }
}
