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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  ApplicationController,
  ContactDetailsController,
  VatDetailsController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.errors.SessionError
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
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  vatRegisteredUkView: vat_registered_uk
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val logger = Logger(this.getClass)

  def createForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) {
      implicit request => _: LoggedInUserWithEnrolments =>
        isIndividualFlow match {
          case Right(isIndividual) =>
            Future.successful(
              Ok(
                vatRegisteredUkView(
                  isInReviewMode = false,
                  vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP),
                  isIndividual,
                  requestSessionData.isPartnershipOrLLP,
                  service
                )
              )
            )
          case Left(_) =>
            logger.warn(s"Unable to identify subscription flow: key not found in cache")
            Future.successful(Redirect(ApplicationController.startRegister(service)))
        }

    }

  def reviewForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) {
      implicit request => _: LoggedInUserWithEnrolments =>
        for {
          isVatRegisteredUk <- subscriptionBusinessService.getCachedVatRegisteredUk
          yesNo: YesNo = YesNo(isVatRegisteredUk)
        } yield isIndividualFlow match {
          case Right(individual) =>
            Ok(
              vatRegisteredUkView(
                isInReviewMode = true,
                vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP).fill(yesNo),
                individual,
                requestSessionData.isPartnershipOrLLP,
                service
              )
            )
          case Left(_) =>
            logger.warn(s"Unable to identify subscription flow: key not found in cache in review mode")
            Redirect(ApplicationController.startRegister(service))
        }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      vatRegisteredUkYesNoAnswerForm(requestSessionData.isPartnershipOrLLP)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            isIndividualFlow match {
              case Right(individual) =>
                Future.successful(
                  BadRequest(
                    vatRegisteredUkView(
                      isInReviewMode,
                      formWithErrors,
                      individual,
                      requestSessionData.isPartnershipOrLLP,
                      service
                    )
                  )
                )
              case Left(_) =>
                logger.warn(s"Unable to identify subscription flow: key not found in cache in review mode")
                Future.successful(Redirect(ApplicationController.startRegister(service)))
            },
          yesNoAnswer =>
            subscriptionDetailsService.cacheVatRegisteredUk(yesNoAnswer).flatMap {
              _ =>
                val result = (isInReviewMode, yesNoAnswer.isYes) match {
                  case (false, true) => Future.successful(VatDetailsController.createForm(service).url)
                  case (true, true)  => Future.successful(VatDetailsController.reviewForm(service).url)
                  case (true, false) =>
                    subscriptionDetailsService.clearCachedUkVatDetails.map(
                      _ => ContactDetailsController.reviewForm(service).url
                    )
                  case (false, false) =>
                    subscriptionDetailsService.clearCachedUkVatDetails.map(
                      _ => ContactDetailsController.createForm(service).url
                    )

                }
                result.map(t => Redirect(t))
            }
        )
    }

  private def isIndividualFlow(implicit rq: Request[AnyContent]): Either[SessionError, Boolean] =
    requestSessionData.userSubscriptionFlow map {
      flow => flow.isIndividualFlow
    }

}
