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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.organisationNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.what_is_your_org_name
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIsYourOrgNameController @Inject() (
  authAction: AuthAction,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  whatIsYourOrgNameView: what_is_your_org_name,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def populateView(
    name: Option[String],
    isInReviewMode: Boolean,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  )(implicit request: Request[AnyContent]): Future[Result] = {
    val form = name.map(n => NameMatchModel(n)).fold(organisationNameForm)(organisationNameForm.fill)
    Future.successful(Ok(whatIsYourOrgNameView(isInReviewMode, form, organisationType, service, journey)))
  }

  def showForm(
    isInReviewMode: Boolean = false,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  ): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedNameDetails.flatMap(
        details => populateView(details.map(_.name), isInReviewMode, organisationType, service, journey)
      )
    }

  def submit(
    isInReviewMode: Boolean = false,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  ): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      organisationNameForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(
            BadRequest(whatIsYourOrgNameView(isInReviewMode, formWithErrors, organisationType, service, journey))
          ),
        formData => submitOrgNameDetails(isInReviewMode, formData, organisationType, service, journey)
      )
    }

  private def submitOrgNameDetails(
    isInReviewMode: Boolean,
    formData: NameMatchModel,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
    subscriptionDetailsService.cacheNameDetails(NameOrganisationMatchModel(formData.name)) map { _ =>
      if (isInReviewMode)
        Redirect(DetermineReviewPageController.determineRoute(service, journey))
      else if (UserLocation.isRow(requestSessionData))
        Redirect(DoYouHaveAUtrNumberController.form(organisationType, service, journey, false))
      else
        Redirect(DoYouHaveAUtrNumberController.form(organisationType, service, journey, false))
    }

}
