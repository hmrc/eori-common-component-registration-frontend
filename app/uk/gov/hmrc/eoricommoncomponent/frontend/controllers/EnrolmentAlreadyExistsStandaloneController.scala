/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{AuthAction, EnrolmentExtractor}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{eori_exists_user, registration_exists_group}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future

class EnrolmentAlreadyExistsStandaloneController @Inject()(
  authAction: AuthAction,
  eoriExistsView: eori_exists_user,
  eoriExistsForGroupView: registration_exists_group,
  mcc: MessagesControllerComponents
) extends FrontendController(mcc) with I18nSupport with EnrolmentExtractor {

  def enrolmentAlreadyExists(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserAction {
      implicit request => loggedInUser: LoggedInUserWithEnrolments =>
        val eoriNumber  = existingEoriForUser(loggedInUser.enrolments.enrolments).map(_.id)
        Future.successful(Ok(eoriExistsView(eoriNumber.getOrElse(throw new IllegalStateException("EORI number could not be retrieved")), false)))
    }

  def enrolmentAlreadyExistsAdminUser(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserAction {
      implicit request => loggedInUser: LoggedInUserWithEnrolments =>
        val eoriNumber  = existingEoriForUser(loggedInUser.enrolments.enrolments).map(_.id)
        Future.successful(Ok(eoriExistsView(eoriNumber.getOrElse(throw new IllegalStateException("EORI number could not be retrieved")), true)))
    }

  def enrolmentAlreadyExistsForGroup(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(eoriExistsForGroupView(service)))
    }

}