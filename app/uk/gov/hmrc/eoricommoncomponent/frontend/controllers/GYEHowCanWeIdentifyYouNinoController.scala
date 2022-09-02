/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{Action, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Individual
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionNinoForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GYEHowCanWeIdentifyYouNinoController @Inject() (
  authAction: AuthAction,
  matchingService: MatchingService,
  mcc: MessagesControllerComponents,
  howCanWeIdentifyYouView: how_can_we_identify_you_nino,
  cdsFrontendDataCache: SessionCache
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      Future.successful(
        Ok(
          howCanWeIdentifyYouView(
            subscriptionNinoForm,
            isInReviewMode = false,
            routes.GYEHowCanWeIdentifyYouNinoController.submit(service)
          )
        )
      )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      subscriptionNinoForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              howCanWeIdentifyYouView(
                formWithErrors,
                isInReviewMode = false,
                routes.GYEHowCanWeIdentifyYouNinoController.submit(service)
              )
            )
          ),
        formData =>
          matchOnId(formData, GroupId(loggedInUser.groupId)).map {
            case true =>
              Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false))
            case false =>
              matchNotFoundBadRequest(formData, service)
          }
      )
    }

  private def matchOnId(formData: IdMatchModel, groupId: GroupId)(implicit hc: HeaderCarrier, request: Request[_]): Future[Boolean] =
    retrieveNameDobFromCache().flatMap(ind => matchingService.matchIndividualWithNino(formData.id, ind, groupId))

  private def matchNotFoundBadRequest(individualFormData: IdMatchModel, service: Service)(implicit
    request: Request[AnyContent]
  ): Result = {
    val errorForm =
      subscriptionNinoForm.withGlobalError(Messages("cds.matching-error.individual-not-found")).fill(individualFormData)

    BadRequest(
      howCanWeIdentifyYouView(
        errorForm,
        isInReviewMode = false,
        routes.GYEHowCanWeIdentifyYouNinoController.submit(service)
      )
    )
  }

  // TODO Get rid of `.get`. Now if there is no information Exception will be thrown, understand what must happen if this is not provided
  private def retrieveNameDobFromCache()(implicit hc: HeaderCarrier, request: Request[_]): Future[Individual] =
    cdsFrontendDataCache.subscriptionDetails.map(_.nameDobDetails.get).map { nameDobDetails =>
      Individual.withLocalDate(
        firstName = nameDobDetails.firstName,
        middleName = None,
        lastName = nameDobDetails.lastName,
        dateOfBirth = nameDobDetails.dateOfBirth
      )
    }

}
