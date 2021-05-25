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
import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.{
  AuthAction,
  EnrolmentExtractor,
  GroupEnrolmentExtractor
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, ExistingEori, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.EnrolmentStoreProxyService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{start, start_subscribe}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject() (
  authorise: AuthAction,
  mcc: MessagesControllerComponents,
  viewStartSubscribe: start_subscribe,
  viewStartRegister: start,
  cache: SessionCache,
  groupEnrolment: GroupEnrolmentExtractor,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  appConfig: AppConfig
)(implicit override val messagesApi: MessagesApi, ec: ExecutionContext)
    extends CdsController(mcc) with EnrolmentExtractor {

  def startRegister(service: Service): Action[AnyContent] = Action { implicit request =>
    Ok(viewStartRegister(service, Journey.Register))
  }

  def startSubscription(service: Service): Action[AnyContent] = authorise.ggAuthorisedUserWithEnrolmentsAction {
    implicit request => implicit loggedInUser: LoggedInUserWithEnrolments =>
      val groupId = loggedInUser.groupId.getOrElse(throw MissingGroupId())
      groupEnrolment.hasGroupIdEnrolmentTo(groupId, service).flatMap { groupIdEnrolmentExists =>
        if (groupIdEnrolmentExists)
          Future.successful(
            Redirect(routes.EnrolmentAlreadyExistsController.enrolmentAlreadyExistsForGroup(service, Journey.Subscribe))
          )
        else
          cdsEnrolmentCheck(loggedInUser, groupId, service)
      }
  }

  private def isEnrolmentInUse(service: Service, loggedInUser: LoggedInUserWithEnrolments)(implicit
    hc: HeaderCarrier
  ): Future[Option[ExistingEori]] = {
    val groupId = loggedInUser.groupId.getOrElse(throw MissingGroupId())

    groupEnrolment.groupIdEnrolments(groupId).flatMap { groupEnrolments =>
      existingEoriForUserOrGroup(loggedInUser, groupEnrolments) match {
        case Some(existingEori) => enrolmentStoreProxyService.isEnrolmentInUse(service, existingEori)
        case _                  => Future.successful(None)
      }
    }
  }

  private def isUserEnrolledFor(loggedInUser: LoggedInUserWithEnrolments, service: Service): Boolean =
    enrolledForService(loggedInUser, service).isDefined

  private def cdsEnrolmentCheck(loggedInUser: LoggedInUserWithEnrolments, groupId: String, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Result] =
    isEnrolmentInUse(service, loggedInUser).flatMap {
      eoriFromUsedEnrolmentOpt =>
        if (eoriFromUsedEnrolmentOpt.isEmpty)
          if (isUserEnrolledFor(loggedInUser, Service.cds))
            Future.successful(Redirect(routes.HasExistingEoriController.displayPage(service)))
          else
            groupEnrolment.groupIdEnrolmentTo(groupId, Service.cds).flatMap {
              case Some(groupEnrolment) if groupEnrolment.eori.isDefined =>
                cache.saveGroupEnrolment(groupEnrolment).map { _ =>
                  Redirect(routes.HasExistingEoriController.displayPage(service)) // AutoEnrolment
                }
              case _ =>
                Future.successful(Ok(viewStartSubscribe(service))) // Display information page
            }
        else
          cache.saveEori(Eori(eoriFromUsedEnrolmentOpt.get.id)).map { _ =>
            Redirect(routes.YouCannotUseServiceController.unableToUseIdPage(service))
          }
    }

  def logout(service: Service, journey: Journey.Value): Action[AnyContent] =
    authorise.ggAuthorisedUserAction {
      implicit request => _: LoggedInUserWithEnrolments =>
        cache.remove map { _ =>
          Redirect(appConfig.feedbackUrl(service, journey)).withNewSession
        }
    }

  def keepAlive(service: Service, journey: Journey.Value): Action[AnyContent] = Action.async { implicit request =>
    cache.keepAlive map { _ =>
      Ok("Ok")
    }
  }

}

case class MissingGroupId() extends Exception(s"User doesn't have groupId")
