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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.organisationNameForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.what_is_your_org_name

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIsYourOrgNameController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  whatIsYourOrgNameView: what_is_your_org_name,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def populateView(name: Option[String], isInReviewMode: Boolean, organisationType: String, service: Service)(
    implicit request: Request[AnyContent]
  ): Future[Result] = {
    val form = name.map(n => NameMatchModel(n)).fold(organisationNameForm)(organisationNameForm.fill)
    Future.successful(Ok(whatIsYourOrgNameView(isInReviewMode, form, organisationType, service)))
  }

  def showForm(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedNameDetails.flatMap(
        details => populateView(details.map(_.name), isInReviewMode, organisationType, service)
      )
    }

  def submit(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      organisationNameForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(whatIsYourOrgNameView(isInReviewMode, formWithErrors, organisationType, service))
          ),
        formData => submitOrgNameDetails(isInReviewMode, formData, organisationType, service)
      )
    }

  private def submitOrgNameDetails(
    isInReviewMode: Boolean,
    formData: NameMatchModel,
    organisationType: String,
    service: Service
  )(implicit request: Request[_]): Future[Result] =
    subscriptionDetailsService.cacheNameDetails(NameOrganisationMatchModel(formData.name)) flatMap { _ =>
      if (!isInReviewMode)
        subscriptionDetailsService.updateSubscriptionDetailsOrganisation.map(
          _ => Redirect(DoYouHaveAUtrNumberController.form(organisationType, service, false))
        )
      else
        Future.successful(Redirect(DetermineReviewPageController.determineRoute(service)))
    }

}
