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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth

import play.api.mvc.{AnyContent, Request, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.NoActiveSession
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.JourneyTypeFromUrl
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey.{Register, Subscribe}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName.service
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

trait AuthRedirectSupport extends AuthRedirects with JourneyTypeFromUrl {

  override val config: Configuration
  override val env: Environment

  private def continueUrl(implicit request: Request[AnyContent]) = {
    val baseUrl = config.get[String]("external-url.company-auth-frontend.continue-url")
    journeyFromUrl match {
      case Subscribe =>
        s"${baseUrl}/${service.code}/subscribe"
      case Register =>
        s"${baseUrl}/${service.code}/register"
      case _ => throw new IllegalArgumentException("No valid journey found in URL: " + request.path)
    }
  }

  def withAuthRecovery(implicit request: Request[AnyContent]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession => toGGLogin(continueUrl = continueUrl)
  }

}
