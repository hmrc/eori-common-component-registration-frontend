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
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{EnrolmentStoreProxyConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  EnrolmentResponse,
  EnrolmentStoreProxyResponse,
  GroupId,
  KeyValue
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.EnrolmentStoreProxyService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreProxyServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter with ScalaFutures {

  private val mockEnrolmentStoreProxyConnector =
    mock[EnrolmentStoreProxyConnector]

  private val service                               = new EnrolmentStoreProxyService(mockEnrolmentStoreProxyConnector)
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  before {
    reset(mockEnrolmentStoreProxyConnector)
  }

  private val serviceName = "HMRC-CUS-ORG"
  private val state       = "Activated"
  private val identifier  = KeyValue("EORINumber", "10000000000000001")
  private val groupId     = GroupId("groupId")

  private val enrolmentResponse =
    EnrolmentResponse(serviceName, state, List(identifier))

  private val enrolmentResponseNotActive =
    EnrolmentResponse("SOME_SERVICE", "NotActive", List(identifier))

  private val serviceName1 = "HMRC-VAT-ORG"

  private val enrolmentResponseNoHmrcCusOrg =
    EnrolmentResponse(serviceName1, state, List(identifier))

  "EnrolmentStoreProxyService" should {

    "return all enrolments for the groupId" in {

      val rightValueForEitherT: Either[ResponseError, EnrolmentStoreProxyResponse] =
        Right(EnrolmentStoreProxyResponse(List(enrolmentResponse, enrolmentResponseNoHmrcCusOrg)))

      getEnrolmentByGroupId()(EitherT[Future, ResponseError, EnrolmentStoreProxyResponse] {
        rightValueForEitherT
      })

      val expected = List(
        EnrolmentResponse("HMRC-CUS-ORG", "Activated", List(KeyValue("EORINumber", "10000000000000001"))),
        EnrolmentResponse("HMRC-VAT-ORG", "Activated", List(KeyValue("EORINumber", "10000000000000001")))
      )

      service.enrolmentsForGroup(groupId).value.futureValue.map(res => res shouldBe expected)

      verify(mockEnrolmentStoreProxyConnector).getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
    }

    "exclude non-active enrolments for the groupId" in {
      val rightValueForEitherT: Either[ResponseError, EnrolmentStoreProxyResponse] =
        Right(EnrolmentStoreProxyResponse(List(enrolmentResponse, enrolmentResponseNotActive)))

      val expected =
        List(EnrolmentResponse("HMRC-CUS-ORG", "Activated", List(KeyValue("EORINumber", "10000000000000001"))))

      getEnrolmentByGroupId()(EitherT[Future, ResponseError, EnrolmentStoreProxyResponse] {
        rightValueForEitherT
      })

      service.enrolmentsForGroup(groupId).value.futureValue.map(res => res shouldBe expected)

      verify(mockEnrolmentStoreProxyConnector).getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
    }
  }

  def getEnrolmentByGroupId()(response: EitherT[Future, ResponseError, EnrolmentStoreProxyResponse]): Unit =
    when(
      mockEnrolmentStoreProxyConnector.getEnrolmentByGroupId(ArgumentMatchers.eq(groupId.id))(
        ArgumentMatchers.any[HeaderCarrier],
        any()
      )
    ) thenReturn response

}
