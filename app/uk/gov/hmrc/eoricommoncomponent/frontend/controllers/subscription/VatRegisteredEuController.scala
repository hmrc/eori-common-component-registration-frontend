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
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.{
  VatDetailsEuConfirmController,
  VatDetailsEuController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{
  SubscriptionPage,
  VatEUConfirmSubscriptionFlowPage,
  VatRegisteredEuSubscriptionFlowPage
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.VatEUDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  SubscriptionBusinessService,
  SubscriptionDetailsService,
  SubscriptionVatEUDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.vat_registered_eu

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegisteredEuController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsService: SubscriptionDetailsService,
  subscriptionVatEUDetailsService: SubscriptionVatEUDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  vatRegisteredEuView: vat_registered_eu,
  subscriptionFlowManager: SubscriptionFlowManager
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(
          Ok(
            vatRegisteredEuView(
              isInReviewMode = false,
              vatRegisteredEuYesNoAnswerForm(requestSessionData.isPartnership),
              isIndividualFlow,
              requestSessionData.isPartnership,
              service,
              journey
            )
          )
        )
    }

  def reviewForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.getCachedVatRegisteredEu map { isVatRegisteredEu =>
          Ok(
            vatRegisteredEuView(
              isInReviewMode = true,
              vatRegisteredEuYesNoAnswerForm(requestSessionData.isPartnership).fill(YesNo(isVatRegisteredEu)),
              isIndividualFlow,
              requestSessionData.isPartnership,
              service,
              journey
            )
          )
        }
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatRegisteredEuYesNoAnswerForm(requestSessionData.isPartnership)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                vatRegisteredEuView(
                  isInReviewMode,
                  formWithErrors,
                  isIndividualFlow,
                  requestSessionData.isPartnership,
                  service,
                  journey
                )
              )
            ),
          yesNoAnswer =>
            subscriptionDetailsService.cacheVatRegisteredEu(yesNoAnswer).flatMap { _ =>
              redirect(isInReviewMode, yesNoAnswer, service, journey)
            }
        )
    }

  private def redirect(isInReviewMode: Boolean, yesNoAnswer: YesNo, service: Service, journey: Journey.Value)(implicit
    rq: Request[AnyContent]
  ): Future[Result] =
    subscriptionVatEUDetailsService.cachedEUVatDetails flatMap { cachedEuVatDetails =>
      (isInReviewMode, yesNoAnswer.isYes) match {
        case (true, true) if cachedEuVatDetails.isEmpty =>
          Future.successful(Redirect(VatDetailsEuController.reviewForm(service, journey)))
        case (true, true)          => Future.successful(Redirect(VatDetailsEuConfirmController.reviewForm(service, journey)))
        case (inReviewMode, false) => redirectForNoAnswer(service, journey, inReviewMode)
        case (false, true)         => redirectForYesAnswer(service, journey, cachedEuVatDetails, isInReviewMode)
      }
    }

  private def redirectForYesAnswer(
    service: Service,
    journey: Journey.Value,
    cachedEuVatDetails: Seq[VatEUDetailsModel],
    isInReviewMode: Boolean
  )(implicit rq: Request[AnyContent]): Future[Result] =
    cachedEuVatDetails.isEmpty match {
      case true => redirectWithFlowManager(VatRegisteredEuSubscriptionFlowPage, service)
      case _ =>
        isInReviewMode match {
          case false => Future.successful(Redirect(VatDetailsEuConfirmController.createForm(service, journey)))
          case _     => Future.successful(Redirect(VatDetailsEuConfirmController.reviewForm(service, journey)))
        }
    }

  private def redirectForNoAnswer(service: Service, journey: Journey.Value, isInReviewMode: Boolean)(implicit
    rq: Request[AnyContent]
  ): Future[Result] =
    subscriptionVatEUDetailsService.saveOrUpdate(Seq.empty) flatMap { _ =>
      if (isInReviewMode)
        Future.successful(Redirect(DetermineReviewPageController.determineRoute(service, journey).url))
      else redirectWithFlowManager(VatEUConfirmSubscriptionFlowPage, service)
    }

  private def isIndividualFlow(implicit rq: Request[AnyContent]) =
    subscriptionFlowManager.currentSubscriptionFlow.isIndividualFlow

  private def redirectWithFlowManager(subPage: SubscriptionPage, service: Service)(implicit rq: Request[AnyContent]) =
    Future.successful(Redirect(subscriptionFlowManager.stepInformation(subPage).nextPage.url(service)))

}
