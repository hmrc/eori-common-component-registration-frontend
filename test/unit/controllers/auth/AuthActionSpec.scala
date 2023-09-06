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

package unit.auth

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthAction, CacheClearOnCompletionAction}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Results.{Ok, Redirect}
import util.builders.AuthBuilder
import play.api.test.Helpers._
import play.api.test.CSRFTokenHelper
import play.api.test.FakeRequest
import org.scalatest.matchers.should.Matchers._
import play.api.{Configuration, Environment}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionSpec extends AnyWordSpec with MockitoSugar with BeforeAndAfterEach {

  private val configuration: Config =
    ConfigFactory.parseString("""
      |services-config.list="atar,gvms"
      |services-config.atar.enrolment=HMRC-ATAR-ORG
      |services-config.atar.shortName=ATaR
      |services-config.atar.callBack=/advance-tariff-application
      |services-config.atar.accessibility=/advance-tariff-application/accessibility
      |services-config.atar.friendlyName=Advance_Tariff_Rulings
      |services-config.atar.friendlyNameWelsh=Dyfarniadau_Tariffau_Uwch_(ATaR)
      |services-config.gvms.enrolment=HMRC-GVMS-ORG
      |services-config.gvms.shortName=GVMS
      |services-config.gvms.callBack=/goods-movement
      |services-config.gvms.accessibility=/goods-movement/accessibility
      |services-config.gvms.friendlyName=Goods_Movement
      |services-config.gvms.friendlyNameWelsh=Symud_Cerbydau_Nwyddau_(GVMS)
      """.stripMargin)

  val config                       = Configuration(configuration)
  val environment                  = Environment.simple()
  val actionBuilder                = DefaultActionBuilder(stubBodyParser(AnyContentAsEmpty))(global)
  val mockBodyParsers              = mock[BodyParsers.Default]
  val mockSessionCache             = mock[SessionCache]
  val mockAuthConnector            = mock[AuthConnector]
  val cacheClearOnCompletionAction = new CacheClearOnCompletionAction(mockSessionCache, mockBodyParsers)

  val authAction = new AuthAction(
    config,
    environment,
    mockAuthConnector,
    actionBuilder,
    mockSessionCache,
    mockBodyParsers,
    cacheClearOnCompletionAction
  )(global)

  object TestController {

    def someCall(service: Service) = authAction.enrolledUserWithSessionAction(service) { _ => _ =>
      Future.successful(Ok("We succeeded!"))
    }

    def someCallNotRedirecting(service: Service) = authAction.enrolledUserClearingCacheOnCompletionAction(service) {
      _ => _ =>
        Future.successful(Ok("We succeeded!"))
    }

  }

  override def beforeEach(): Unit = {
    reset(mockSessionCache)
    reset(mockAuthConnector)
    reset(mockBodyParsers)
  }

  def withFakeCSRF = CSRFTokenHelper.addCSRFToken(FakeRequest("GET", "/cds/subscribe"))

  "enrolledUserWithSessionAction" should {

    "redirect to the start page and clear the cache where the user has completed the journey" in {
      when(mockSessionCache.isJourneyComplete(any[Request[AnyContent]]))
        .thenReturn(Future.successful(true))
      when(mockSessionCache.emailOpt(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Some("some@email.com")))
      when(mockSessionCache.remove(any[Request[AnyContent]]))
        .thenReturn(Future.successful(true))

      val result =
        TestController.someCall(Service("cds", "HMRC-CUS-ORG", "", None, "", "", None)).apply(withFakeCSRF)
      status(result) shouldEqual SEE_OTHER
      await(result).header.headers("Location") should endWith("/customs-registration-services/cds/register")
    }

    "redirect to the start page for atar and clear the cache where the user has completed the journey" in {
      when(mockSessionCache.isJourneyComplete(any[Request[AnyContent]]))
        .thenReturn(Future.successful(true))
      when(mockSessionCache.emailOpt(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Some("some@email.com")))
      when(mockSessionCache.remove(any[Request[AnyContent]]))
        .thenReturn(Future.successful(true))

      val result =
        TestController.someCall(Service("atar", "HMRC-CUS-ORG", "", None, "", "", None)).apply(withFakeCSRF)
      status(result) shouldEqual SEE_OTHER
      await(result).header.headers("Location") should endWith("/customs-registration-services/atar/register")
    }

    "redirect to the start page without clearing the cache where the user has doesn't have an active session" in {
      when(mockSessionCache.isJourneyComplete(any[Request[AnyContent]]))
        .thenReturn(Future.successful(false))
      when(mockSessionCache.emailOpt(any[Request[AnyContent]]))
        .thenReturn(Future.successful(None))

      val result =
        TestController.someCall(Service("cds", "HMRC-CUS-ORG", "", None, "", "", None)).apply(withFakeCSRF)
      status(result) shouldEqual SEE_OTHER
      await(result).header.headers("Location") should endWith("/customs-registration-services/cds/register")
    }

    "pass the user to the controller logic where the user has a session which isn't complete and is authenticated" in {
      when(mockSessionCache.isJourneyComplete(any[Request[AnyContent]]))
        .thenReturn(Future.successful(false))
      when(mockSessionCache.emailOpt(any[Request[AnyContent]]))
        .thenReturn(Future.successful(Some("some@email.com")))
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("some@email.com"))

      val result =
        TestController.someCall(Service("cds", "HMRC-CUS-ORG", "", None, "", "", None)).apply(withFakeCSRF)
      status(result) shouldEqual OK
    }

  }

  "enrolledUserClearingCacheOnCompletionAction" should {

    "clear the cache where the user has completed the journey, and then pass the user to the controller logic" in {
      when(mockSessionCache.isJourneyComplete(any[Request[AnyContent]]))
        .thenReturn(Future.successful(true))
      when(mockSessionCache.remove(any[Request[AnyContent]]))
        .thenReturn(Future.successful(true))
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("some@email.com"))

      val result = TestController.someCallNotRedirecting(Service("cds", "HMRC-CUS-ORG", "", None, "", "", None)).apply(
        withFakeCSRF
      )
      status(result) shouldEqual OK
    }

    "don't clear the cache where the user has not completed the journey, and then pass the user to the controller logic" in {
      when(mockSessionCache.isJourneyComplete(any[Request[AnyContent]]))
        .thenReturn(Future.successful(false))
      AuthBuilder.withAuthorisedUser("user-2300121", mockAuthConnector, userEmail = Some("some@email.com"))

      val result = TestController.someCallNotRedirecting(Service("cds", "HMRC-CUS-ORG", "", None, "", "", None)).apply(
        withFakeCSRF
      )
      status(result) shouldEqual OK
    }

  }
}
