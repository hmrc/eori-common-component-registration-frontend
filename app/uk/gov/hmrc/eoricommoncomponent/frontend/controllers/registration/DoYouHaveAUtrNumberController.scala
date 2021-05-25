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
import play.api.data.Form
import play.api.mvc.{Action, _}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.{GetUtrNumberController, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.haveUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.registration.match_organisation_utr

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DoYouHaveAUtrNumberController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  matchOrganisationUtrView: match_organisation_utr,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val OrganisationModeDM = "organisation"

  def form(
    organisationType: String,
    service: Service,
    journey: Journey.Value,
    isInReviewMode: Boolean = false
  ): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedUtrMatch.map { cachedUtrOpt =>
        val form = cachedUtrOpt.fold(haveUtrForm)(haveUtrForm.fill(_))

        Ok(matchOrganisationUtrView(form, organisationType, OrganisationModeDM, service, journey, isInReviewMode))
      }
    }

  def submit(
    organisationType: String,
    service: Service,
    journey: Journey.Value,
    isInReviewMode: Boolean = false
  ): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      haveUtrForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(organisationType, formWithErrors, service, journey))),
        formData => destinationsByAnswer(formData, organisationType, service, journey, isInReviewMode)
      )
    }

  private def destinationsByAnswer(
    formData: UtrMatchModel,
    organisationType: String,
    service: Service,
    journey: Journey.Value,
    isInReviewMode: Boolean
  )(implicit request: Request[AnyContent]): Future[Result] =
    subscriptionDetailsService.cachedUtrMatch.flatMap { cachedUtrOpt =>
      formData.haveUtr match {
        case Some(true) =>
          subscriptionDetailsService.cacheUtrMatch(Some(formData)).map {
            _ =>
              Redirect(GetUtrNumberController.form(organisationType, service, journey, isInReviewMode))
          }
        case Some(false) if cachedUtrOpt.exists(_.haveUtr.exists(_ == false)) =>
          Future.successful(noUtrDestination(organisationType, service, journey, isInReviewMode))
        case Some(false) =>
          subscriptionDetailsService.updateSubscriptionDetails.flatMap { _ =>
            subscriptionDetailsService.cacheUtrMatch(Some(formData)).map { _ =>
              noUtrDestination(organisationType, service, journey, isInReviewMode)
            }
          }
        case _ =>
          throw new IllegalArgumentException("Have UTR should be Some(true) or Some(false) but was None")
      }
    }

  private def noUtrDestination(
    organisationType: String,
    service: Service,
    journey: Journey.Value,
    isInReviewMode: Boolean
  ): Result =
    organisationType match {
      case CdsOrganisationType.CharityPublicBodyNotForProfitId =>
        Redirect(VatRegisteredUkController.form(service))
      case CdsOrganisationType.ThirdCountryOrganisationId =>
        noUtrThirdCountryOrganisationRedirect(isInReviewMode, organisationType, service, journey)
      case CdsOrganisationType.ThirdCountrySoleTraderId | CdsOrganisationType.ThirdCountryIndividualId =>
        noUtrThirdCountryIndividualsRedirect(service, journey)
      case _ =>
        Redirect(YouNeedADifferentServiceController.form(journey))
    }

  private def noUtrThirdCountryOrganisationRedirect(
    isInReviewMode: Boolean,
    organisationType: String,
    service: Service,
    journey: Journey.Value
  ): Result =
    if (isInReviewMode)
      Redirect(DetermineReviewPageController.determineRoute(service, journey))
    else
      Redirect(
        SixLineAddressController
          .showForm(isInReviewMode = false, organisationType, service, journey)
      )

  private def noUtrThirdCountryIndividualsRedirect(service: Service, journey: Journey.Value): Result =
    Redirect(DoYouHaveNinoController.displayForm(service, journey))

  private def view(organisationType: String, form: Form[UtrMatchModel], service: Service, journey: Journey.Value)(
    implicit request: Request[AnyContent]
  ): HtmlFormat.Appendable =
    matchOrganisationUtrView(form, organisationType, OrganisationModeDM, service, journey)

}
