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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.ConfirmContactDetailsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.registration.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.SubscriptionDetailsService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.migration.how_can_we_identify_you_nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetNinoController @Inject() (
  authAction: AuthAction,
  matchingService: MatchingService,
  mcc: MessagesControllerComponents,
  matchNinoRowIndividualView: how_can_we_identify_you_nino,
  subscriptionDetailsService: SubscriptionDetailsService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def displayForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(
        Ok(matchNinoRowIndividualView(subscriptionNinoForm, false, routes.GetNinoController.submit(service)))
      )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => loggedInUser: LoggedInUserWithEnrolments =>
        subscriptionNinoForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(
              BadRequest(matchNinoRowIndividualView(formWithErrors, false, routes.GetNinoController.submit(service)))
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
            Individual.withLocalDate(details.firstName, details.middleName, details.lastName, details.dateOfBirth),
            groupId
          )
          .map { matched =>
            if (matched)
              Redirect(ConfirmContactDetailsController.form(service))
            else
              matchNotFoundBadRequest(formData, service)
          }
      case None => Future.successful(matchNotFoundBadRequest(formData, service))
    }

  private def matchNotFoundBadRequest(formData: IdMatchModel, service: Service)(implicit
    request: Request[AnyContent]
  ): Result = {
    val errorMsg  = Messages("cds.matching-error.individual-not-found")
    val errorForm = subscriptionNinoForm.withGlobalError(errorMsg).fill(formData)
    BadRequest(matchNinoRowIndividualView(errorForm, false, routes.GetNinoController.submit(service)))
  }

}
