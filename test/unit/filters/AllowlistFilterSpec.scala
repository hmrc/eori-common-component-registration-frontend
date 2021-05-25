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

package unit.filters

import base.UnitSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Cookie, RequestHeader, Result, Results, SessionCookieBaker}
import play.api.test.{FakeRequest, NoMaterializer}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.filters.AllowlistFilter

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class AllowlistFilterSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val config = mock[AppConfig]
  private val next   = mock[RequestHeader => Future[Result]]

  private val mockSessionCookieBaker = mock[SessionCookieBaker]

  private def filter: AllowlistFilter = new AllowlistFilter(config, mockSessionCookieBaker)(NoMaterializer, global)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(mockSessionCookieBaker.encodeAsCookie(any())).thenReturn(Cookie("cookie", "value"))
  }

  override protected def afterEach(): Unit = {
    reset(next, config, mockSessionCookieBaker)

    super.afterEach()
  }

  "AllowlistFilter on restricted route" should {

    "Do nothing" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq("123"))

      val request =
        FakeRequest("GET", "/customs-enrolment-services/register").withHeaders(HeaderNames.REFERER -> "123")

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe None
    }
  }

  "AllowlistFilter on permitted route" should {

    val requestOnPermittedRoute = FakeRequest("GET", "/customs-enrolment-services/subscribe")

    "Do nothing for blank referer allowlist" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq.empty)

      val request = requestOnPermittedRoute

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe None
    }

    "Do nothing for blank referer header" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq.empty)

      val request = requestOnPermittedRoute.withHeaders(HeaderNames.REFERER -> "")

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe None
    }

    "Do nothing for unmatched referer" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq("123"))

      val request = requestOnPermittedRoute.withHeaders(HeaderNames.REFERER -> "abc")

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe None
    }

    "Append Session Param for exact matched referer" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq("123"))

      val request = requestOnPermittedRoute.withHeaders(HeaderNames.REFERER -> "123")

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe Some("true")
    }

    "Append Session Param for contains matched referer" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq("123"))

      val request = requestOnPermittedRoute.withHeaders(HeaderNames.REFERER -> "01234")

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe Some("true")
    }

    "Append Session Param on permitted child route with matched referrer" in {

      when(next.apply(any[RequestHeader])).thenReturn(Future.successful(Results.Ok))
      when(config.allowlistReferrers).thenReturn(Seq("test"))

      val request =
        FakeRequest("GET", "/customs-enrolment-services/subscribe/some-path").withHeaders(HeaderNames.REFERER -> "test")

      val result = await(filter.apply(next)(request))

      result.session(request).get("allowlisted") shouldBe Some("true")
    }
  }
}
