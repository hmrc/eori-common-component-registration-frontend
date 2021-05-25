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

package unit.controllers.auth

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.GroupEnrolmentExtractor
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.EnrolmentResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.EnrolmentStoreProxyService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class GroupEnrolmentExtractorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  private val enrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  private val enrolmentResponse = EnrolmentResponse("HMRC-CUS-ORG", "ACTIVATED", List.empty)

  private val hc = HeaderCarrier()

  private val groupEnrolmentExtractor = new GroupEnrolmentExtractor(enrolmentStoreProxyService)(global)

  override protected def afterEach(): Unit = {
    reset(enrolmentStoreProxyService)

    super.afterEach()
  }

  "GroupEnrolmentExtractor" should {

    "return false for hasGroupIdEnrolmentTo" when {

      "group Id doesn't have enrolment to specific service" in {

        when(enrolmentStoreProxyService.enrolmentForGroup(any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = groupEnrolmentExtractor.hasGroupIdEnrolmentTo("groupId", Service.cds)(hc)

        result.futureValue shouldBe false
      }
    }

    "return true for hasGroupIdEnrolmentTo" when {

      "group Id has enrolment to specific service" in {

        when(enrolmentStoreProxyService.enrolmentForGroup(any(), any())(any()))
          .thenReturn(Future.successful(Some(enrolmentResponse)))

        val result = groupEnrolmentExtractor.hasGroupIdEnrolmentTo("groupId", Service.cds)(hc)

        result.futureValue shouldBe true
      }
    }

    "return enrolmentResponse" when {

      "groupId has enrolment to specfic service" in {

        when(enrolmentStoreProxyService.enrolmentForGroup(any(), any())(any()))
          .thenReturn(Future.successful(Some(enrolmentResponse)))

        val result = groupEnrolmentExtractor.groupIdEnrolmentTo("groupId", Service.cds)(hc)

        result.futureValue shouldBe Some(enrolmentResponse)
      }
    }

    "return None" when {

      "groupId doesn't have enrolment to specific service" in {

        when(enrolmentStoreProxyService.enrolmentForGroup(any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = groupEnrolmentExtractor.groupIdEnrolmentTo("groupId", Service.cds)(hc)

        result.futureValue shouldBe None
      }
    }

    "return all group enrolments" when {

      "groupId has enrolments" in {

        when(enrolmentStoreProxyService.enrolmentsForGroup(any())(any()))
          .thenReturn(Future.successful(List(enrolmentResponse)))

        val result = groupEnrolmentExtractor.groupIdEnrolments("groupId")(hc)

        result.futureValue shouldBe List(enrolmentResponse)
      }
    }
  }
}
