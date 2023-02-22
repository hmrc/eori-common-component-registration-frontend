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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{VatDetailsSubscriptionFlowPage, VatGroupFlowPage, VatRegisteredUkSubscriptionFlowPage}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_registered_uk

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegisteredUkController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionFlowManager: SubscriptionFlowManager,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  vatRegisteredUkView: vat_registered_uk
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
                if (isInReviewMode)
                  if (yesNoAnswer.isYes)
                    Future.successful(Redirect(VatDetailsController.reviewForm(service).url))
                  else
                    subscriptionDetailsService.clearCachedUkVatDetails flatMap { _ =>
                      Future.successful(Redirect(DetermineReviewPageController.determineRoute(service).url))
                    }
                else if (yesNoAnswer.isYes)
                  Future.successful(
                    Redirect(
                      subscriptionFlowManager.stepInformation(VatRegisteredUkSubscriptionFlowPage).nextPage.url(service)
                    )
                  )
                else
                  subscriptionDetailsService.clearCachedUkVatDetails flatMap { _ =>
                    Future.successful(
                      Redirect(
                        subscriptionFlowManager.stepInformation(VatDetailsSubscriptionFlowPage).nextPage.url(service)
                      )
                    )
                  }
            }
        )
    }

  private def isIndividualFlow(implicit rq: Request[AnyContent]) =
    subscriptionFlowManager.currentSubscriptionFlow.isIndividualFlow

}
