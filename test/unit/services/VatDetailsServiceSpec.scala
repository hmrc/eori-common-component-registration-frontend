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

package unit.services

import cats.data.EitherT
import integration.IntegrationTestsSpec
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{GetVatCustomerInformationConnector, ResponseError, VatControlListConnector}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{VatControlListRequest, VatControlListResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.VatDetailsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatDetailsServiceSpec extends IntegrationTestsSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val mockAppConfig: AppConfig = mock[AppConfig]
  private val mockVatListConnector: VatControlListConnector = mock[VatControlListConnector]

  private val mockGetVatCustomerInfoConnector: GetVatCustomerInformationConnector =
    mock[GetVatCustomerInformationConnector]

  private val vatControlListResponse: EitherT[Future, ResponseError, VatControlListResponse] =
    EitherT.fromEither[Future](Right(VatControlListResponse(Some("SE28 1AA"), Some("2021-01-31"))))

  val vatService =
    new VatDetailsService(mockAppConfig, mockGetVatCustomerInfoConnector, mockVatListConnector)

  override protected def beforeEach(): Unit = {
    reset(mockAppConfig)
    reset(mockVatListConnector)
    reset(mockGetVatCustomerInfoConnector)
  }

  "VatDetailsServiceSpec" should {
    "get vat details from mockGetVatCustomerInfoConnector when feature flag is true" in {
      when(mockAppConfig.vatDetailsFeatureFlag).thenReturn(true)
      when(
        mockGetVatCustomerInfoConnector.getVatCustomerInformation(any[String])(any[HeaderCarrier])
      ) thenReturn vatControlListResponse

      await(vatService.getVatCustomerInformation("123 456 789").value)
      verify(mockGetVatCustomerInfoConnector, times(1)).getVatCustomerInformation(meq("123456789"))(any())
      verify(mockVatListConnector, never()).vatControlList(any())(any())

    }

    "get vat details from VatControlListConnector when feature flag is false" in {
      when(mockAppConfig.vatDetailsFeatureFlag).thenReturn(false)
      when(
        mockVatListConnector.vatControlList(any[VatControlListRequest])(any[HeaderCarrier])
      ) thenReturn vatControlListResponse

      await(vatService.getVatCustomerInformation("number").value)
      verify(mockVatListConnector, times(1)).vatControlList(any())(any())
      verify(mockGetVatCustomerInfoConnector, never()).getVatCustomerInformation(any())(any())
    }
  }

}
