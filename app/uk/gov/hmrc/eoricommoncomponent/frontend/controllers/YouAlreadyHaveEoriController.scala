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

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.StandaloneAlreadyHaveEoriViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{standalone_already_have_eori, you_already_have_eori}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YouAlreadyHaveEoriController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  eoriExistsView: you_already_have_eori,
  standAloneEoriExistsView: standalone_already_have_eori,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  // Note: permitted for user with service enrolment
  def display(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithServiceAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(eoriExistsView(service)))
    }

  // Note: permitted for user with service enrolment
  def displayStandAlone(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithServiceAction {
      implicit request => loggedInUser: LoggedInUserWithEnrolments =>
        sessionCache.eori.map(
          eoriNumber =>
            Ok(
              standAloneEoriExistsView(
                eoriNumber,
                loggedInUser.isAdminUser,
                service,
                StandaloneAlreadyHaveEoriViewModel(loggedInUser.isAdminUser)
              )
            )
        )
    }

}
