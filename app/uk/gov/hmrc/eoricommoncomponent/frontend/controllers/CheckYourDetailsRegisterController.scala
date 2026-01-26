/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegisterWithoutIdWithSubscriptionService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.CheckYourDetailsRegisterConstructor
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.check_your_details_register

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourDetailsRegisterController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  sessionCacheService: SessionCacheService,
  mcc: MessagesControllerComponents,
  checkYourDetailsRegisterView: check_your_details_register,
  registerWithoutIdWithSubscription: RegisterWithoutIdWithSubscriptionService,
  viewModelConstructor: CheckYourDetailsRegisterConstructor
)(implicit ec: ExecutionContext)
    extends CdsController(mcc)
    with Logging {

  def reviewDetails(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => user: LoggedInUserWithEnrolments =>
      viewModelConstructor.generateViewModel(service).flatMap {
        case Some(viewModel) =>
          val result =
            Ok(checkYourDetailsRegisterView(viewModel, requestSessionData.userSelectedOrganisationType, service))
          sessionCacheService.individualAndSoleTraderRouter(
            user.groupId.getOrElse(throw new Exception("GroupId does not exists")),
            service,
            result
          )
        case None =>
          logger.warn("Data is missing from the cache so the user is being redirected to the start of the journey")
          Future.successful(Redirect(routes.EmailController.form(service)))
      }
    }

  def submitDetails(service: Service): Action[AnyContent] = authAction.enrolledUserWithSessionAction(service) {
    implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      registerWithoutIdWithSubscription.rowRegisterWithoutIdWithSubscription(loggedInUser, service)
  }

}
