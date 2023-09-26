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
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{
  AuthAction,
  EnrolmentExtractor,
  GroupEnrolmentExtractor
}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.{
  EnrolmentAlreadyExistsController,
  YouAlreadyHaveEoriController
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, GroupId, InternalId, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.UserGroupIdSubscriptionStatusCheckService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailJourneyService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{
  enrolment_pending_against_group_id,
  enrolment_pending_for_user,
  error_template
}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailController @Inject() (
  authAction: AuthAction,
  sessionCache: SessionCache,
  mcc: MessagesControllerComponents,
  userGroupIdSubscriptionStatusCheckService: UserGroupIdSubscriptionStatusCheckService,
  groupEnrolment: GroupEnrolmentExtractor,
  appConfig: AppConfig,
  enrolmentPendingForUser: enrolment_pending_for_user,
  enrolmentPendingAgainstGroupId: enrolment_pending_against_group_id,
  emailJourneyService: EmailJourneyService,
  errorPage: error_template
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with EnrolmentExtractor {

  def form(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => implicit user: LoggedInUserWithEnrolments =>
      startRegisterJourney(service)
    }

  private def startRegisterJourney(
    service: Service
  )(implicit hc: HeaderCarrier, request: Request[AnyContent], user: LoggedInUserWithEnrolments): Future[Result] =
    groupEnrolment.groupIdEnrolments(user.groupId.getOrElse(throw MissingGroupId()))
      .foldF(
        _ => Future.successful(InternalServerError(errorPage(service))),
        enrolmentResponseList =>
          if (enrolmentResponseList.exists(_.service == service.enrolmentKey))
            if (service.code.equalsIgnoreCase(appConfig.standaloneServiceCode))
              existingEoriForUserOrGroup(user, enrolmentResponseList) match {
                case Some(eori) =>
                  sessionCache.saveEori(Eori(eori.id)).map(
                    _ => Redirect(EnrolmentAlreadyExistsController.enrolmentAlreadyExistsForGroupStandalone(service))
                  )
                case None =>
                  Future.successful(
                    Redirect(EnrolmentAlreadyExistsController.enrolmentAlreadyExistsForGroupStandalone(service))
                  )
              }
            else
              Future.successful(Redirect(EnrolmentAlreadyExistsController.enrolmentAlreadyExistsForGroup(service)))
          else
            existingEoriForUserOrGroup(user, enrolmentResponseList) match {
              case Some(eori) =>
                if (service.code.equalsIgnoreCase(appConfig.standaloneServiceCode))
                  sessionCache.saveEori(Eori(eori.id)).map(
                    _ => Redirect(YouAlreadyHaveEoriController.displayStandAlone(service))
                  )
                else
                  Future.successful(Redirect(YouAlreadyHaveEoriController.display(service)))
              case None =>
                userGroupIdSubscriptionStatusCheckService
                  .checksToProceed(GroupId(user.groupId), InternalId(user.internalId), service)(
                    emailJourneyService.continue(service)
                  )(Future.successful(Ok(enrolmentPendingForUser(service))))(
                    Future.successful(Ok(enrolmentPendingAgainstGroupId(service)))
                  )
            }
      )

}
