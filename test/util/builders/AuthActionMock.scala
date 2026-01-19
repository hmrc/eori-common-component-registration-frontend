/*
 * Copyright 2025 HM Revenue & Customs
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

package util.builders

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers.stubBodyParser
import play.api.test.Injecting
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthAction, CacheClearOnCompletionAction}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

trait AuthActionMock extends AnyWordSpec with MockitoSugar with Injecting {

  lazy val app: Application

  val configuration = inject[Configuration]
  val environment = Environment.simple()
  val mockedSessionCacheForAuth = mock[SessionCache]
  when(mockedSessionCacheForAuth.emailOpt(any[Request[AnyContent]]))
    .thenReturn(Future.successful(Some("some@email.com")))
  when(mockedSessionCacheForAuth.isJourneyComplete(any[Request[AnyContent]]))
    .thenReturn(Future.successful(false))

  val actionBuilder = DefaultActionBuilder(stubBodyParser(AnyContentAsEmpty))(global)

  def authAction(authConnector: AuthConnector) =
    new AuthAction(
      configuration,
      environment,
      authConnector,
      actionBuilder,
      mockedSessionCacheForAuth,
      inject[BodyParsers.Default],
      inject[CacheClearOnCompletionAction]
    )(global)

}
