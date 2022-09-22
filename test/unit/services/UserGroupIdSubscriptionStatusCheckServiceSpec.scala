/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.LOCATION
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CacheIds, GroupId, InternalId, SafeId}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{
  NewSubscription,
  PreSubscriptionStatus,
  Save4LaterService,
  SubscriptionExists,
  SubscriptionProcessing,
  SubscriptionRejected,
  SubscriptionStatusService,
  UserGroupIdSubscriptionStatusCheckService
}
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

  private def existingApplicationInProcess: Future[Result] =
    Future.successful(Redirect("/blocked/existingApplicationInProcess"))

  private def otherUserWithinGroupIsInProcess: Future[Result] =
    Future.successful(Redirect("/blocked/otherUserWithinGroupIsInProcess"))

  override implicit def patienceConfig: PatienceConfig =
    super.patienceConfig.copy(timeout = Span(defaultTimeout.toMillis, Millis))

  override def beforeEach() =
    reset(mockSubscriptionStatusService, mockSave4LaterService)

  "checksToProceed" should {
    "Allow the user when there's no cache against groupId" in {
      when(mockSave4LaterService.fetchCacheIds(any())(any[HeaderCarrier])) thenReturn Future.successful(None)
      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/continue"
    }

    "Allow the user for the groupID is cache for same service and subscription status is NewSubscription" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds)))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(NewSubscription))
      when(mockSave4LaterService.deleteCachedGroupId(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/continue"
    }

    "Allow the user for the groupID is cache for same service and subscription status is SubscriptionRejected" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds)))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionRejected))
      when(mockSave4LaterService.deleteCachedGroupId(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/continue"
    }

    "Allow the user for the groupID is cache for different service and subscription status is NewSubscription" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(serviceCode = Some("other")))))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(NewSubscription))
      when(mockSave4LaterService.deleteCacheIds(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/continue"
    }

    "Allow the user for the groupID is cache for different service and subscription status is SubscriptionRejected" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(serviceCode = Some("other")))))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionRejected))
      when(mockSave4LaterService.deleteCacheIds(any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/continue"
    }

    "block when SubscriptionProcessing for user" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds)))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionProcessing))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/blocked/existingApplicationInProcess"
    }

    "block when SubscriptionProcessing for group" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(internalId = InternalId("other")))))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionProcessing))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/blocked/otherUserWithinGroupIsInProcess"
    }

    "block when SubscriptionExists for user" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds)))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionExists))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/blocked/userIsInProcess"
    }

    "block when SubscriptionExists for group" in {
      when(
        mockSave4LaterService
          .fetchCacheIds(any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(cacheIds.copy(internalId = InternalId("other")))))
      when(mockSubscriptionStatusService.getStatus(any[String], any[String])(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful(SubscriptionExists))

      val result: Result = service
        .checksToProceed(groupId, internalId, atarService)(continue)(userIsInProcess)(existingApplicationInProcess)(
          otherUserWithinGroupIsInProcess
        ).futureValue

      result.header.headers(LOCATION) shouldBe "/blocked/otherUserWithinGroupIsInProcess"
    }
  }

}
