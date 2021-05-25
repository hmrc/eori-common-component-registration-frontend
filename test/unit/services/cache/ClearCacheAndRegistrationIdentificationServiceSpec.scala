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

package unit.services.cache

import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUser
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  ClearCacheAndRegistrationIdentificationService,
  SessionCache
}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class ClearCacheAndRegistrationIdentificationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterAll {
  val mockSessionCache: SessionCache = mock[SessionCache]
  val loggedInUserId                 = "user-id"
  val Failure                        = new RuntimeException("something bad has happened")

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]
  val mockLoggedInUser: LoggedInUser        = mock[LoggedInUser]

  override protected def beforeAll() {
    when(mockLoggedInUser.userId()).thenReturn(loggedInUserId)
  }

  val service = new ClearCacheAndRegistrationIdentificationService(mockSessionCache)(global)

  "ClearCacheAndRegistrationIdentificationService" should {
    "clear cache and database" in {
      when(mockSessionCache.saveEmail(any())(any())).thenReturn(Future.successful(true))
      when(mockSessionCache.email).thenReturn(Future.successful("testEmail"))
      when(mockSessionCache.remove).thenReturn(Future.successful(true))

      await(service.clear(mockLoggedInUser)) should be(())
    }

    "return a failure if cache clear fails unexpectedly" in {
      when(mockSessionCache.remove).thenReturn(Future.failed(Failure))

      intercept[RuntimeException] {
        await(service.clear(mockLoggedInUser))
      } shouldBe Failure
    }
  }
}
