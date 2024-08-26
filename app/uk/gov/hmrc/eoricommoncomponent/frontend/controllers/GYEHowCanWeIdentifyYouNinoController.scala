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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GYEHowCanWeIdentifyYouNinoController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  sessionCache: SessionCache,
  howCanWeIdentifyYouView: how_can_we_identify_you_nino,
  sessionCacheService: SessionCacheService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      sessionCacheService.individualAndSoleTraderRouter(
        user.groupId.getOrElse(throw new Exception("GroupId does not exists")), service,
        Ok(
          howCanWeIdentifyYouView(
            subscriptionNinoForm,
            isInReviewMode = false,
            routes.GYEHowCanWeIdentifyYouNinoController.submit(service),
            service = service
          )
        )
      )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      subscriptionNinoForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              howCanWeIdentifyYouView(
                formWithErrors,
                isInReviewMode = false,
                routes.GYEHowCanWeIdentifyYouNinoController.submit(service),
                service = service
              )
            )
          ),
        ninoForm =>
          sessionCache.saveNinoOrUtrDetails(NinoOrUtr(Some(Nino(ninoForm.id)))).map(_ => Redirect(PostCodeController.createForm(service)))
      )
    }

}
