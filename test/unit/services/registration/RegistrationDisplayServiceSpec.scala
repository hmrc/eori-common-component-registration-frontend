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

package unit.services.registration

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{RegistrationDisplayConnector, ServiceUnavailableResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.ResponseDetail
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.{
  RegistrationDisplayResponse,
  ResponseCommon
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation,
  SafeId
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.RegistrationDisplayService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RegistrationDisplayServiceSpec extends UnitSpec with MockitoSugar {
  val mockCache     = mock[SessionCache]
  val mockConnector = mock[RegistrationDisplayConnector]
  val mockCreator   = mock[RegistrationDetailsCreator]
  val testService   = new RegistrationDisplayService(mockCache, mockConnector, mockCreator)

  val mockResponseCommon  = mock[ResponseCommon]
  val mockResponseDetail  = mock[ResponseDetail]
  val mockDisplayResponse = mock[RegistrationDisplayResponse]

  "RegistrationDisplayService" should {
    "successfully request details from registration display service" in {
      when(mockConnector.registrationDisplay(any())(any(), any()))
        .thenReturn(Future.successful(Right(mockDisplayResponse)))
      await(testService.requestDetails(SafeId("SAFEID"))(HeaderCarrier(), ExecutionContext.global)) shouldBe Right(
        mockDisplayResponse
      )
    }

    "return failure response from registration display service" in {
      when(mockConnector.registrationDisplay(any())(any(), any()))
        .thenReturn(Future.successful(Left(ServiceUnavailableResponse)))
      await(testService.requestDetails(SafeId("SAFEID"))(HeaderCarrier(), ExecutionContext.global)) shouldBe Left(
        ServiceUnavailableResponse
      )
    }

    "successfully cache details from registration display service" in {
      when(mockCreator.registrationDetails(any[RegistrationDisplayResponse]))
        .thenReturn(RegistrationDetailsIndividual())
      when(mockCache.saveRegistrationDetails(any[RegistrationDetailsIndividual])(any()))
        .thenReturn(Future.successful(true))
      await(testService.cacheDetails(mockDisplayResponse)(HeaderCarrier())) shouldBe true
    }

    "return false when unable to cache details" in {
      when(mockCreator.registrationDetails(any[RegistrationDisplayResponse]))
        .thenReturn(RegistrationDetailsOrganisation())
      when(mockCache.saveRegistrationDetails(any[RegistrationDetailsOrganisation])(any()))
        .thenReturn(Future.successful(false))
      await(testService.cacheDetails(mockDisplayResponse)(HeaderCarrier())) shouldBe false
    }
  }
}
