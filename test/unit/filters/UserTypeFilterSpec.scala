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

package unit.filters

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Request
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.DownloadTextController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.Sub02Outcome
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{eori_number_text_download, subscription_text_download}
import util.ControllerSpec
import util.builders.{AuthActionMock, AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UserTypeFilterSpec extends ControllerSpec with BeforeAndAfterEach with AuthActionMock {

  private val mockAuthConnector            = mock[AuthConnector]
  private val mockAuthAction               = authAction(mockAuthConnector)
  private val mockCache                    = mock[SessionCache]
  private val eoriNumberTextDownloadView   = instanceOf[eori_number_text_download]
  private val subscriptionTextDownloadView = instanceOf[subscription_text_download]

  private val controller =
    new DownloadTextController(mockAuthAction, mockCache, eoriNumberTextDownloadView, subscriptionTextDownloadView, mcc)

  override def beforeEach(): Unit =
    when(mockCache.sub02Outcome(any[Request[_]]))
      .thenReturn(Future.successful(Sub02Outcome("20/01/2019", "John Doe", Some("GB123456789012"))))

  "User Type Filter verification" should {

    "return Redirect (303) when a agent attempts to access a route" in {
      AuthBuilder.withAuthorisedUser("user-1236213", mockAuthConnector, userAffinityGroup = AffinityGroup.Agent)

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/subscribe/", defaultUserId)
        )

      status(result) shouldBe SEE_OTHER
    }

    "return Redirect (303) when an organisation with an assistant account attempts to access a route" in {
      AuthBuilder.withAuthorisedUser(
        "user-1236213",
        mockAuthConnector,
        userAffinityGroup = AffinityGroup.Organisation,
        userCredentialRole = Some(Assistant)
      )

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/subscribe/", defaultUserId)
        )

      status(result) shouldBe SEE_OTHER
    }

    "return Redirect (303) when an organisation with an assistant account attempts to access a route the URL should contain you cannot use this service" in {
      AuthBuilder.withAuthorisedUser(
        "user-1236213",
        mockAuthConnector,
        userAffinityGroup = AffinityGroup.Organisation,
        userCredentialRole = Some(Assistant)
      )

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/subscribe/", defaultUserId)
        )

      await(result).header.headers(LOCATION) should endWith("you-cannot-use-service")
    }

    "return Redirect (303) when an organisation with an assistant account attempts to access a route with existing enrollment" in {
      val atarEnrolment = Enrolment("HMRC-ATAR-ORG").withIdentifier("EORINumber", "GB134123")

      AuthBuilder.withAuthorisedUser(
        "user-1236213",
        mockAuthConnector,
        userAffinityGroup = AffinityGroup.Organisation,
        userCredentialRole = Some(Assistant),
        otherEnrolments = Set(atarEnrolment)
      )

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/atar/subscribe/", defaultUserId)
        )

      await(result).header.headers(LOCATION) should endWith("enrolment-already-exists")
    }

    "return Redirect (303) when an organisation with an assistant account attempts to access a route without existing enrollment" in {
      AuthBuilder.withAuthorisedUser(
        "user-1236213",
        mockAuthConnector,
        userAffinityGroup = AffinityGroup.Organisation,
        userCredentialRole = Some(Assistant),
        otherEnrolments = Set.empty[Enrolment]
      )

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/atar/subscribe/", defaultUserId)
        )

      await(result).header.headers(LOCATION) should endWith("you-cannot-use-service")
    }

    "return OK (200) when an organisation with a standard account attempts to access a route" in {
      AuthBuilder.withAuthorisedUser(
        "user-1236213",
        mockAuthConnector,
        userAffinityGroup = AffinityGroup.Organisation,
        userCredentialRole = Some(User)
      )

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/subscribe/", defaultUserId)
        )

      status(result) shouldBe OK
    }

    "return OK (200) when an organisation with an User account attempts to access a route" in {
      AuthBuilder.withAuthorisedUser(
        "user-1236213",
        mockAuthConnector,
        userAffinityGroup = AffinityGroup.Organisation,
        userCredentialRole = Some(User)
      )

      val result = controller
        .download(atarService)
        .apply(
          SessionBuilder.buildRequestWithSessionAndPath("/customs-registration-services/subscribe/", defaultUserId)
        )

      status(result) shouldBe OK
    }
  }
}
