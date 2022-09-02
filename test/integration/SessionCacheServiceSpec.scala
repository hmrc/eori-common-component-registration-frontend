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

package integration

import java.util.UUID
import common.support.testdata.registration.RegistrationInfoGenerator._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json.toJson
import play.api.mvc.{Request, Session}
import play.libs.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{BusinessShortName, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{CachedData, SessionCache, SessionTimeOutException}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.mongo.test.MongoSupport
import util.builders.RegistrationDetailsBuilder._

import scala.concurrent.ExecutionContext.Implicits.global

class SessionCacheSpec extends IntegrationTestsSpec with MockitoSugar with MongoSupport {

  lazy val appConfig = app.injector.instanceOf[AppConfig]

  val mockTimeStampSupport = new CurrentTimestampSupport()

  private val save4LaterService = app.injector.instanceOf[Save4LaterService]
  implicit val request: Request[Any] = mock[Request[Any]]
  val hc: HeaderCarrier              = mock[HeaderCarrier]
  val sessionCache = new SessionCache(appConfig, mongoComponent, save4LaterService, mockTimeStampSupport)

  "Session cache" should {

    "store, fetch and update Subscription details handler correctly" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-" + UUID.randomUUID()))))

      val holder = SubscriptionDetails(businessShortName = Some(BusinessShortName("some business name")))

      await(sessionCache.saveSubscriptionDetails(holder)(request))

      val expectedJson                     = toJson(CachedData(subDetails = Some(holder)))
      val cache = await(sessionCache.cacheRepo.findById(request))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.subscriptionDetails(request)) mustBe holder

      val updatedHolder = SubscriptionDetails(
        businessShortName = Some(BusinessShortName("different business name")),
        sicCode = Some("sic")
      )

      await(sessionCache.saveSubscriptionDetails(updatedHolder)(request))

      val expectedUpdatedJson                     = toJson(CachedData(subDetails = Some(updatedHolder)))
      val updatedCache                            = await(sessionCache.cacheRepo.findById(request))
      val Some(CacheItem(_, updatedJson, _, _)) = updatedCache
      updatedJson mustBe expectedUpdatedJson
    }

    "provide default when subscription details holder not in cache" in {
      when(hc.sessionId).thenReturn(Some(SessionId("does-not-exist")))

      val e1 = intercept[SessionTimeOutException] {
        await(sessionCache.subscriptionDetails(request))
      }
      e1.errorMessage mustBe "No match session id for signed in user with session : does-not-exist"

      val s1 = setupSession

      val e2 = intercept[SessionTimeOutException] {
        await(sessionCache.subscriptionDetails(request))
      }
      e2.errorMessage mustBe s"No match session id for signed in user with session : ${s1.value}"

      await(sessionCache.putSession(DataKey("regDetails"), data = Json.toJson(individualRegistrationDetails)))

      await(sessionCache.subscriptionDetails(request)) mustBe SubscriptionDetails()
    }

    "store, fetch and update Registration details correctly" in {
      val sessionId: SessionId = setupSession

      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))

      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson                     = toJson(CachedData(regDetails = Some(organisationRegistrationDetails)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.registrationDetails(request)) mustBe organisationRegistrationDetails
      await(sessionCache.saveRegistrationDetails(individualRegistrationDetails)(request))

      val updatedCache = await(sessionCache.cacheRepo.findById(request))

      val expectedUpdatedJson                     = toJson(CachedData(regDetails = Some(individualRegistrationDetails)))
      val Some(CacheItem(_, updatedJson, _, _)) = updatedCache
      updatedJson mustBe expectedUpdatedJson
    }

    "throw exception when registration Details requested and not available in cache" in {
      when(request.session).thenReturn(Session(Map(("sessionId", "sessionId-123"))))
      await(sessionCache.putSession(DataKey("sub01Outcome"), data = Json.toJson(sub01Outcome)))

      val caught = intercept[IllegalStateException] {
        await(sessionCache.registrationDetails(request))
      }
      caught.getMessage mustBe s"regDetails is not cached in data for the sessionId: sessionId-123"
    }

    "store, fetch and update Registration Info correctly" in {
      val sessionId: SessionId = setupSession

      await(sessionCache.saveRegistrationInfo(organisationRegistrationInfoWithAllOptionalValues)(request))

      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson                     = toJson(CachedData(regInfo = Some(organisationRegistrationInfoWithAllOptionalValues)))
      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson

      await(sessionCache.registrationInfo(request)) mustBe organisationRegistrationInfoWithAllOptionalValues
      await(sessionCache.saveRegistrationInfo(individualRegistrationInfoWithAllOptionalValues)(request))

      val updatedCache = await(sessionCache.cacheRepo.findById(request))

      val expectedUpdatedJson                     = toJson(CachedData(regInfo = Some(individualRegistrationInfoWithAllOptionalValues)))
      val Some(CacheItem(_, updatedJson, _, _)) = updatedCache
      updatedJson mustBe expectedUpdatedJson
    }

//    "throw exception when registration info requested and not available in cache" in {
//      val s = setupSession
//      await(sessionCache.insert(Cache(Id(s.value), data = Some(toJson(CachedData())))))
//
//      val caught = intercept[IllegalStateException] {
//        await(sessionCache.registrationInfo(request))
//      }
//      caught.getMessage mustBe s"regInfo is not cached in data for the sessionId: ${s.value}"
//    }

    "store Registration Details, Info and Subscription Details Holder correctly" in {
      val sessionId: SessionId = setupSession

      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))
      val holder = SubscriptionDetails()
      await(sessionCache.saveSubscriptionDetails(holder)(request))
      await(sessionCache.saveRegistrationInfo(organisationRegistrationInfoWithAllOptionalValues)(request))
      val cache = await(sessionCache.cacheRepo.findById(request))

      val expectedJson = toJson(
        CachedData(
          Some(organisationRegistrationDetails),
          Some(holder),
          Some(organisationRegistrationInfoWithAllOptionalValues)
        )
      )

      val Some(CacheItem(_, json, _, _)) = cache
      json mustBe expectedJson
    }

    "remove from the cache" in {
      val sessionId: SessionId = setupSession
      await(sessionCache.saveRegistrationDetails(organisationRegistrationDetails)(request))

      await(sessionCache.remove(request))

      val cached = await(sessionCache.cacheRepo.findById(request))
      cached mustBe None
    }
  }

  private def setupSession: SessionId = {
    val sessionId = SessionId("sessionId-" + UUID.randomUUID())
    when(hc.sessionId).thenReturn(Some(sessionId))
    sessionId
  }

}
