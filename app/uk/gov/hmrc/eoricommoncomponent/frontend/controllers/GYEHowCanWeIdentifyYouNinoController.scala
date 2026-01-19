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

import cats.data.EitherT
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{MatchingServiceConnector, ResponseError}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.MatchingResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.isRow
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.SubscriptionNinoFormProvider
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache, SessionCacheService}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GYEHowCanWeIdentifyYouNinoController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  sessionCache: SessionCache,
  howCanWeIdentifyYouView: how_can_we_identify_you_nino,
  requestSessionData: RequestSessionData,
  matchingService: MatchingService,
  sessionCacheService: SessionCacheService,
  subscriptionNinoFormProvider: SubscriptionNinoFormProvider,
  errorView: error_template
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  val form: Form[IdMatchModel] = subscriptionNinoFormProvider.subscriptionNinoForm

  def form(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => _: LoggedInUserWithEnrolments =>
      if (requestSessionData.selectedUserLocation.exists(isRow) && requestSessionData.isIndividualOrSoleTrader)
        Future.successful(Redirect(IndStCannotRegisterUsingThisServiceController.form(service)))
      else
        Future.successful(
          Ok(
            howCanWeIdentifyYouView(
              form,
              isInReviewMode = false,
              routes.GYEHowCanWeIdentifyYouNinoController.submit(service),
              service = service
            )
          )
        )
    }

  def submit(service: Service): Action[AnyContent] =
    authAction.enrolledUserWithSessionAction(service) { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                howCanWeIdentifyYouView(
                  formWithErrors,
                  isInReviewMode = false,
                  routes.GYEHowCanWeIdentifyYouNinoController.submit(service),
                  service = service
                )
              )
            ),
          ninoForm => {
            sessionCache
              .saveNinoOrUtrDetails(NinoOrUtr(Some(Nino(ninoForm.id))))
              .flatMap { saved =>
                matchOnId(ninoForm, GroupId(loggedInUser.groupId.getOrElse(throw new Exception("GroupId does not exists"))))
                  .fold(
                    {
                      case MatchingServiceConnector.matchFailureResponse => matchNotFoundBadRequest(ninoForm, service)
                      case MatchingServiceConnector.downstreamFailureResponse => Ok(errorView(service))
                      case _ => InternalServerError(errorView(service))
                    },
                    matchingResponse => Redirect(PostCodeController.createForm(service))
                  )
              }
          }
        )
    }

  private def matchOnId(formData: IdMatchModel, groupId: GroupId)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): EitherT[Future, ResponseError, MatchingResponse] = {

    EitherT {
      sessionCacheService
        .retrieveNameDobFromCache()
        .flatMap(ind => matchingService.matchIndividualWithNino(formData.id, ind, groupId).value)
    }
  }

  private def matchNotFoundBadRequest(individualFormData: IdMatchModel, service: Service)(implicit request: Request[AnyContent]): Result = {
    val errorForm = form.withGlobalError(Messages("cds.matching-error.individual-not-found")).fill(individualFormData)

    BadRequest(
      howCanWeIdentifyYouView(
        errorForm,
        isInReviewMode = false,
        routes.GYEHowCanWeIdentifyYouNinoController.submit(service),
        service = service
      )
    )
  }

}
