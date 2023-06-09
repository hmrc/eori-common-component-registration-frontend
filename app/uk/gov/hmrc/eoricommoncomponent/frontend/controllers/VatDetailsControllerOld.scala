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

import play.api.Logger

import javax.inject.{Inject, Singleton}
import java.time.LocalDate
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  InvalidResponse,
  NotFoundResponse,
  ServiceUnavailableResponse,
  VatControlListConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.VatDetailsSubscriptionFlowPage
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  LoggedInUserWithEnrolments,
  VatControlListRequest,
  VatControlListResponse
}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsOld
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.VatDetailsFormOld.vatDetailsFormOld
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{vat_details_old, we_cannot_confirm_your_identity}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatDetailsControllerOld @Inject() (
  authAction: AuthAction,
  subscriptionFlowManager: SubscriptionFlowManager,
  vatControlListConnector: VatControlListConnector,
  subscriptionBusinessService: SubscriptionBusinessService,
  mcc: MessagesControllerComponents,
  vatDetailsView: vat_details_old,
  errorTemplate: error_template,
  weCannotConfirmYourIdentity: we_cannot_confirm_your_identity,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {
  private val logger = Logger(this.getClass)

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(vatDetailsView(vatDetailsFormOld, isInReviewMode = false, service)))
    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        subscriptionBusinessService.getCachedUkVatDetailsOld.map {
          case Some(vatDetails) =>
            Ok(vatDetailsView(vatDetailsFormOld.fill(vatDetails), isInReviewMode = true, service))
          case None => Ok(vatDetailsView(vatDetailsFormOld, isInReviewMode = true, service))
        }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      vatDetailsFormOld.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(vatDetailsView(formWithErrors, isInReviewMode, service))),
        formData => lookupVatDetails(formData, isInReviewMode, service)
      )
    }

  private def lookupVatDetails(vatForm: VatDetailsOld, isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[Result] = {

    def isEffectiveDateAssociatedWithVrn(effectiveDate: Option[String]) =
      effectiveDate.map(LocalDate.parse).fold(false)(_ == vatForm.effectiveDate)

    def stripSpaces: String => String = s => s.filterNot(_.isSpaceChar)

    def isPostcodeAssociatedWithVrn(postcode: Option[String]) =
      postcode.fold(false)(stripSpaces(_) equalsIgnoreCase stripSpaces(vatForm.postcode))

    def confirmKnownFacts(knownFacts: VatControlListResponse) =
      isEffectiveDateAssociatedWithVrn(knownFacts.dateOfReg) && isPostcodeAssociatedWithVrn(knownFacts.postcode)

    vatControlListConnector.vatControlList(VatControlListRequest(vatForm.number)).flatMap {
      case Right(knownFacts) =>
        if (confirmKnownFacts(knownFacts))
          subscriptionDetailsService
            .cacheUkVatDetailsOld(vatForm)
            .map(
              _ =>
                subscriptionFlowManager.stepInformation(VatDetailsSubscriptionFlowPage) match {
                  case Right(flowInfo) =>
                    if (isInReviewMode)
                      Redirect(DetermineReviewPageController.determineRoute(service))
                    else
                      Redirect(flowInfo.nextPage.url(service))
                  case Left(_) =>
                    logger.warn(s"Unable to identify subscription flow: key not found in cache")
                    Redirect(ApplicationController.startRegister(service))
                }
            )
        else
          Future.successful(Redirect(VatDetailsControllerOld.vatDetailsNotMatched(isInReviewMode, service)))
      case Left(errorResponse) =>
        errorResponse match {
          case NotFoundResponse =>
            Future.successful(Redirect(VatDetailsControllerOld.vatDetailsNotMatched(isInReviewMode, service)))
          case InvalidResponse =>
            Future.successful(Redirect(VatDetailsControllerOld.vatDetailsNotMatched(isInReviewMode, service)))
          case ServiceUnavailableResponse => Future.successful(Results.ServiceUnavailable(errorTemplate()))
        }
    }
  }

  def vatDetailsNotMatched(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      val tryAgainUrl = isInReviewMode match {
        case true  => VatDetailsControllerOld.reviewForm(service).url
        case false => VatDetailsControllerOld.createForm(service).url
      }
      Future.successful(Ok(weCannotConfirmYourIdentity(isInReviewMode, tryAgainUrl, service)))
    }

}
