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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  DetermineReviewPageController,
  VatDetailsController,
  VatDetailsControllerOld,
  VatGroupController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.VatDetailsSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_registered_uk

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegisteredUkController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionFlowManager: SubscriptionFlowManager,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  vatRegisteredUkView: vat_registered_uk,
  featureFlags: FeatureFlags
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(
          Ok(
            vatRegisteredUkView(
              isInReviewMode = false,
              vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP),
              isIndividualFlow,
              requestSessionData.isPartnershipOrLLP,
              service
            )
          )
        )
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          isVatRegisteredUk <- subscriptionBusinessService.getCachedVatRegisteredUk
          yesNo: YesNo = YesNo(isVatRegisteredUk)
        } yield Ok(
          vatRegisteredUkView(
            isInReviewMode = true,
            vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP).fill(yesNo),
            isIndividualFlow,
            requestSessionData.isPartnershipOrLLP,
            service
          )
        )
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                vatRegisteredUkView(
                  isInReviewMode,
                  formWithErrors,
                  isIndividualFlow,
                  requestSessionData.isPartnershipOrLLP,
                  service
                )
              )
            ),
          yesNoAnswer =>
            subscriptionDetailsService.cacheVatRegisteredUk(yesNoAnswer).flatMap {
              _ =>
                val result = (
                  isInReviewMode,
                  yesNoAnswer.isYes,
                  featureFlags.useNewVATJourney,
                  featureFlags.edgeCaseJourney
                ) match {
                  case (_, true, _, true)      => Future.successful(VatGroupController.createForm(service).url)
                  case (false, true, false, _) => Future.successful(VatDetailsControllerOld.createForm(service).url)
                  case (true, true, false, _)  => Future.successful(VatDetailsControllerOld.reviewForm(service).url)
                  case (false, true, true, _)  => Future.successful(VatDetailsController.createForm(service).url)
                  case (true, true, true, _)   => Future.successful(VatDetailsController.reviewForm(service).url)
                  case (false, false, true, _) =>
                    subscriptionDetailsService.clearCachedUkVatDetails map { _ =>
                      subscriptionFlowManager.stepInformation(VatDetailsSubscriptionFlowPage).nextPage.url(service)
                    }
                  case (false, false, false, _) =>
                    subscriptionDetailsService.clearCachedUkVatDetailsOld map { _ =>
                      subscriptionFlowManager.stepInformation(VatDetailsSubscriptionFlowPage).nextPage.url(service)
                    }
                  case (true, false, _, _) =>
                    subscriptionDetailsService.clearCachedUkVatDetails flatMap { _ =>
                      Future.successful(DetermineReviewPageController.determineRoute(service).url)
                    }

                }
                result.map(t => Redirect(t))
            }
        )
    }

  private def isIndividualFlow(implicit rq: Request[AnyContent]) =
    subscriptionFlowManager.currentSubscriptionFlow.isIndividualFlow

}
