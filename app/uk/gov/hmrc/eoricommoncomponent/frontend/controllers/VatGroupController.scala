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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsControllerOld
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_group

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatGroupController @Inject() (
  mcc: MessagesControllerComponents,
  vatGroupView: vat_group,
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionBusinessService: SubscriptionBusinessService,
  featureFlags: FeatureFlags
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(Ok(vatGroupView(isInReviewMode = false, vatGroupYesNoAnswerForm(), service)))
  }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          isVatGroup <- subscriptionBusinessService.getCachedVatGroup
          yesNo: YesNo = YesNo(isVatGroup)
        } yield Ok(vatGroupView(isInReviewMode = true, vatGroupYesNoAnswerForm().fill(yesNo), service))
    }

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatGroupYesNoAnswerForm()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(vatGroupView(isInReviewMode, formWithErrors, service))),
          yesNoAnswer =>
            subscriptionDetailsService.cacheVatGroup(yesNoAnswer).flatMap {
              _ =>
                vatDetailsRoutes(service, isInReviewMode, yesNoAnswer)
            }
        )
    }

  private def redirectCannotUseThisService(service: Service) = Future.successful(Redirect(routes.VatGroupsCannotRegisterUsingThisServiceController.form(service)))

  private def vatDetailsRoutes(service: Service, isInReviewMode: Boolean, yesNoAnswer: YesNo) = {
    if (featureFlags.useNewVATJourney) {
      if (isInReviewMode && yesNoAnswer.isNo) Future.successful(Redirect(VatDetailsController.reviewForm(service)))
      else if (yesNoAnswer.isNo) Future.successful(Redirect(VatDetailsController.createForm(service)))
      else redirectCannotUseThisService(service) //TODO: Go to new YES page
    }
    else
      if (isInReviewMode && yesNoAnswer.isNo) Future.successful(Redirect(VatDetailsControllerOld.reviewForm(service)))
      else if (yesNoAnswer.isNo) Future.successful(Redirect(VatDetailsControllerOld.createForm(service)))
      else redirectCannotUseThisService(service) //TODO: Remove this when VAT journey is live
  }
}
