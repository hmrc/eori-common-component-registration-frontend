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


import cats.data.EitherT

import javax.inject.{Inject, Singleton}
import play.api.i18n.Messages
import play.api.mvc.{Action, _}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{MatchingServiceConnector, ResponseError}

import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.MatchingForms.subscriptionUtrForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.MatchingService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCacheService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation.OrgTypeLookup
import uk.gov.hmrc.eoricommoncomponent.frontend.viewModels.HowCanWeIdentifyYouUtrViewModel
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GYEHowCanWeIdentifyYouUtrController @Inject() (
  authAction: AuthAction,
  matchingService: MatchingService,
  mcc: MessagesControllerComponents,
  howCanWeIdentifyYouView: how_can_we_identify_you_utr,
  orgTypeLookup: OrgTypeLookup,
  sessionCacheService: SessionCacheService,
  errorView: error_template
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  def form(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      for {
        orgType <- orgTypeLookup.etmpOrgType
      } yield Ok(
        howCanWeIdentifyYouView(
          subscriptionUtrForm,
          isInReviewMode = false,
          routes.GYEHowCanWeIdentifyYouUtrController.submit(service),
          HowCanWeIdentifyYouUtrViewModel.forHintMessage(orgType),
          service = service
        )
      )

    }

  def submit(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => loggedInUser: LoggedInUserWithEnrolments =>
      orgTypeLookup.etmpOrgType.flatMap(
        orgType =>
          subscriptionUtrForm.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  howCanWeIdentifyYouView(
                    formWithErrors,
                    isInReviewMode = false,
                    routes.GYEHowCanWeIdentifyYouUtrController.submit(service),
                    HowCanWeIdentifyYouUtrViewModel.forHintMessage(orgType),
                    service = service
                  )
                )
              ),
            formData =>
              matchOnId(formData, GroupId(loggedInUser.groupId)).fold(
                {
                  case MatchingServiceConnector.matchFailureResponse =>
                    matchNotFoundBadRequest(formData, service, orgType)
                  case MatchingServiceConnector.downstreamFailureResponse => Ok(errorView(service))
                  case _                                                  => InternalServerError(errorView(service))
                },
                _ => Redirect(ConfirmContactDetailsController.form(service, isInReviewMode = false))
              )
          )
      )
    }

  private def matchOnId(formData: IdMatchModel, groupId: GroupId)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): EitherT[Future, ResponseError, Unit] = EitherT {
    sessionCacheService
      .retrieveNameDobFromCache()
      .flatMap(ind => matchingService.matchIndividualWithId(Utr(formData.id), ind, groupId).value)
  }

  private def matchNotFoundBadRequest(
    individualFormData: IdMatchModel,
    service: Service,
    etmpOrganisationType: EtmpOrganisationType
  )(implicit request: Request[AnyContent]): Result = {
    val errorForm = subscriptionUtrForm
      .withGlobalError(Messages("cds.matching-error.individual-not-found"))
      .fill(individualFormData)
    BadRequest(
      howCanWeIdentifyYouView(
        errorForm,
        isInReviewMode = false,
        routes.GYEHowCanWeIdentifyYouUtrController.submit(service),
        HowCanWeIdentifyYouUtrViewModel.forHintMessage(etmpOrganisationType),
        service = service
      )
    )
  }

}
