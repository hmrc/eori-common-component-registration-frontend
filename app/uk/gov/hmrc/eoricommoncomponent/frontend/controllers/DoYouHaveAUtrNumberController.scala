/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.data.Form
import play.api.mvc._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.isRow
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.haveUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.match_organisation_utr

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DoYouHaveAUtrNumberController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  requestSessionData: RequestSessionData,
  matchOrganisationUtrView: match_organisation_utr,
  subscriptionDetailsService: SubscriptionDetailsService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val OrganisationModeDM = "organisation"

  def form(organisationType: String, service: Service, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
        Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
      else
        subscriptionDetailsService.cachedUtrMatch.map { cachedUtrOpt =>
          val form = cachedUtrOpt.fold(haveUtrForm)(haveUtrForm.fill(_))
          val userLocation: UserLocation = requestSessionData.selectedUserLocation.getOrElse(
            throw new RuntimeException("Unable to find user location in session")
          )
          Ok(
            matchOrganisationUtrView(form, organisationType, userLocation, OrganisationModeDM, service, isInReviewMode)
          )
        }
    }

  def submit(organisationType: String, service: Service, isInReviewMode: Boolean = false): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      haveUtrForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(organisationType, formWithErrors, service))),
          formData => destinationsByAnswer(formData, organisationType, service, isInReviewMode)
        )
    }

  private def destinationsByAnswer(
    formData: UtrMatchModel,
    organisationType: String,
    service: Service,
    isInReviewMode: Boolean
  )(implicit request: Request[AnyContent]): Future[Result] =
    subscriptionDetailsService.cachedUtrMatch.flatMap { cachedUtrOpt =>
      formData.haveUtr match {
        case Some(true) =>
          subscriptionDetailsService.cacheUtrMatch(Some(formData)).map { _ =>
            Redirect(GetUtrNumberController.form(organisationType, service, isInReviewMode))
          }
        case Some(false) if cachedUtrOpt.exists(_.haveUtr.exists(_ == false)) =>
          Future.successful(noUtrDestination(organisationType, service, isInReviewMode))
        case Some(false) =>
          subscriptionDetailsService.updateSubscriptionDetailsOrganisation.flatMap { _ =>
            subscriptionDetailsService.cacheUtrMatch(Some(formData)).map { _ =>
              noUtrDestination(organisationType, service, isInReviewMode)
            }
          }
        case _ =>
          throw new IllegalArgumentException("Have UTR must be Some(true) or Some(false) but was None")
      }
    }

  private def noUtrDestination(organisationType: String, service: Service, isInReviewMode: Boolean): Result =
    organisationType match {
      case CdsOrganisationType.CharityPublicBodyNotForProfitId =>
        if (appConfig.allowNoIdJourney) {
          Redirect(WhatIsYourOrganisationsAddressController.showForm(service))
        } else {
          Redirect(VatRegisteredUkKanaController.form(service))
        }
      case CdsOrganisationType.ThirdCountryOrganisationId =>
        noUtrOrganisationRedirect(isInReviewMode, organisationType, service)
      case CdsOrganisationType.ThirdCountrySoleTraderId | CdsOrganisationType.ThirdCountryIndividualId =>
        noUtrThirdCountryIndividualsRedirect(service)
      case _ =>
        Redirect(YouNeedADifferentServiceController.form(service))
    }

  private def noUtrOrganisationRedirect(isInReviewMode: Boolean, organisationType: String, service: Service): Result =
    if (isInReviewMode)
      Redirect(DetermineReviewPageController.determineRoute(service))
    else
      Redirect(
        SixLineAddressController
          .showForm(isInReviewMode = false, organisationType, service)
      )

  private def noUtrThirdCountryIndividualsRedirect(service: Service): Result =
    Redirect(DoYouHaveNinoController.displayForm(service))

  private def view(organisationType: String, form: Form[UtrMatchModel], service: Service)(implicit
    request: Request[AnyContent]
  ): HtmlFormat.Appendable = {
    val userLocation: UserLocation = requestSessionData.selectedUserLocation.getOrElse(
      throw new RuntimeException("Unable to find user location in session")
    )
    matchOrganisationUtrView(form, organisationType, userLocation, OrganisationModeDM, service)
  }

}
