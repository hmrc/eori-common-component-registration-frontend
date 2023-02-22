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
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.VatDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{SubscriptionBusinessService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_group

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatGroupController @Inject() (mcc: MessagesControllerComponents, vatGroupView: vat_group, authAction: AuthAction, subscriptionDetailsService: SubscriptionDetailsService, subscriptionBusinessService: SubscriptionBusinessService)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] = authAction.ggAuthorisedUserWithEnrolmentsAction {
        implicit request => _: LoggedInUserWithEnrolments =>
          Future.successful(Ok(vatGroupView(vatGroupYesNoAnswerForm(), service)))
  }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request =>
        _: LoggedInUserWithEnrolments =>
          for {
            isVatGroup <- subscriptionBusinessService.getCachedVatGroup
            yesNo: YesNo = YesNo(isVatGroup)
          } yield Ok(
            vatGroupView(
              isInReviewMode = true,
              vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP).fill(yesNo),
              isIndividualFlow,
              requestSessionData.isPartnershipOrLLP,
              service
            )
          )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request =>
      _: LoggedInUserWithEnrolments =>
    vatGroupYesNoAnswerForm()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(vatGroupView(formWithErrors, service))),
        yesNoAnswer =>
          subscriptionDetailsService.cacheVatGroup(yesNoAnswer).flatMap {
            _ =>
              if (yesNoAnswer.isNo) Future.successful(Redirect(VatDetailsController.createForm(service)))
              else Future.successful(Redirect(routes.VatGroupsCannotRegisterUsingThisServiceController.form(service)))
          }
      )
  }

}
