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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{CdsOrganisationType, LoggedInUser, SixLineAddressMatchModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.RegistrationDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.countries._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.mapping.RegistrationDetailsCreator
import uk.gov.hmrc.eoricommoncomponent.frontend.util.Require.requireThatUrlValue
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.six_line_address

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SixLineAddressController @Inject() (
  authAction: AuthAction,
  regDetailsCreator: RegistrationDetailsCreator,
  subscriptionFlowManager: SubscriptionFlowManager,
  sessionCache: SessionCache,
  requestSessionData: RequestSessionData,
  mcc: MessagesControllerComponents,
  sixLineAddressView: six_line_address,
  registrationDetailsService: RegistrationDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def populateView(
    address: Option[Address],
    isInReviewMode: Boolean,
    organisationType: String,
    service: Service
  )(implicit request: Request[AnyContent]): Future[Result] = {
    val formByOrgType = formsByOrganisationTypes(request)(organisationType)
    lazy val form     = address.map(ad => createSixLineAddress(ad)).fold(formByOrgType)(formByOrgType.fill)
    val (countriesToInclude, countriesInCountryPicker) =
      Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)
    Future.successful(
      Ok(
        sixLineAddressView(
          UserLocation.isIOM(requestSessionData.selectedUserLocation.getOrElse("")),
          isInReviewMode,
          form,
          countriesToInclude,
          countriesInCountryPicker,
          organisationType,
          service
        )
      )
    )
  }

  def showForm(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUser =>
      assertOrganisationTypeIsValid(organisationType)
      sessionCache.registrationDetails.flatMap(
        rd => populateView(Some(rd.address), isInReviewMode, organisationType, service)
      )
    }

  def submit(isInReviewMode: Boolean = false, organisationType: String, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUser =>
      val (countriesToInclude, countriesInCountryPicker) =
        Countries.getCountryParameters(requestSessionData.selectedUserLocationWithIslands)
      assertOrganisationTypeIsValid(organisationType)(request)
      formsByOrganisationTypes(request)(organisationType).bindFromRequest.fold(
        invalidForm =>
          Future.successful(
            BadRequest(
              sixLineAddressView(
                UserLocation.isIOM(requestSessionData.selectedUserLocation.getOrElse("")),
                isInReviewMode,
                invalidForm,
                countriesToInclude,
                countriesInCountryPicker,
                organisationType,
                service
              )
            )
          ),
        formData => submitAddressDetails(isInReviewMode, formData, service)
      )
    }

  private def submitAddressDetails(isInReviewMode: Boolean, formData: SixLineAddressMatchModel, service: Service)(
    implicit request: Request[AnyContent]
  ): Future[Result] =
    if (isInReviewMode)
      registrationDetailsService
        .cacheAddress(regDetailsCreator.registrationAddress(formData))
        .map(
          _ =>
            Redirect(
              uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.DetermineReviewPageController.determineRoute(
                service
              )
            )
        )
    else
      registrationDetailsService.cacheAddress(regDetailsCreator.registrationAddress(formData)).flatMap { _ =>
        subscriptionFlowManager.startSubscriptionFlow(service)
      } map {
        case (firstSubscriptionPage, session) => Redirect(firstSubscriptionPage.url(service)).withSession(session)
      }

  private def assertOrganisationTypeIsValid(organisationType: String)(implicit request: Request[AnyContent]): Unit =
    requireThatUrlValue(
      formsByOrganisationTypes(request) contains organisationType,
      message = s"Invalid organisation type '$organisationType'."
    )

  private def formsByOrganisationTypes(implicit request: Request[AnyContent]) = {
    val form = requestSessionData.selectedUserLocationWithIslands(request) match {
      case Some(UserLocation.Islands) => islandsSixLineAddressForm
      case Some(UserLocation.Iom)     => iomSixLineAddressForm
      case Some(UserLocation.Uk)      => ukSixLineAddressForm
      case _                          => thirdCountrySixLineAddressForm
    }

    Map(
      CdsOrganisationType.ThirdCountryOrganisationId      -> form,
      CdsOrganisationType.ThirdCountryIndividualId        -> form,
      CdsOrganisationType.ThirdCountrySoleTraderId        -> form,
      CdsOrganisationType.IsleOfManOrganisationId         -> form,
      CdsOrganisationType.IsleOfManIndividualId           -> form,
      CdsOrganisationType.IsleOfManSoleTraderId           -> form,
      CdsOrganisationType.CharityPublicBodyNotForProfitId -> form
    )
  }

}
