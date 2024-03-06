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

package unit.services

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers.{ServiceUnavailable, VerifiedEmailResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  UpdateCustomsDataStoreConnector,
  UpdateVerifiedEmailConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.UpdateVerifiedEmailResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{MessagingServiceParam, ResponseCommon}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{RequestCommonGenerator, UpdateVerifiedEmailService}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateVerifiedEmailServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter {

  private val mockUpdateVerifiedEmailConnector                   = mock[UpdateVerifiedEmailConnector]
  private val mockUpdateCustomsDataStoreConnector                = mock[UpdateCustomsDataStoreConnector]
  private val mockRequestCommonGenerator: RequestCommonGenerator = mock[RequestCommonGenerator]

  private val service = new UpdateVerifiedEmailService(
    mockRequestCommonGenerator,
    mockUpdateVerifiedEmailConnector,
    mockUpdateCustomsDataStoreConnector
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    reset(mockUpdateVerifiedEmailConnector, mockUpdateCustomsDataStoreConnector)
  }

  val dateTime: LocalDateTime = LocalDateTime.now()
  private val eori            = "GBXXXXXXXXX0000"

  private val verifiedEmailResponse = VerifiedEmailResponse(
    UpdateVerifiedEmailResponse(
      ResponseCommon(
        "OK",
        None,
        LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), 0, ZoneOffset.UTC),
        Some(List(MessagingServiceParam("ETMPFORMBUNDLENUMBER", "XXXX")))
      )
    )
  )

  private val verifiedEmailResponseWithoutFormBudleId = VerifiedEmailResponse(
    UpdateVerifiedEmailResponse(
      ResponseCommon(
        "OK",
        None,
        LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), 0, ZoneOffset.UTC),
        None
      )
    )
  )

  "UpdateVerifiedEmailService" should {

    "return true when connector invokes updateVerifiedEmail method is invoked successfully" in {
      when(mockUpdateVerifiedEmailConnector.updateVerifiedEmail(any())(any()))
        .thenReturn(Future.successful(Right(verifiedEmailResponse)))
      when(mockUpdateCustomsDataStoreConnector.updateCustomsDataStore(any())(any()))
        .thenReturn(Future.successful {})
      await(service.updateVerifiedEmail("email-address", eori)) shouldBe true
      verify(mockUpdateVerifiedEmailConnector).updateVerifiedEmail(any())(any())
    }

    "return true when connector invokes updateVerifiedEmail method is invoked successfully without returnedParams" in {
      when(mockUpdateVerifiedEmailConnector.updateVerifiedEmail(any())(any()))
        .thenReturn(Future.successful(Right(verifiedEmailResponseWithoutFormBudleId)))
      when(mockUpdateCustomsDataStoreConnector.updateCustomsDataStore(any())(any()))
        .thenReturn(Future.successful {})
      await(service.updateVerifiedEmail("email-address", eori)) shouldBe false
    }

    "return false when connector returns failure response" in {
      when(mockUpdateVerifiedEmailConnector.updateVerifiedEmail(any())(any()))
        .thenReturn(Future.successful(Left(ServiceUnavailable)))
      when(mockUpdateCustomsDataStoreConnector.updateCustomsDataStore(any())(any()))
        .thenReturn(Future.successful {})
      await(service.updateVerifiedEmail("email-address", eori)) shouldBe false
      verify(mockUpdateVerifiedEmailConnector).updateVerifiedEmail(any())(any())
    }
  }
}
