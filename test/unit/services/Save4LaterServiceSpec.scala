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

package unit.services

import base.UnitSpec
import ch.qos.logback.classic.Logger
import org.apache.pekko.dispatch.ThreadPoolConfig.defaultTimeout
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, Reads}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.*
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Save4LaterServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with LogCapturing {
  private val mockSave4LaterConnector = mock[Save4LaterConnector]

  implicit private val hc: HeaderCarrier = mock[HeaderCarrier]
  private val safeId = SafeId("safeId")
  private val groupId = GroupId("groupId-123")

  private val organisationType: CdsOrganisationType =
    CdsOrganisationType.Company

  private val emailStatus = EmailStatus(Some("test@example.com"))

  private val safeIdKey = "safeId"
  private val orgTypeKey = "orgType"
  private val cacheIds = "cacheIds"
  private val groupIdKey = "cachedGroupId"
  private val emailKey = "email"
  private val userLocation = "userLocation"
  private val userLocationKey = "userLoc"

  private val service =
    new Save4LaterService(mockSave4LaterConnector)

  val connectorLogger: Logger =
    LoggerFactory
      .getLogger(classOf[Save4LaterService])
      .asInstanceOf[Logger]

  implicit override def patienceConfig: PatienceConfig =
    super.patienceConfig.copy(timeout = Span(defaultTimeout.toMillis, Millis))

  override protected def beforeEach(): Unit =
    reset(mockSave4LaterConnector)

  "Save4LaterService" should {
    "save the safeId against the users InternalId" in {
      when(
        mockSave4LaterConnector.put[SafeId](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(safeIdKey),
          ArgumentMatchers.eq(safeId)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      withCaptureOfLoggingFrom(connectorLogger) { events =>

        val res = service
          .saveSafeId(groupId, safeId)

        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
        }

        result shouldBe ((): Unit)
      }
    }

    "save the CdsOrganisationType against the users InternalId" in {
      when(
        mockSave4LaterConnector.put[CdsOrganisationType](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(orgTypeKey),
          ArgumentMatchers.eq(Some(organisationType))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      withCaptureOfLoggingFrom(connectorLogger) { events =>

        val res = service
          .saveOrgType(groupId, Some(organisationType))

        val maybeOrgType: Option[CdsOrganisationType] = Some(organisationType)

        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
        }

        result shouldBe ((): Unit)
      }
    }

    "save the email against the users InternalId" in {
      when(
        mockSave4LaterConnector.put[EmailStatus](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(emailKey),
          ArgumentMatchers.eq(Json.toJson(emailStatus))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      withCaptureOfLoggingFrom(connectorLogger) { events =>

        val res = service
          .saveEmail(groupId, emailStatus)

        val result = await(res)
        eventually(timeout(Span(30, Seconds))) {
          events should not be empty
          events.exists(_.getLevel.levelStr == "DEBUG") shouldBe true
        }

        result shouldBe ((): Unit)
      }
    }

    "save the user location against the users InternalId" in {
      when(
        mockSave4LaterConnector.put[UserLocation](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(userLocationKey),
          ArgumentMatchers.eq(Json.toJson(UserLocation.Iom))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val result: Unit = service
        .saveUserLocation(groupId, UserLocation.Iom)
        .futureValue
      result shouldBe ((): Unit)
    }

    "delete the Cached GroupId Id against the users InternalId" in {
      when(
        mockSave4LaterConnector.deleteKey[CacheIds](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(groupIdKey)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val result: Unit = service
        .deleteCachedGroupId(groupId)
        .futureValue
      result shouldBe ((): Unit)
    }

    "delete the Cached against the users InternalId" in {
      when(
        mockSave4LaterConnector.delete[CacheIds](
          ArgumentMatchers.eq(groupId.id)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val result: Unit = service
        .deleteCacheIds(groupId)
        .futureValue
      result shouldBe ((): Unit)
    }

    "fetch the SafeId for the users InternalId" in {
      when(
        mockSave4LaterConnector.get[SafeId](ArgumentMatchers.eq(groupId.id), ArgumentMatchers.eq(safeIdKey))(
          any[HeaderCarrier],
          any[Reads[SafeId]]
        )
      ).thenReturn(Future.successful(Some(safeId)))

      val result = service
        .fetchSafeId(groupId)
        .futureValue
      result shouldBe Some(safeId)
    }

    "fetch the CacheId for the users InternalId" in {
      when(
        mockSave4LaterConnector.get[CacheIds](ArgumentMatchers.eq(groupId.id), ArgumentMatchers.eq(groupIdKey))(
          any[HeaderCarrier],
          any[Reads[CacheIds]]
        )
      ).thenReturn(Future.successful(Some(cacheIds)))

      val result = service
        .fetchCacheIds(groupId)
        .futureValue
      result shouldBe Some(cacheIds)
    }

    "fetch the user location for the users user InternalId" in {
      when(
        mockSave4LaterConnector.get[UserLocation](ArgumentMatchers.eq(groupId.id), ArgumentMatchers.eq(userLocationKey))(
          any[HeaderCarrier],
          any[Reads[UserLocation]]
        )
      ).thenReturn(Future.successful(Some(userLocation)))

      val result = service
        .fetchUserLocation(groupId)
        .futureValue
      result shouldBe Some(userLocation)
    }

    "fetch the Processing Service from CacheIds" in {

      val cacheIds = CacheIds(
        InternalId(groupId.id),
        SafeId(safeId.id),
        Some("cds")
      )

      when(
        mockSave4LaterConnector.get[CacheIds](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(groupIdKey)
        )(any[HeaderCarrier], any[Reads[CacheIds]])
      ).thenReturn(Future.successful(Some(cacheIds)))

      val result = service
        .fetchProcessingService(groupId)
        .futureValue

      result.get.code shouldBe cacheIds.serviceCode.get
    }

    "fetch the Processing Service from CacheIds when none found" in {

      when(
        mockSave4LaterConnector.get[CacheIds](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(groupIdKey)
        )(any[HeaderCarrier], any[Reads[CacheIds]])
      ).thenReturn(Future.successful(None))

      val result = service
        .fetchProcessingService(groupId)
        .futureValue

      result shouldBe None
    }

    "fetch the Organisation Type for the users InternalId" in {
      when(
        mockSave4LaterConnector.get[CdsOrganisationType](
          ArgumentMatchers.eq(groupId.id),
          ArgumentMatchers.eq(orgTypeKey)
        )(any[HeaderCarrier], any[Reads[CdsOrganisationType]])
      ).thenReturn(Future.successful(Some(organisationType)))

      val result = service
        .fetchOrgType(groupId)
        .futureValue
      result shouldBe Some(organisationType)
    }

    "fetch the email for the users InternalId" in {
      when(
        mockSave4LaterConnector.get[EmailStatus](ArgumentMatchers.eq(groupId.id), ArgumentMatchers.eq(emailKey))(
          any[HeaderCarrier],
          any[Reads[EmailStatus]]
        )
      ).thenReturn(Future.successful(Some(emailStatus)))

      val result = service
        .fetchEmail(groupId)
        .futureValue
      result shouldBe Some(emailStatus)
    }
  }

}
