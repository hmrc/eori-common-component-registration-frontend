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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.enterNameDobForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_namedob

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NameDobController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  matchNameDobView: match_namedob,
  cdsFrontendDataCache: SessionCache
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(matchNameDobView(enterNameDobForm, organisationType, service)))
    }

  def submit(organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      enterNameDobForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(matchNameDobView(formWithErrors, organisationType, service))),
        formData => submitNewDetails(formData, service)
      )
    }

  private def submitNewDetails(formData: NameDobMatchModel, service: Service)(implicit
    request: Request[_]
  ): Future[Result] =
    cdsFrontendDataCache.saveSubscriptionDetails(SubscriptionDetails(nameDobDetails = Some(formData))).map { _ =>
      Redirect(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.HowCanWeIdentifyYouController
          .createForm(service)
      )
    }

}
