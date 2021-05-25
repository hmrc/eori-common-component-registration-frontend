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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{
  AuthAction,
  EnrolmentExtractor,
  GroupEnrolmentExtractor
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.UserLocationController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.{CdsController, MissingGroupId}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{ExistingEori, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.EoriNumberViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.subscription.SubscriptionForm.eoriNumberForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{
  EnrolmentStoreProxyService,
  SubscriptionBusinessService,
  SubscriptionDetailsService
}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIsYourEoriController @Inject() (
  authAction: AuthAction,
  subscriptionBusinessService: SubscriptionBusinessService,
  subscriptionDetailsHolderService: SubscriptionDetailsService,
  groupEnrolment: GroupEnrolmentExtractor,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  whatIsYourEoriView: what_is_your_eori
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with EnrolmentExtractor {

  def createForm(service: Service): Action[AnyContent] =
    displayForm(service, false)

  def reviewForm(service: Service): Action[AnyContent] =
    displayForm(service, true)

  private def displayForm(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => user: LoggedInUserWithEnrolments =>
        existingEori(user).flatMap {
          case Some(e) =>
            useExistingEori(e, service)
          case None =>
            subscriptionBusinessService.cachedEoriNumber.map(eori => populateView(eori, isInReviewMode, service))
        }

    }

  private def useExistingEori(eori: ExistingEori, service: Service)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    subscriptionDetailsHolderService.cacheExistingEoriNumber(eori).map { _ =>
      Redirect(
        uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.UseThisEoriController.display(service)
      )
    }

  private def populateView(eoriNumber: Option[String], isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Result = {
    val eoriForForm = eoriNumber.map(eoriWithoutCountry(_))

    val form = eoriForForm.map(EoriNumberViewModel.apply).fold(eoriNumberForm)(eoriNumberForm.fill)
    Ok(whatIsYourEoriView(form, isInReviewMode, UserLocation.isRow(requestSessionData), service))
  }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      eoriNumberForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              whatIsYourEoriView(formWithErrors, isInReviewMode, UserLocation.isRow(requestSessionData), service)
            )
          ),
        formData => submitNewDetails(formData, isInReviewMode, service)
      )
    }

  private def submitNewDetails(formData: EoriNumberViewModel, isInReviewMode: Boolean, service: Service)(implicit
    hc: HeaderCarrier
  ) = {
    val eori = eoriWithCountry(formData.eoriNumber)

    subscriptionDetailsHolderService.cacheEoriNumber(eori).flatMap { _ =>
      enrolmentStoreProxyService.isEnrolmentInUse(service, ExistingEori(eori, service.enrolmentKey)).map {
        case Some(ExistingEori(id, _)) if id.nonEmpty =>
          Redirect(
            uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.EoriUnableToUseController.displayPage(
              service
            )
          )
        case _ =>
          if (isInReviewMode) Redirect(DetermineReviewPageController.determineRoute(service, Journey.Subscribe))
          else Redirect(UserLocationController.form(service, Journey.Subscribe))
      }
    }
  }

  private def existingEori(
    user: LoggedInUserWithEnrolments
  )(implicit headerCarrier: HeaderCarrier): Future[Option[ExistingEori]] =
    groupEnrolment.groupIdEnrolments(user.groupId.getOrElse(throw MissingGroupId())).map {
      groupEnrolments =>
        existingEoriForUserOrGroup(user, groupEnrolments)
    }

  private def eoriWithoutCountry(eori: String): String = if (eori.startsWith("GB")) eori.drop(2) else eori

  private def eoriWithCountry(eori: String): String = if (eori.forall(_.isDigit)) "GB" + eori else eori

}
