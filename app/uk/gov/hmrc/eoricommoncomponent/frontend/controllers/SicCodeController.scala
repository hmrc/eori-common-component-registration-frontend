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

import play.api.Logger

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ApplicationController
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionForm.sicCodeform
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.SicCodeViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.sic_code

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SicCodeController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionFlowManager: SubscriptionFlowManager,
  subscriptionDetailsHolderService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  sicCodeView: sic_code,
  requestSessionData: RequestSessionData
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {
  private val logger = Logger(this.getClass)

  private def populateView(sicCode: Option[String], isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    lazy val form = sicCode.map(SicCodeViewModel).fold(sicCodeform)(sicCodeform.fill)
    Future.successful(
      Ok(
        sicCodeView(
          form,
          isInReviewMode,
          requestSessionData.userSelectedOrganisationType,
          service,
          requestSessionData.selectedUserLocation
        )
      )
    )
  }

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.cachedSicCode.flatMap(populateView(_, isInReviewMode = false, service))
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.getCachedSicCode.flatMap(
          sic => populateView(Some(sic), isInReviewMode = true, service)
        )
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      sicCodeform.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              sicCodeView(
                formWithErrors,
                isInReviewMode,
                requestSessionData.userSelectedOrganisationType,
                service,
                requestSessionData.selectedUserLocation
              )
            )
          ),
        formData => submitNewDetails(formData, isInReviewMode, service)
      )
    }

  private def submitNewDetails(formData: SicCodeViewModel, isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionDetailsHolderService
      .cacheSicCode(formData.sicCode.filterNot(_.isWhitespace))
      .map(
        _ =>
          subscriptionFlowManager.stepInformation(SicCodeSubscriptionFlowPage) match {
            case Right(flowInfo) =>
              if (isInReviewMode)
                Redirect(routes.DetermineReviewPageController.determineRoute(service))
              else
                Redirect(flowInfo.nextPage.url(service))
            case Left(_) =>
              logger.warn(s"Unable to identify subscription flow: key not found in cache")
              Redirect(ApplicationController.startRegister(service))
          }
      )

}
