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

import java.time.LocalDate

import base.UnitSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{EnrolmentStoreProxyConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{EnrolmentService, MissingEnrolmentException}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class EnrolmentServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  private val enrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]
  private val taxEnrolmentsConnector       = mock[TaxEnrolmentsConnector]
  private val headerCarrier                = HeaderCarrier()

  private val enrolmentService = new EnrolmentService(enrolmentStoreProxyConnector, taxEnrolmentsConnector)(global)

  private val eori = "GB123456789012"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(enrolmentStoreProxyConnector, taxEnrolmentsConnector)

    when(taxEnrolmentsConnector.enrolAndActivate(any(), any())(any()))
      .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))
  }

  "Enrolment service on enrolWithExistingCDSEnrolment" should {

    "return NO_CONTENT" when {

      "user has eori and all calls are successful (with capitalised date of establishment)" in {

        val date       = LocalDate.now().toString
        val verifiers  = List(KeyValuePair(key = "DATEOFESTABLISHMENT", value = date))
        val knownFact  = KnownFact(List.empty, verifiers)
        val knownFacts = KnownFacts("atar", List(knownFact))

        when(enrolmentStoreProxyConnector.queryKnownFactsByIdentifiers(any())(any()))
          .thenReturn(Future.successful(Some(knownFacts)))

        val enrolmentKeyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
        val requestCaptor: ArgumentCaptor[GovernmentGatewayEnrolmentRequest] =
          ArgumentCaptor.forClass(classOf[GovernmentGatewayEnrolmentRequest])

        val expectedResult = GovernmentGatewayEnrolmentRequest(
          identifiers = List(Identifier("EORINumber", eori)),
          verifiers = List(Verifier("DATEOFESTABLISHMENT", date))
        )

        val result = enrolmentService.enrolWithExistingCDSEnrolment(eori, atarService)(headerCarrier)

        result.futureValue shouldBe NO_CONTENT

        verify(taxEnrolmentsConnector).enrolAndActivate(enrolmentKeyCaptor.capture(), requestCaptor.capture())(any())

        enrolmentKeyCaptor.getValue shouldBe "HMRC-ATAR-ORG"
        requestCaptor.getValue shouldBe expectedResult
      }

      "user has eori and all calls are successful (with camel case date of establishment)" in {

        val date       = LocalDate.now().toString
        val verifiers  = List(KeyValuePair(key = "DateOfEstablishment", value = date))
        val knownFact  = KnownFact(List.empty, verifiers)
        val knownFacts = KnownFacts("atar", List(knownFact))

        when(enrolmentStoreProxyConnector.queryKnownFactsByIdentifiers(any())(any()))
          .thenReturn(Future.successful(Some(knownFacts)))

        val enrolmentKeyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
        val requestCaptor: ArgumentCaptor[GovernmentGatewayEnrolmentRequest] =
          ArgumentCaptor.forClass(classOf[GovernmentGatewayEnrolmentRequest])

        val expectedResult = GovernmentGatewayEnrolmentRequest(
          identifiers = List(Identifier("EORINumber", eori)),
          verifiers = List(Verifier("DateOfEstablishment", date))
        )

        val result = enrolmentService.enrolWithExistingCDSEnrolment(eori, atarService)(headerCarrier)

        result.futureValue shouldBe NO_CONTENT

        verify(taxEnrolmentsConnector).enrolAndActivate(enrolmentKeyCaptor.capture(), requestCaptor.capture())(any())

        enrolmentKeyCaptor.getValue shouldBe "HMRC-ATAR-ORG"
        requestCaptor.getValue shouldBe expectedResult
      }
    }

    "throw MissingEnrolmentException" when {

      "query Known facts return None" in {

        when(enrolmentStoreProxyConnector.queryKnownFactsByIdentifiers(any())(any()))
          .thenReturn(Future.successful(None))

        intercept[MissingEnrolmentException] {
          await(enrolmentService.enrolWithExistingCDSEnrolment("GB64344234", atarService)(headerCarrier))
        }
      }

      "verifier from known facts is missing" in {

        val knownFacts = KnownFacts("atar", List.empty)
        when(enrolmentStoreProxyConnector.queryKnownFactsByIdentifiers(any())(any()))
          .thenReturn(Future.successful(Some(knownFacts)))

        intercept[MissingEnrolmentException] {
          await(enrolmentService.enrolWithExistingCDSEnrolment("GB234232342", atarService)(headerCarrier))
        }
      }

      "empty verifiers are returned" in {

        val knownFact  = KnownFact(List.empty, List.empty)
        val knownFacts = KnownFacts("atar", List(knownFact))

        when(enrolmentStoreProxyConnector.queryKnownFactsByIdentifiers(any())(any()))
          .thenReturn(Future.successful(Some(knownFacts)))

        intercept[MissingEnrolmentException] {
          await(enrolmentService.enrolWithExistingCDSEnrolment("GB234232342", atarService)(headerCarrier))
        }
      }

      "verifiers are returned but do not contain DoE" in {

        val verifiers  = List(KeyValuePair(key = "Postcode", value = "SW1A 2AA"))
        val knownFact  = KnownFact(List.empty, verifiers)
        val knownFacts = KnownFacts("atar", List(knownFact))

        when(enrolmentStoreProxyConnector.queryKnownFactsByIdentifiers(any())(any()))
          .thenReturn(Future.successful(Some(knownFacts)))

        intercept[MissingEnrolmentException] {
          await(enrolmentService.enrolWithExistingCDSEnrolment("GB234232342", atarService)(headerCarrier))
        }
      }
    }
  }
}
