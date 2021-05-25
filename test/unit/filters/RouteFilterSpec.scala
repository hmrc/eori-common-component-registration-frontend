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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import base.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, spy, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{RequestHeader, Result, Results}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import uk.gov.hmrc.eoricommoncomponent.frontend.CdsErrorHandler
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.filters.RouteFilter
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class RouteFilterSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val env: Environment          = Environment.simple()
  val realConfig: Configuration = Configuration.load(env)

  implicit val system            = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  val mockErrorHandler           = mock[CdsErrorHandler]
  val mockConfig                 = spy(realConfig)
  val mockServicesConfig         = mock[ServicesConfig]

  private def filter =
    new RouteFilter(new AppConfig(mockConfig, mockServicesConfig, "test"), mockErrorHandler)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(mockErrorHandler.onClientError(any(), any(), any())).thenReturn(Future.successful(Results.NotFound("Content")))
  }

  override protected def afterEach(): Unit = {
    reset(mockErrorHandler, mockConfig)
    super.afterEach()
  }

  "RouteFilter" should {

    "ignore the filter when blocked routes not defined" in {

      whenRoutesToBlock(None)
      val request = FakeRequest("GET", "/some-url")

      val headerToResultFunction: RequestHeader => Future[Result] = _ => Future.successful(Results.Ok)

      val result: Result = await(filter.apply(headerToResultFunction)(request))

      status(result) shouldBe 200
    }

    "ignore the filter when there are no blocked routes" in {

      whenRoutesToBlock(Some(""))
      val request = FakeRequest("GET", "/some-url")

      val headerToResultFunction: RequestHeader => Future[Result] = _ => Future.successful(Results.Ok)

      val result: Result = await(filter.apply(headerToResultFunction)(request))

      status(result) shouldBe 200
    }

    "return 404 when blocked routes contains a URL that matches" in {
      whenRoutesToBlock(Some("/some-url"))

      thenRouteIsBlocked("/some-url/get-access")
    }

    "return 200 when blocked routes contains a URL that doesn't match" in {
      whenRoutesToBlock(Some("/some-url"))
      val request = FakeRequest("GET", "/some-other-url")

      val result: Result = await(filter.apply(okAction)(request))

      status(result) shouldBe 200
    }

    "block access to multiple routes" in {
      whenRoutesToBlock(Some("cds/subscribe,register"))

      thenRouteIsBlocked("/customs-enrolment-services/cds/subscribe/matching/user-location")

      thenRouteIsBlocked("/customs-enrolment-services/register/vat-group")
    }

  }

  private def thenRouteIsBlocked(url: String) = {
    val request        = FakeRequest("GET", url)
    val result: Result = await(filter.apply(okAction)(request))
    status(result) shouldBe 404
  }

  private def whenRoutesToBlock(routes: Option[String]) =
    when(mockConfig.getOptional[String]("routes-to-block")).thenReturn(routes)

  private val okAction = (rh: RequestHeader) => Future.successful(Results.Ok)
}
