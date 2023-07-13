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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisedFunctions}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthRedirectSupport, EnrolmentExtractor}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YouCannotUseServiceController @Inject() (
  override val config: Configuration,
  override val env: Environment,
  override val authConnector: AuthConnector,
  youCantUseService: you_cant_use_service,
  unauthorisedView: unauthorized,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with AuthorisedFunctions with AuthRedirectSupport with EnrolmentExtractor {

  def page(service: Service): Action[AnyContent] = Action.async { implicit request =>
    authorised(AuthProviders(GovernmentGateway))
      .retrieve(affinityGroup) { ag =>
        Future.successful(Unauthorized(youCantUseService(ag)))
      } recover withAuthRecovery(request)
  }

  def unauthorisedPage(service: Service): Action[AnyContent] = Action { implicit request =>
    Unauthorized(unauthorisedView())
  }

}
