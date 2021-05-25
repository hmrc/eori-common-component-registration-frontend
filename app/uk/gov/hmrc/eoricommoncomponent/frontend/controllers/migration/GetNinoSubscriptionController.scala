/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{NinoSubscriptionFlowPage, RowIndividualFlow}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetNinoSubscriptionController @Inject() (
  authAction: AuthAction,
  subscriptionFlowManager: SubscriptionFlowManager,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  getNinoSubscriptionView: how_can_we_identify_you_nino,
  subscriptionDetailsHolderService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        populateView(false, service, journey)
    }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        populateView(true, service, journey)
    }

  private def populateView(isInReviewMode: Boolean, service: Service, journey: Journey.Value)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ) =
    subscriptionDetailsHolderService.cachedCustomsId.map {
      case Some(Nino(id)) =>
        Ok(
          getNinoSubscriptionView(
            subscriptionNinoForm.fill(IdMatchModel(id)),
            isInReviewMode,
            routes.GetNinoSubscriptionController.submit(isInReviewMode, service, journey)
          )
        )

      case _ =>
        Ok(
          getNinoSubscriptionView(
            subscriptionNinoForm,
            isInReviewMode,
            routes.GetNinoSubscriptionController.submit(isInReviewMode, service, journey)
          )
        )
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionNinoForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                getNinoSubscriptionView(
                  formWithErrors,
                  isInReviewMode,
                  routes.GetNinoSubscriptionController.submit(isInReviewMode, service, journey)
                )
              )
            ),
          formData => cacheAndContinue(isInReviewMode, formData, service, journey)
        )
    }

  private def cacheAndContinue(isInReviewMode: Boolean, form: IdMatchModel, service: Service, journey: Journey.Value)(
    implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] =
    subscriptionDetailsHolderService.cacheCustomsId(Nino(form.id)).map(
      _ =>
        if (isInReviewMode && !isItRowJourney)
          Redirect(DetermineReviewPageController.determineRoute(service, journey))
        else
          Redirect(subscriptionFlowManager.stepInformation(NinoSubscriptionFlowPage).nextPage.url(service))
    )

  private def isItRowJourney()(implicit request: Request[AnyContent]): Boolean =
    requestSessionData.userSubscriptionFlow == RowIndividualFlow

}
