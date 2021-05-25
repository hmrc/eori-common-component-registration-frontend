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

package unit.services.subscription

import base.UnitSpec
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EnrolmentStoreProxyConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  EnrolmentResponse,
  EnrolmentStoreProxyResponse,
  ExistingEori,
  GroupId,
  KeyValue
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest.ES1Response
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.EnrolmentStoreProxyService
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

  private val enrolmentStoreProxyResponse = EnrolmentStoreProxyResponse(List(enrolmentResponse))
  private val serviceName1                = "HMRC-VAT-ORG"

  private val enrolmentResponseNoHmrcCusOrg =
    EnrolmentResponse(serviceName1, state, List(identifier))

  private val enrolmentStoreProxyResponseNoHmrcCusOrg =
    EnrolmentStoreProxyResponse(List(enrolmentResponseNoHmrcCusOrg))

  "EnrolmentStoreProxyService" should {
    "return enrolment if they exist against the groupId" in {
      when(
        mockEnrolmentStoreProxyConnector
          .getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
      ).thenReturn(Future.successful(enrolmentStoreProxyResponse))

      await(service.enrolmentForGroup(groupId, Service.cds)) shouldBe Some(enrolmentResponse)

      verify(mockEnrolmentStoreProxyConnector).getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
    }

    "return enrolment if they exist against service  HMRC-CUS-ORG the groupId" in {
      when(
        mockEnrolmentStoreProxyConnector
          .getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
      ).thenReturn(Future.successful(enrolmentStoreProxyResponseNoHmrcCusOrg))

      await(service.enrolmentForGroup(groupId, Service.cds)) shouldBe None

      verify(mockEnrolmentStoreProxyConnector).getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
    }

    "return all enrolments for the groupId" in {
      when(
        mockEnrolmentStoreProxyConnector
          .getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
      ).thenReturn(
        Future.successful(EnrolmentStoreProxyResponse(List(enrolmentResponse, enrolmentResponseNoHmrcCusOrg)))
      )

      await(service.enrolmentsForGroup(groupId)) shouldBe List(enrolmentResponse, enrolmentResponseNoHmrcCusOrg)

      verify(mockEnrolmentStoreProxyConnector).getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
    }

    "exclude non-active enrolments for the groupId" in {
      when(
        mockEnrolmentStoreProxyConnector
          .getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
      ).thenReturn(Future.successful(EnrolmentStoreProxyResponse(List(enrolmentResponse, enrolmentResponseNotActive))))

      await(service.enrolmentsForGroup(groupId)) shouldBe List(enrolmentResponse)

      verify(mockEnrolmentStoreProxyConnector).getEnrolmentByGroupId(any[String])(meq(headerCarrier), any())
    }

    "return Existing EORI" when {

      "EORI is allocated to different groupId" in {

        val es1Response = ES1Response(Some(Seq("groupId")), None)

        when(mockEnrolmentStoreProxyConnector.queryGroupsWithAllocatedEnrolment(any())(any()))
          .thenReturn(Future.successful(es1Response))

        val existingEori = ExistingEori("Gb123456789123", "HMRC-GVMS-ORG")

        val result = service.isEnrolmentInUse(Service.withName("atar").get, existingEori)

        result.futureValue shouldBe Some(existingEori)
      }
    }

    "not return existing EORI" when {

      "EORI is not used for the service the user is attempting to enrol" in {

        val es1Response = ES1Response(None, None)

        when(mockEnrolmentStoreProxyConnector.queryGroupsWithAllocatedEnrolment(any())(any()))
          .thenReturn(Future.successful(es1Response))

        val existingEori = ExistingEori("Gb123456789123", "HMRC-GVMS-ORG")

        val result = service.isEnrolmentInUse(Service.withName("atar").get, existingEori)

        result.futureValue shouldBe None
      }
    }
  }

}
