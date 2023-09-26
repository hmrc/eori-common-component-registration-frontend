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
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmContactDetailsController @Inject() (
  authAction: AuthAction,
  confirmContactDetailsService: ConfirmContactDetailsService,
  sessionCache: SessionCache,
  mcc: MessagesControllerComponents,
  sub01OutcomeProcessingView: sub01_outcome_processing
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      confirmContactDetailsService.handleAddressAndPopulateView(service, isInReviewMode)
    }

  def submit(service: Service, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      YesNoWrongAddress
        .createForm()
        .bindFromRequest()
        .fold(
          formWithErrors => confirmContactDetailsService.handleFormWithErrors(isInReviewMode, formWithErrors, service),
          areDetailsCorrectAnswer =>
            confirmContactDetailsService.checkAddressDetails(service, isInReviewMode, areDetailsCorrectAnswer)
        )
    }

  def processing(service: Service): Action[AnyContent] = authAction.enrolledUserWithSessionAction(service) {
    implicit request => _: LoggedInUserWithEnrolments =>
      for {
        processedDate <- sessionCache.sub01Outcome.map(_.processedDate)
      } yield Ok(sub01OutcomeProcessingView(processedDate, service))
  }

}
