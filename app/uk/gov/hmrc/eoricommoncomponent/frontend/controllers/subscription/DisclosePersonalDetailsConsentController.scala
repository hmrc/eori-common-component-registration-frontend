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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.EoriConsentSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.disclose_personal_details_consent

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DisclosePersonalDetailsConsentController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionBusinessService: SubscriptionBusinessService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  disclosePersonalDetailsConsentView: disclose_personal_details_consent,
  subscriptionFlowManager: SubscriptionFlowManager
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(
          Ok(
            disclosePersonalDetailsConsentView(
              isInReviewMode = false,
              disclosePersonalDetailsYesNoAnswerForm,
              requestSessionData,
              service,
              journey
            )
          )
        )
    }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.getCachedPersonalDataDisclosureConsent.map { isConsentDisclosed =>
          Ok(
            disclosePersonalDetailsConsentView(
              isInReviewMode = true,
              disclosePersonalDetailsYesNoAnswerForm.fill(YesNo(isConsentDisclosed)),
              requestSessionData,
              service,
              journey
            )
          )
        }
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      disclosePersonalDetailsYesNoAnswerForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                disclosePersonalDetailsConsentView(isInReviewMode, formWithErrors, requestSessionData, service, journey)
              )
            ),
          yesNoAnswer =>
            subscriptionDetailsService.cacheConsentToDisclosePersonalDetails(yesNoAnswer).flatMap { _ =>
              if (isInReviewMode)
                Future.successful(Redirect(DetermineReviewPageController.determineRoute(service, journey).url))
              else
                Future.successful(
                  Redirect(
                    subscriptionFlowManager.stepInformation(EoriConsentSubscriptionFlowPage).nextPage.url(service)
                  )
                )
            }
        )
    }

}
