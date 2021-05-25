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
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.subscription.routes.{
  ContactDetailsController,
  DateOfEstablishmentController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUserWithEnrolments
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.RowOrganisationFlow
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.subscription.CompanyRegisteredCountry
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries.Countries
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.subscription.{country_individual, country_organisation}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompanyRegisteredCountryController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  countryIndividualPage: country_individual,
  countryOrganisationPage: country_organisation
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedRegisteredCountry().map { countryOpt =>
        populateView(countryOpt, service, false)
      }
    }

  def reviewPage(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      subscriptionDetailsService.cachedRegisteredCountry().map { countryOpt =>
        populateView(countryOpt, service, true)
      }
    }

  private def populateView(country: Option[CompanyRegisteredCountry], service: Service, isInReviewMode: Boolean)(
    implicit request: Request[AnyContent]
  ): Result = {

    val form       = CompanyRegisteredCountry.form(errorMessageBasedOnType)
    val filledForm = country.fold(form)(form.fill(_))

    Ok(prepareViewBasedOnType(filledForm, service, isInReviewMode))
  }

  private def prepareViewBasedOnType(form: Form[CompanyRegisteredCountry], service: Service, isInReviewMode: Boolean)(
    implicit request: Request[AnyContent]
  ): Html = {
    val (countries, picker) = Countries.getCountryParametersForAllCountries()

    if (requestSessionData.userSubscriptionFlow == RowOrganisationFlow)
      countryOrganisationPage(form, countries, picker, service, isInReviewMode)
    else
      countryIndividualPage(form, countries, picker, service, isInReviewMode)
  }

  private def errorMessageBasedOnType()(implicit request: Request[AnyContent]): String =
    if (requestSessionData.userSubscriptionFlow == RowOrganisationFlow)
      "ecc.registered-company-country.organisation.error"
    else "ecc.registered-company-country.individual.error"

  def submit(service: Service, isInReviewMode: Boolean): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      CompanyRegisteredCountry
        .form(errorMessageBasedOnType)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(prepareViewBasedOnType(formWithErrors, service, isInReviewMode))),
          country =>
            subscriptionDetailsService.cacheRegisteredCountry(country).map { _ =>
              if (isInReviewMode)
                Redirect(DetermineReviewPageController.determineRoute(service, Journey.Subscribe))
              else
                redirectBasedOnTheJourney(service)
            }
        )
    }

  private def redirectBasedOnTheJourney(service: Service)(implicit request: Request[AnyContent]): Result =
    if (requestSessionData.userSubscriptionFlow == RowOrganisationFlow)
      Redirect(DateOfEstablishmentController.createForm(service, Journey.Subscribe))
    else
      Redirect(ContactDetailsController.createForm(service))

}
