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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{DetermineReviewPageController, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.{enterNameDobForm, thirdCountryIndividualNameDateOfBirthForm}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.util.Require.requireThatUrlValue
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RowIndividualNameDateOfBirthController @Inject() (
  authAction: AuthAction,
  subscriptionDetailsService: SubscriptionDetailsService,
  mcc: MessagesControllerComponents,
  individualNameDob: match_namedob
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request =>
      _: LoggedInUserWithEnrolments =>
        Future.successful(Ok(individualNameDob(enterNameDobForm, organisationType, service)))
    }

  def reviewForm(organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      subscriptionDetailsService.cachedNameDobDetails flatMap {
        case Some(NameDobMatchModel(firstName, middleName, lastName, dateOfBirth)) =>
          val form = thirdCountryIndividualNameDateOfBirthForm.fill(
            NameDobMatchModel(firstName, middleName, lastName, dateOfBirth)
          )
          Future.successful(Ok(individualNameDob(form, organisationType, service)))
        case _ => Future.successful(Redirect(SecuritySignOutController.signOut(service)))
      }
    }

  def submit(isInReviewMode: Boolean, organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      thirdCountryIndividualNameDateOfBirthForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(individualNameDob(formWithErrors, organisationType, service))),
        form => submitDetails(isInReviewMode, form, organisationType, service)
      )
    }

  private def assertOrganisationTypeIsValid(cdsOrganisationType: String): Unit =
    requireThatUrlValue(
      formsByOrganisationTypes contains cdsOrganisationType,
      message = s"Invalid organisation type '$cdsOrganisationType'."
    )

  private lazy val formsByOrganisationTypes =
    Seq(CdsOrganisationType.ThirdCountryIndividualId, CdsOrganisationType.ThirdCountrySoleTraderId)

  private def submitDetails(
    isInReviewMode: Boolean,
    formData: NameDobMatchModel,
    organisationType: String,
    service: Service
  )(implicit request: Request[_]): Future[Result] = {
    val nameDobMatchModel =
      NameDobMatchModel(formData.firstName, formData.middleName, formData.lastName, formData.dateOfBirth)

    subscriptionDetailsService.cacheNameDobDetails(nameDobMatchModel) map { _ =>
      if (isInReviewMode)
        Redirect(DetermineReviewPageController.determineRoute(service))
      else
        Redirect(DoYouHaveAUtrNumberController.form(organisationType, service, false))
    }
  }

}
