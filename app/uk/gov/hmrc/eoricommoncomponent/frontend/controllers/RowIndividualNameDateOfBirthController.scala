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

import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  DetermineReviewPageController,
  DoYouHaveAUtrNumberController,
  IndStCannotRegisterUsingThisServiceController,
  SecuritySignOutController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.isRow
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.thirdCountryIndividualNameDateOfBirthForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.util.Require.requireThatUrlValue
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.row_individual_name_dob

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RowIndividualNameDateOfBirthController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  rowIndividualNameDob: row_individual_name_dob
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUser =>
      if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
        Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
      else {
        assertOrganisationTypeIsValid(organisationType)
        Future.successful(
          Ok(
            rowIndividualNameDob(
              thirdCountryIndividualNameDateOfBirthForm,
              organisationType,
              service,
              isInReviewMode = false
            )
          )
        )
      }
    }

  def reviewForm(organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUser =>
      if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
        Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
      else {
        assertOrganisationTypeIsValid(organisationType)
        subscriptionDetailsService.cachedNameDobDetails flatMap {
          case Some(NameDobMatchModel(firstName, lastName, dateOfBirth)) =>
            val form = thirdCountryIndividualNameDateOfBirthForm.fill(
              IndividualNameAndDateOfBirth(firstName, lastName, dateOfBirth)
            )
            Future.successful(Ok(rowIndividualNameDob(form, organisationType, service, isInReviewMode = true)))
          case _ => Future.successful(Redirect(SecuritySignOutController.signOut(service)))
        }
      }
    }

  def submit(isInReviewMode: Boolean, organisationType: String, service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      thirdCountryIndividualNameDateOfBirthForm.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(rowIndividualNameDob(formWithErrors, organisationType, service, isInReviewMode))
          ),
        form => submitDetails(isInReviewMode, form, organisationType, service)
      )
    }

  private def assertOrganisationTypeIsValid(cdsOrganisationType: String): Unit =
    requireThatUrlValue(
      CdsOrganisationType.rowIndividualOrganisationIds contains cdsOrganisationType,
      message = s"Invalid organisation type '$cdsOrganisationType'."
    )

  private def submitDetails(
    isInReviewMode: Boolean,
    formData: IndividualNameAndDateOfBirth,
    organisationType: String,
    service: Service
  )(implicit request: Request[_]): Future[Result] = {
    val nameDobMatchModel = NameDobMatchModel(formData.firstName, formData.lastName, formData.dateOfBirth)

    subscriptionDetailsService.cacheNameDobDetails(nameDobMatchModel) flatMap { _ =>
      if (!isInReviewMode)
        subscriptionDetailsService.updateSubscriptionDetailsIndividual.map(
          _ => Redirect(DoYouHaveAUtrNumberController.form(organisationType, service, isInReviewMode = false))
        )
      else
        Future.successful(Redirect(DetermineReviewPageController.determineRoute(service)))
    }
  }

}
