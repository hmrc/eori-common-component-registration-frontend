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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, LoggedInUserWithEnrolments, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.confirmEmailYesNoAnswerForm
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.eoricommoncomponent.frontend.services.email.EmailVerificationService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email.{check_your_email, email_confirmed, verify_your_email}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourEmailController @Inject() (
  authAction: AuthAction,
  save4LaterService: Save4LaterService,
  cdsFrontendDataCache: SessionCache,
  mcc: MessagesControllerComponents,
  checkYourEmailView: check_your_email,
  emailConfirmedView: email_confirmed,
  verifyYourEmail: verify_your_email,
  emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private val logger = Logger(this.getClass)

  private def populateView(email: Option[String], isInReviewMode: Boolean, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    Future.successful(Ok(checkYourEmailView(email, confirmEmailYesNoAnswerForm(), isInReviewMode, service)))

  private def populateEmailVerificationView(email: Option[String], service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    Future.successful(Ok(verifyYourEmail(email, service)))

  def createForm(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap {
          _.fold {
            // $COVERAGE-OFF$Loggers
            logger.warn(s"[CheckYourEmailController][createForm] -   emailStatus cache none")
            // $COVERAGE-ON
            populateView(None, isInReviewMode = false, service)
          } { emailStatus =>
            populateView(emailStatus.email, isInReviewMode = false, service)
          }
        }
    }

  def submit(isInReviewMode: Boolean, service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        confirmEmailYesNoAnswerForm()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              save4LaterService
                .fetchEmail(GroupId(userWithEnrolments.groupId))
                .flatMap {
                  _.fold {
                    // $COVERAGE-OFF$Loggers
                    logger.warn(s"[CheckYourEmailController][submit] -   emailStatus cache none")
                    // $COVERAGE-ON
                    Future(
                      BadRequest(
                        checkYourEmailView(None, formWithErrors, isInReviewMode = isInReviewMode, service = service)
                      )
                    )
                  } { emailStatus =>
                    Future(
                      BadRequest(
                        checkYourEmailView(
                          emailStatus.email,
                          formWithErrors,
                          isInReviewMode = isInReviewMode,
                          service = service
                        )
                      )
                    )
                  }
                },
            yesNoAnswer => locationByAnswer(GroupId(userWithEnrolments.groupId), yesNoAnswer, service)
          )
    }

  def verifyEmailView(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap { emailStatus =>
          emailStatus.fold {
            // $COVERAGE-OFF$Loggers
            logger.warn(s"[CheckYourEmailController][verifyEmailView] -  emailStatus cache none")
            // $COVERAGE-ON
            populateEmailVerificationView(None, service)
          } { email =>
            populateEmailVerificationView(email.email, service)
          }
        }
    }

  def emailConfirmed(service: Service): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap { emailStatus =>
          emailStatus.fold {
            // $COVERAGE-OFF$Loggers
            logger.warn(s"[CheckYourEmailController][emailConfirmed] -  emailStatus cache none")
            // $COVERAGE-ON
            Future.successful(Redirect(SecuritySignOutController.signOut(service)))
          } { email =>
            if (email.isConfirmed.getOrElse(false))
              Future.successful(Redirect(MatchingIdController.matchWithIdOnly(service)))
            else
              save4LaterService
                .saveEmail(GroupId(userWithEnrolments.groupId), email.copy(isConfirmed = Some(true)))
                .map { _ =>
                  Ok(emailConfirmedView())
                }

          }
        }

    }

  def emailConfirmedContinue(service: Service): Action[AnyContent] =
    Action(_ => Redirect(MatchingIdController.matchWithIdOnly(service)))

  private def submitNewDetails(groupId: GroupId, service: Service)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[Result] =
    save4LaterService.fetchEmail(groupId) flatMap {
      _.fold {
        // $COVERAGE-OFF$Loggers
        logger.warn(s"[CheckYourEmailController][submitNewDetails] -  emailStatus cache none")
        // $COVERAGE-ON
        throw new IllegalStateException(s"[CheckYourEmailController][submitNewDetails] - emailStatus cache none")
      } { emailStatus =>
        val email: String = emailStatus.email.getOrElse(
          throw new IllegalStateException(s"[CheckYourEmailController][submitNewDetails] - emailStatus.email none")
        )
        emailVerificationService.createEmailVerificationRequest(email, EmailController.form(service).url) flatMap {
          case Some(true) =>
            Future.successful(Redirect(CheckYourEmailController.verifyEmailView(service)))
          case Some(false) =>
            // $COVERAGE-OFF$Loggers
            logger.warn(
              s"[CheckYourEmailController][sendVerification] - " +
                "Unable to send email verification request. Service responded with 'already verified'"
            )
            // $COVERAGE-ON
            save4LaterService
              .saveEmail(groupId, emailStatus.copy(isVerified = true))
              .flatMap { _ =>
                cdsFrontendDataCache.saveEmail(email).map { _ =>
                  Redirect(EmailController.form(service))
                }
              }
          case _ =>
            throw new IllegalStateException("CreateEmailVerificationRequest Failed")
        }
      }
    }

  private def locationByAnswer(groupId: GroupId, yesNoAnswer: YesNo, service: Service)(implicit
    request: Request[AnyContent]
  ): Future[Result] = yesNoAnswer match {
    case theAnswer if theAnswer.isYes =>
      submitNewDetails(groupId, service)
    case _ => Future(Redirect(WhatIsYourEmailController.createForm(service)))
  }

}
