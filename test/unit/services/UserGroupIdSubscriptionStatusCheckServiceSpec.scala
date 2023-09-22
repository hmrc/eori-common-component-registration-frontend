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

import akka.dispatch.ThreadPoolConfig.defaultTimeout
import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, header, LOCATION}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CacheIds, GroupId, InternalId, SafeId}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.http.HeaderCarrier
import util.TestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserGroupIdSubscriptionStatusCheckServiceSpec
    extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with TestData {

  private val mockSubscriptionStatusService = mock[SubscriptionStatusService]
  private val mockSave4LaterService         = mock[Save4LaterService]
  private implicit val hc: HeaderCarrier    = mock[HeaderCarrier]
  private val safeId                        = SafeId("safeId")
  private val groupId                       = GroupId("groupId-123")
  private val internalId                    = InternalId("internalId-123")
  private val cacheIds                      = CacheIds(internalId, safeId, Some("atar"))
  implicit val request: Request[Any]        = mock[Request[Any]]

  private val service =
    new UserGroupIdSubscriptionStatusCheckService(mockSubscriptionStatusService, mockSave4LaterService)

  private def continue: Future[Result]        = Future.successful(Redirect("/continue"))
  private def userIsInProcess: Future[Result] = Future.successful(Redirect("/blocked/userIsInProcess"))

  private def otherUserWithinGroupIsInProcess: Future[Result] =
    Future.successful(Redirect("/blocked/otherUserWithinGroupIsInProcess"))

  override implicit def patienceConfig: PatienceConfig =
    super.patienceConfig.copy(timeout = Span(defaultTimeout.toMillis, Millis))

  override def beforeEach(): Unit =
    reset(mockSubscriptionStatusService)

  reset(mockSave4LaterService)

  "UserGroupIdSubscriptionStatusCheckService" should {

    "block the user for the groupID is cache for different service and subscription status is SubscriptionProcessing" in {

      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(serviceCode = Some("otherService")))))
      when(
        mockSubscriptionStatusService.getStatus(any[String], any[String])(
          any[HeaderCarrier],
          any[Service],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(SubscriptionProcessing))

      val result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(otherUserWithinGroupIsInProcess)

      header(LOCATION, result).value shouldBe "/blocked/userIsInProcess"
    }

    "block the user for the groupID is cache and subscription status is SubscriptionProcessing for some other user within the group" in {

      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(internalId = InternalId("otherUserInternalId")))))
      when(
        mockSubscriptionStatusService.getStatus(any[String], any[String])(
          any[HeaderCarrier],
          any[Service],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(SubscriptionProcessing))

      val result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(otherUserWithinGroupIsInProcess)

      header(LOCATION, result).value shouldBe "/blocked/otherUserWithinGroupIsInProcess"
    }

    "block the same user with the same groupID for same service and subscription status is SubscriptionProcessing" in {

      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(serviceCode = Some(atarService.code)))))
      when(
        mockSubscriptionStatusService.getStatus(any[String], any[String])(
          any[HeaderCarrier],
          any[Service],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(SubscriptionProcessing))

      val result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(otherUserWithinGroupIsInProcess)

      header(LOCATION, result).value shouldBe "/blocked/userIsInProcess"
    }

    "Allow the user for the groupID is cached and subscription status is SubscriptionRejected" in {

      allowUserWhenGroupIdCachedAndSubscriptionStatusIs(SubscriptionRejected)
    }

    "Allow the user for the groupID is cached and subscription status is NewSubscription" in {

      allowUserWhenGroupIdCachedAndSubscriptionStatusIs(NewSubscription)
    }

    "Allow the user for the groupID is cached and subscription status is SubscriptionExists" in {

      allowUserWhenGroupIdCachedAndSubscriptionStatusIs(SubscriptionExists)
    }

    "Delete cache for user when starting a second subscription" in {

      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(serviceCode = Some("other")))))
      when(
        mockSubscriptionStatusService.getStatus(any[String], any[String])(
          any[HeaderCarrier],
          any[Service],
          any[Request[_]]
        )
      )
        .thenReturn(Future.successful(NewSubscription))
      when(mockSave4LaterService.deleteCacheIds(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(otherUserWithinGroupIsInProcess)

      header(LOCATION, result).value shouldBe "/continue"
    }

    "Allow the user if groupID is not cached" in {

      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      val result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(otherUserWithinGroupIsInProcess)

      header(LOCATION, result).value shouldBe "/continue"
    }
  }

  private def allowUserWhenGroupIdCachedAndSubscriptionStatusIs(status: PreSubscriptionStatus) = {
    when(
      mockSave4LaterService
        .fetchCacheIds(any())(any[HeaderCarrier])
    ).thenReturn(Future.successful(Some(cacheIds)))
    when(
      mockSubscriptionStatusService.getStatus(any[String], any[String])(
        any[HeaderCarrier],
        any[Service],
        any[Request[_]]
      )
    )
      .thenReturn(Future.successful(status))
    when(mockSave4LaterService.deleteCachedGroupId(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

    val result = service
      .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(otherUserWithinGroupIsInProcess)

    header(LOCATION, result).value shouldBe "/continue"
  }

}
