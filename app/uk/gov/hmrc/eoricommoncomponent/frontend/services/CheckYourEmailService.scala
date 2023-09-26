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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.Logging
import play.api.data.Form
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.routes.WhatIsYourEmailController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.SecuritySignOutController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.confirmEmailYesNoAnswerForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailJourneyService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.{check_your_email, email_confirmed, verify_your_email}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourEmailService @Inject() (
  save4LaterService: Save4LaterService,
  mcc: MessagesControllerComponents,
  checkYourEmailView: check_your_email,
  verifyYourEmail: verify_your_email,
  emailConfirmedView: email_confirmed,
  emailJourneyService: EmailJourneyService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) with Logging {

  def fetchEmailAndPopulateView(
    userWithEnrolments: LoggedInUserWithEnrolments,
    service: Service,
    emailVerificationView: Boolean = false
  )(implicit request: Request[AnyContent]): Future[Result] =
    save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap {
      _.fold {
        if (emailVerificationView) {
          // $COVERAGE-OFF$Loggers
          logger.warn(s"[CheckYourEmailService][verifyEmailView] -   emailStatus cache none")
          // $COVERAGE-ON
          Future.successful(Ok(verifyYourEmail(None, service)))
        } else {
          // $COVERAGE-OFF$Loggers
          logger.warn(s"[CheckYourEmailService][createForm] -   emailStatus cache none")
          // $COVERAGE-ON
          Future.successful(
            Ok(checkYourEmailView(None, confirmEmailYesNoAnswerForm(), isInReviewMode = false, service))
          )
        }
      } { emailStatus =>
        if (emailVerificationView)
          Future.successful(Ok(verifyYourEmail(emailStatus.email, service)))
        else
          Future.successful(
            Ok(checkYourEmailView(emailStatus.email, confirmEmailYesNoAnswerForm(), isInReviewMode = false, service))
          )
      }
    }

  def emailConfirmed(userWithEnrolments: LoggedInUserWithEnrolments, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap { emailStatus =>
      emailStatus.fold {
        // $COVERAGE-OFF$Loggers
        logger.warn(s"[CheckYourEmailService][emailConfirmed] -  emailStatus cache none")
        // $COVERAGE-ON
        Future.successful(Redirect(SecuritySignOutController.signOut(service)))
      } { email =>
        if (email.isConfirmed.getOrElse(false))
          Future.successful(toResult(service))
        else
          save4LaterService
            .saveEmail(GroupId(userWithEnrolments.groupId), email.copy(isConfirmed = Some(true)))
            .map { _ =>
              Ok(emailConfirmedView(service))
            }

      }
    }

  def toResult(service: Service)(implicit r: Request[AnyContent]): Result =
    Ok(emailConfirmedView(service))

  def handleFormWithErrors(
    userWithEnrolments: LoggedInUserWithEnrolments,
    formWithErrors: Form[YesNo],
    isInReviewMode: Boolean,
    service: Service
  )(implicit request: Request[AnyContent]): Future[Result] =
    save4LaterService
      .fetchEmail(GroupId(userWithEnrolments.groupId))
      .flatMap {
        _.fold {
          // $COVERAGE-OFF$Loggers
          logger.warn(s"[CheckYourEmailService][handleFormWithErrors] -   emailStatus cache none")
          // $COVERAGE-ON
          Future(
            BadRequest(checkYourEmailView(None, formWithErrors, isInReviewMode = isInReviewMode, service = service))
          )
        } { emailStatus =>
          Future(
            BadRequest(
              checkYourEmailView(emailStatus.email, formWithErrors, isInReviewMode = isInReviewMode, service = service)
            )
          )
        }
      }

  def locationByAnswer(yesNoAnswer: YesNo, service: Service)(implicit
    request: Request[AnyContent],
    userWithEnrolments: LoggedInUserWithEnrolments
  ): Future[Result] = yesNoAnswer match {
    case theAnswer if theAnswer.isYes => emailJourneyService.continue(service)
    case _                            => Future(Redirect(WhatIsYourEmailController.createForm(service)))
  }

}
