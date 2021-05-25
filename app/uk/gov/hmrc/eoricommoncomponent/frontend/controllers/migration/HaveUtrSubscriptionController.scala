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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.migration.routes.GetUtrSubscriptionController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.CompanyRegisteredCountryController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.SubscriptionFlowManager
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{RowOrganisationFlow, UtrSubscriptionFlowPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.haveUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.match_utr_subscription
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HaveUtrSubscriptionController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  subscriptionFlowManager: SubscriptionFlowManager,
  mcc: MessagesControllerComponents,
  matchUtrSubscriptionView: match_utr_subscription,
  subscriptionDetailsService: SubscriptionDetailsService
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

  private def populateView(inReviewMode: Boolean, service: Service, journey: Journey.Value)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ) =
    requestSessionData.userSelectedOrganisationType match {
      case Some(orgType) =>
        subscriptionDetailsService.cachedUtrMatch.map {
          case Some(formData) =>
            Ok(matchUtrSubscriptionView(haveUtrForm.fill(formData), orgType.id, inReviewMode, service, journey))

          case _ => Ok(matchUtrSubscriptionView(haveUtrForm, orgType.id, inReviewMode, service, journey))
        }
      case None => noOrgTypeSelected
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        requestSessionData.userSelectedOrganisationType match {
          case Some(orgType) =>
            haveUtrForm.bindFromRequest.fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(matchUtrSubscriptionView(formWithErrors, orgType.id, isInReviewMode, service, journey))
                ),
              formData => destinationsByAnswer(isInReviewMode, formData, service, journey)
            )
          case None => noOrgTypeSelected
        }
    }

  private def destinationsByAnswer(
    isInReviewMode: Boolean,
    form: UtrMatchModel,
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    form.haveUtr match {
      case Some(true) =>
        subscriptionDetailsService.cacheUtrMatch(Some(form)).map {
          _ =>
            Redirect(
              if (isInReviewMode) GetUtrSubscriptionController.reviewForm(service, journey)
              else GetUtrSubscriptionController.createForm(service, journey)
            )
        }
      case Some(false) =>
        subscriptionDetailsService.cacheUtrMatchForNoAnswer(Some(form)).map {
          _ =>
            if (requestSessionData.userSubscriptionFlow == RowOrganisationFlow)
              Redirect(CompanyRegisteredCountryController.displayPage(service))
            else
              Redirect(subscriptionFlowManager.stepInformation(UtrSubscriptionFlowPage).nextPage.url(service))
        }
      case _ => throw new IllegalStateException("No Data from the form")
    }

  private lazy val noOrgTypeSelected = throw new IllegalStateException("No organisation type selected by user")

}
