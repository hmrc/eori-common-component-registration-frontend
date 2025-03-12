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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.email

import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.{routes => emailRoutes}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.email.{EmailVerificationStatus, ResponseWithURI}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.error_template
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailJourneyService @Inject() (
  emailVerificationService: EmailVerificationService,
  sessionCache: SessionCache,
  save4LaterService: Save4LaterService,
  errorPage: error_template,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def continue(service: Service)(implicit
    request: Request[AnyContent],
    user: LoggedInUserWithEnrolments,
    messages: Messages,
    hc: HeaderCarrier
  ): Future[Result] =
    save4LaterService.fetchEmail(GroupId(user.groupId)) flatMap {
      _.fold {
        // $COVERAGE-OFF$Loggers
        logger.info(s"emailStatus cache none ${user.internalId}")
        Future.successful(Redirect(emailRoutes.WhatIsYourEmailController.createForm(service)))
      } { cachedEmailStatus =>
        cachedEmailStatus.email match {
          case Some(email) =>
            if (cachedEmailStatus.isVerified)
              sessionCache.saveEmail(email) map { _ =>
                Redirect(routes.UserLocationController.form(service))
              }
            else
              checkWithEmailService(email, cachedEmailStatus, user.credId, service)
          case None =>
            Future.successful(Redirect(emailRoutes.WhatIsYourEmailController.createForm(service)))
        }
      }
    }

  private def checkWithEmailService(email: String, emailStatus: EmailStatus, credId: String, service: Service)(implicit
    request: Request[AnyContent],
    userWithEnrolments: LoggedInUserWithEnrolments,
    messages: Messages,
    hc: HeaderCarrier
  ): Future[Result] =
    emailVerificationService.getVerificationStatus(email, credId).foldF(
      (_ => Future.successful(InternalServerError(errorPage(service)))),
      {
        case EmailVerificationStatus.Verified =>
          onVerifiedEmail(service, email, emailStatus, GroupId(userWithEnrolments.groupId))
        case EmailVerificationStatus.Unverified =>
          // $COVERAGE-OFF$Loggers
          logger.info("Email address was not verified")
          // $COVERAGE-ON
          submitNewDetails(email, service, credId)
        case EmailVerificationStatus.Locked =>
          // $COVERAGE-OFF$Loggers
          logger.warn("Email address is locked")
          // $COVERAGE-ON
          Future.successful(Redirect(emailRoutes.LockedEmailController.onPageLoad(service)))
      }
    )

  private def onVerifiedEmail(service: Service, email: String, emailStatus: EmailStatus, groupId: GroupId)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier
  ) =
    for {
      _ <- save4LaterService.saveEmail(groupId, emailStatus.copy(isVerified = true))
      _ <- sessionCache.saveEmail(email)
    } yield Redirect(emailRoutes.CheckYourEmailController.emailConfirmed(service))

  private def submitNewDetails(email: String, service: Service, credId: String)(implicit
    request: Request[AnyContent],
    messages: Messages,
    hc: HeaderCarrier
  ): Future[Result] =
    emailVerificationService.startVerificationJourney(credId, service, email).fold(
      _ => InternalServerError(errorPage(service)),
      { responseWithUri: ResponseWithURI =>
        Redirect(s"${appConfig.emailVerificationFrontendBaseUrl}${responseWithUri.redirectUri}")
      }
    )

}
