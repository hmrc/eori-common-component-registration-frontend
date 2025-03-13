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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth

import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompletedJourneyFilter @Inject() (service: Service, sessionCache: SessionCache, val parser: BodyParsers.Default)(implicit
  val executionContext: ExecutionContext
) extends ActionBuilder[Request, AnyContent]
    with ActionFilter[Request]
    with NewUserSession {

  override protected def filter[A](request: Request[A]) = {
    implicit val req = request

    for {
      journeyCompleted <- sessionCache.isJourneyComplete
      email            <- sessionCache.emailOpt
      _                <- clearSessionCacheIfComplete(journeyCompleted)
    } yield
      if (journeyCompleted || email.isEmpty)
        Some(Redirect(ApplicationController.startRegister(service)).withSession(newUserSession))
      else None
  }

  def clearSessionCacheIfComplete(isJourneyComplete: Boolean)(implicit request: Request[_]): Future[Boolean] =
    if (isJourneyComplete) sessionCache.remove
    else Future.successful(false)

}
