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
import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.NOT_FOUND
import play.api.test.FakeRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{GetVatCustomerInformationConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GetVatInformationResponse, VatControlListResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.services.GetVatCustomerInformationService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GetVatCustomerInformationServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  private val getVatCustomerInformationConnectorMock = mock[GetVatCustomerInformationConnector]
  private val sessionCacheMock                       = mock[SessionCache]

  private val service              = new GetVatCustomerInformationService(getVatCustomerInformationConnectorMock, sessionCacheMock)
  private implicit val hc          = HeaderCarrier()
  private implicit val fakeRequest = FakeRequest()

  "checkResponseMatchesNewVATAPI" should {
    val dateAsString = "2021-11-21"
    val postCode     = "SE28 1AA"
    val vatDetails   = VatDetails(postCode, "123456789")

    "handle Successful getVatCustomerInformation API Response" in {

      val format = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val date   = format.parse(dateAsString)

      when(sessionCacheMock.subscriptionDetails).thenReturn(SubscriptionDetails(ukVatDetails = Some(vatDetails)))

      val connectorResponse: Either[Int, GetVatInformationResponse] =
        Right(GetVatInformationResponse(Some(date), Some(postCode)))
      mockGetVatCustomerInformation()(
        EitherT[Future, Int, GetVatInformationResponse](Future.successful(connectorResponse))
      )

      val vatControlListResponse: VatControlListResponse =
        VatControlListResponse(dateOfReg = Some(dateAsString), postcode = Some(postCode))

      service.checkResponseMatchesNewVATAPI(vatControlListResponse)
    }

    "handle Unsuccessful getVatCustomerInformation API Response" in {

      when(sessionCacheMock.subscriptionDetails).thenReturn(SubscriptionDetails(ukVatDetails = Some(vatDetails)))

      val connectorResponse: Either[Int, GetVatInformationResponse] = Left(NOT_FOUND)
      mockGetVatCustomerInformation()(
        EitherT[Future, Int, GetVatInformationResponse](Future.successful(connectorResponse))
      )

      val vatControlListResponse: VatControlListResponse =
        VatControlListResponse(dateOfReg = Some(dateAsString), postcode = Some(postCode))

      service.checkResponseMatchesNewVATAPI(vatControlListResponse)
    }
  }

  "compareApiResponses" should {
    "handle matched API responses" in {
      val dateAsString = "2021-11-21"
      val postCode     = "SE28 1AA"
      val format       = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val date         = format.parse(dateAsString)

      val vatControlListResponse: VatControlListResponse =
        VatControlListResponse(dateOfReg = Some(dateAsString), postcode = Some(postCode))
      val getVatInformationResponse: GetVatInformationResponse =
        GetVatInformationResponse(effectiveRegistrationDate = Some(date), postCode = Some(postCode))

      val result = service.compareApiResponses(vatControlListResponse, getVatInformationResponse)
      result shouldBe true
    }

    "handle non matched API responses" in {
      val dateAsString = "2021-11-21"
      val postCode     = "SE28 1AA"
      val format       = new java.text.SimpleDateFormat("yyyy-MM-dd")
      val date         = format.parse(dateAsString)

      val vatControlListResponse: VatControlListResponse =
        VatControlListResponse(dateOfReg = Some(dateAsString), postcode = Some(postCode))
      val getVatInformationResponse: GetVatInformationResponse =
        GetVatInformationResponse(effectiveRegistrationDate = Some(date), postCode = Some("SA29 1AA"))

      val result = service.compareApiResponses(vatControlListResponse, getVatInformationResponse)
      result shouldBe false
    }
  }

  private def mockGetVatCustomerInformation()(response: EitherT[Future, Int, GetVatInformationResponse]): Unit =
    when(
      getVatCustomerInformationConnectorMock.getVatCustomerInformation(any())(ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response

}
