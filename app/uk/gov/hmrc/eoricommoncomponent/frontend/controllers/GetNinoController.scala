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
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{ConfirmContactDetailsController, EmailController, IndStCannotRegisterUsingThisServiceController}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.isRow
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionNinoFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.RequestSessionData
import uk.gov.hmrc.eoricommoncomponent.frontend.services.{MatchingService, SubscriptionDetailsService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, how_can_we_identify_you_nino}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetNinoController @Inject() (
  authAction: AuthAction,
  matchingService: MatchingService,
  mcc: MessagesControllerComponents,
  matchNinoRowIndividualView: how_can_we_identify_you_nino,
  subscriptionDetailsService: SubscriptionDetailsService,
  errorView: error_template,
  requestSessionData: RequestSessionData,
  subscriptionNinoFormProvider: SubscriptionNinoFormProvider
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  val form: Form[IdMatchModel] = subscriptionNinoFormProvider.subscriptionNinoForm

  def displayForm(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (_: LoggedInUserWithEnrolments) =>
      if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
        Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
      else
        Future.successful(
          Ok(
            matchNinoRowIndividualView(
              form,
              isInReviewMode = false,
              routes.GetNinoController.submit(service),
              service = service
            )
          )
        )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => (loggedInUser: LoggedInUserWithEnrolments) =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                matchNinoRowIndividualView(
                  formWithErrors,
                  isInReviewMode = false,
                  routes.GetNinoController.submit(service),
                  service = service
                )
              )
            ),
          formData => matchIndividual(Nino(formData.id), service, formData, GroupId(loggedInUser.groupId))
        )
    }

  private def matchIndividual(id: CustomsId, service: Service, formData: IdMatchModel, groupId: GroupId)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ): Future[Result] =
    subscriptionDetailsService.cachedNameDobDetails flatMap {
      case Some(details) =>
        matchingService
          .matchIndividualWithId(
            id,
            Individual.withLocalDate(details.firstName, details.lastName, details.dateOfBirth),
            groupId
          )
          .fold(
            {
              case MatchingServiceConnector.matchFailureResponse => matchNotFoundBadRequest(formData, service)
              case MatchingServiceConnector.downstreamFailureResponse => Ok(errorView(service))
              case _ => InternalServerError(errorView(service))
            },
            _ => Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false))
          )
      case None => Future.successful(Redirect(EmailController.form(service)))
    }

  private def matchNotFoundBadRequest(formData: IdMatchModel, service: Service)(implicit
    request: Request[AnyContent]
  ): Result = {
    val errorMsg = Messages("cds.matching-error.individual-not-found")
    val errorForm = form.withGlobalError(errorMsg).fill(formData)
    BadRequest(
      matchNinoRowIndividualView(errorForm, isInReviewMode = false, routes.GetNinoController.submit(service), service)
    )
  }

}
