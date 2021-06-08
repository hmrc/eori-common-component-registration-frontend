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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.SubscriptionDetails.EuVatDetailsLimit
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.VatEUConfirmSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionVatEUDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.vat_details_eu_confirm

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatDetailsEuConfirmController @Inject() (
  authAction: AuthAction,
  vatEUDetailsService: SubscriptionVatEUDetailsService,
  mcc: MessagesControllerComponents,
  vatDetailsEuConfirmView: vat_details_eu_confirm,
  subscriptionFlowManager: SubscriptionFlowManager
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatEUDetailsService.cachedEUVatDetails map {
        case Seq() => Redirect(VatRegisteredEuController.createForm(service))
        case details if details.size < EuVatDetailsLimit =>
          Ok(
            vatDetailsEuConfirmView(
              euVatLimitNotReachedYesNoAnswerForm,
              isInReviewMode = false,
              details,
              service,
              vatLimitNotReached = true
            )
          )
        case details =>
          Ok(
            vatDetailsEuConfirmView(
              euVatLimitNotReachedYesNoAnswerForm,
              isInReviewMode = false,
              details,
              service,
              vatLimitNotReached = false
            )
          )
      }
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatEUDetailsService.cachedEUVatDetails map {
        case Seq() => Redirect(VatRegisteredEuController.reviewForm(service))
        case details if details.size < EuVatDetailsLimit =>
          Ok(
            vatDetailsEuConfirmView(
              euVatLimitNotReachedYesNoAnswerForm,
              isInReviewMode = true,
              details,
              service,
              vatLimitNotReached = true
            )
          )
        case details =>
          Ok(
            vatDetailsEuConfirmView(
              euVatLimitNotReachedYesNoAnswerForm,
              isInReviewMode = true,
              details,
              service,
              vatLimitNotReached = false
            )
          )
      }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatEUDetailsService.cachedEUVatDetails flatMap (
        details =>
          if (details.size < EuVatDetailsLimit)
            underVatLimitSubmit(service, isInReviewMode)
          else overVatLimitSubmit(service, isInReviewMode)
      )
    }

  private def underVatLimitSubmit(service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    euVatLimitNotReachedYesNoAnswerForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          vatEUDetailsService.cachedEUVatDetails map { details =>
            BadRequest(
              vatDetailsEuConfirmView(
                formWithErrors,
                isInReviewMode = isInReviewMode,
                details,
                service,
                vatLimitNotReached = true
              )
            )
          },
        yesNoAnswer => Future.successful(redirect(yesNoAnswer, isInReviewMode, service))
      )

  private def redirect(yesNoAnswer: YesNo, isInReviewMode: Boolean, service: Service)(implicit
    rc: Request[AnyContent]
  ): Result =
    (yesNoAnswer.isYes, isInReviewMode) match {
      case (true, false) => Redirect(VatDetailsEuController.createForm(service))
      case (true, true)  => Redirect(VatDetailsEuController.reviewForm(service))
      case (false, true) =>
        Redirect(DetermineReviewPageController.determineRoute(service).url)
      case (false, false) =>
        Redirect(
          subscriptionFlowManager
            .stepInformation(VatEUConfirmSubscriptionFlowPage)
            .nextPage
            .url(service)
        )
    }

  private def overVatLimitSubmit(service: Service, isInReviewMode: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    euVatLimitReachedYesNoAnswerForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          vatEUDetailsService.cachedEUVatDetails map { details =>
            BadRequest(
              vatDetailsEuConfirmView(formWithErrors, isInReviewMode, details, service, vatLimitNotReached = false)
            )
          },
        _ =>
          if (isInReviewMode)
            Future.successful(Redirect(DetermineReviewPageController.determineRoute(service).url))
          else
            Future.successful(
              Redirect(
                subscriptionFlowManager
                  .stepInformation(VatEUConfirmSubscriptionFlowPage)
                  .nextPage
                  .url(service)
              )
            )
      )

}
