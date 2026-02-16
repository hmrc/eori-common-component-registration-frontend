/*
 * Copyright 2026 HM Revenue & Customs
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

package unit.audit

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

class AuditorSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockConnector = mock[AuditConnector]
  private val mockConfig = mock[AppConfig]

  private val appName = "test-app"

  when(mockConfig.appName).thenReturn(appName)

  // Stub including implicit parameters
  when(
    mockConnector.sendExtendedEvent(
      any[ExtendedDataEvent]
    )(any[HeaderCarrier], any[ExecutionContext])
  ).thenReturn(Future.successful(AuditResult.Success))

  private val auditor = new Auditor(mockConnector, mockConfig)

  private val path = "/test-path"
  private val details = Json.obj("key" -> "value")

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    // reset the mock to remove previous call counts
    reset(mockConnector)

    // re-stub after reset
    when(
      mockConnector.sendExtendedEvent(
        any[ExtendedDataEvent]
      )(any[HeaderCarrier], any[ExecutionContext])
    ).thenReturn(Future.successful(AuditResult.Success))
  }

  "Auditor" should {

    "send subscription event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendSubscriptionDataEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "Subscription"
      event.detail mustBe details
    }

    "send registration event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendRegistrationDataEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "Registration"
      event.detail mustBe details
    }

    "send enrolment store proxy event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendEnrolmentStoreCallEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "EnrolmentStoreProxyCall"
      event.detail mustBe details
    }

    "send registration display event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendRegistrationDisplayEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "RegistrationDisplay"
      event.detail mustBe details
    }

    "send subscription display event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendSubscriptionDisplayEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "SubscriptionDisplay"
      event.detail mustBe details
    }

    "send subscription status event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendSubscriptionStatusEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "SubscriptionStatus"
      event.detail mustBe details
    }

    "send issuer call event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendTaxEnrolmentIssuerCallEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "IssuerCall"
      event.detail mustBe details
    }

    "send customs datastore event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendCustomsDataStoreEvent(path, details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "CustomsDataStoreUpdate"
      event.detail mustBe details
    }

    "send subscription flow session failure event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendSubscriptionFlowSessionFailureEvent(details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "SubscriptionFlowSessionFailure"
      event.detail mustBe details
    }

    "send address validation failure event" in {
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      auditor.sendAddressValidationFailureEvent(details).futureValue

      verify(mockConnector).sendExtendedEvent(
        captor.capture()
      )(any[HeaderCarrier], any[ExecutionContext])

      val event = captor.getValue
      event.auditSource mustBe appName
      event.auditType mustBe "AddressValidationFailure"
      event.detail mustBe details
    }
  }
}
