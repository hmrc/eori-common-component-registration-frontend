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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.ConfirmIndividualTypePage
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm.confirmIndividualTypeForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.confirm_individual_type

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmIndividualTypeController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  subscriptionFlowManager: SubscriptionFlowManager,
  confirmIndividualTypeView: confirm_individual_type,
  mcc: MessagesControllerComponents,
  sessionCacheService: SessionCacheService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      sessionCacheService.individualAndSoleTraderRouter(
        user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
        service,
        Ok(confirmIndividualTypeView(confirmIndividualTypeForm, service)).withSession(
          requestSessionData.sessionWithoutOrganisationType
        )
      )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      confirmIndividualTypeForm
        .bindFromRequest()
        .fold(
          invalidForm => Future.successful(BadRequest(confirmIndividualTypeView(invalidForm, service))),
          selectedIndividualType =>
            subscriptionFlowManager
              .startSubscriptionFlow(Some(ConfirmIndividualTypePage), selectedIndividualType, service) map { case (page, newSession) =>
              val sessionWithOrganisationType =
                requestSessionData.sessionWithOrganisationTypeAdded(newSession, selectedIndividualType)

              Redirect(page.url(service)).withSession(sessionWithOrganisationType)
            }
        )
    }

}
