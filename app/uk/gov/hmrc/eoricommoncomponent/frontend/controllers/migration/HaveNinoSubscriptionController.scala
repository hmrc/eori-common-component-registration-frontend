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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.GetNinoSubscriptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.CompanyRegisteredCountryController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{NinoSubscriptionFlowPage, RowIndividualFlow}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.haveRowIndividualsNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.match_nino_subscription
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HaveNinoSubscriptionController @Inject() (
  authAction: AuthAction,
  subscriptionFlowManager: SubscriptionFlowManager,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  matchNinoSubscriptionView: match_nino_subscription,
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
    subscriptionDetailsHolderService.cachedNinoMatch.map {
      case Some(formData) =>
        Ok(matchNinoSubscriptionView(haveRowIndividualsNinoForm.fill(formData), isInReviewMode, service, journey))

      case _ => Ok(matchNinoSubscriptionView(haveRowIndividualsNinoForm, isInReviewMode, service, journey))
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        haveRowIndividualsNinoForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(matchNinoSubscriptionView(formWithErrors, isInReviewMode, service, journey))),
          formData => destinationsByAnswer(isInReviewMode, formData, service, journey)
        )
    }

  private def destinationsByAnswer(
    isInReviewMode: Boolean,
    form: NinoMatchModel,
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    form.haveNino match {
      case Some(true) =>
        subscriptionDetailsHolderService
          .cacheNinoMatch(Some(form))
          .map(
            _ =>
              Redirect(
                if (isInReviewMode) GetNinoSubscriptionController.reviewForm(service, journey)
                else GetNinoSubscriptionController.createForm(service, journey)
              )
          )
      case Some(false) =>
        subscriptionDetailsHolderService.cacheNinoMatchForNoAnswer(Some(form)) map (
          _ =>
            if (requestSessionData.userSubscriptionFlow == RowIndividualFlow)
              Redirect(CompanyRegisteredCountryController.displayPage(service))
            else
              Redirect(subscriptionFlowManager.stepInformation(NinoSubscriptionFlowPage).nextPage.url(service))
        )
      case _ => throw new IllegalStateException("No Data from the form")
    }

}
