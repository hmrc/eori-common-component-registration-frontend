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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.registration.routes.MatchingIdController
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, InternalId, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.{confirmEmailYesNoAnswerForm, YesNo}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
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

  private def populateView(email: Option[String], isInReviewMode: Boolean, service: Service, journey: Journey.Value)(
    implicit request: Request[AnyContent]
  ): Future[Result] =
    Future.successful(Ok(checkYourEmailView(email, confirmEmailYesNoAnswerForm, isInReviewMode, service, journey)))

  private def populateEmailVerificationView(email: Option[String], service: Service, journey: Journey.Value)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    Future.successful(Ok(verifyYourEmail(email, service, journey)))

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap {
          _.fold {
            logger.warn("[CheckYourEmailController][createForm] -   emailStatus cache none")
            populateView(None, isInReviewMode = false, service, journey)
          } { emailStatus =>
            populateView(emailStatus.email, isInReviewMode = false, service, journey)
          }
        }
    }

  def submit(isInReviewMode: Boolean, service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        confirmEmailYesNoAnswerForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              save4LaterService
                .fetchEmail(GroupId(userWithEnrolments.groupId))
                .flatMap {
                  _.fold {
                    logger.warn("[CheckYourEmailController][submit] -   emailStatus cache none")
                    Future(
                      BadRequest(
                        checkYourEmailView(
                          None,
                          formWithErrors,
                          isInReviewMode = isInReviewMode,
                          service = service,
                          journey = journey
                        )
                      )
                    )
                  } { emailStatus =>
                    Future(
                      BadRequest(
                        checkYourEmailView(
                          emailStatus.email,
                          formWithErrors,
                          isInReviewMode = isInReviewMode,
                          service = service,
                          journey = journey
                        )
                      )
                    )
                  }
                },
            yesNoAnswer => locationByAnswer(GroupId(userWithEnrolments.groupId), yesNoAnswer, service, journey)
          )
    }

  def verifyEmailView(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap { emailStatus =>
          emailStatus.fold {
            logger.warn("[CheckYourEmailController][verifyEmailView] -  emailStatus cache none")
            populateEmailVerificationView(None, service, journey)
          } { email =>
            populateEmailVerificationView(email.email, service, journey)
          }
        }
    }

  def emailConfirmed(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        save4LaterService.fetchEmail(GroupId(userWithEnrolments.groupId)) flatMap { emailStatus =>
          emailStatus.fold {
            logger.warn("[CheckYourEmailController][emailConfirmed] -  emailStatus cache none")
            Future.successful(Redirect(SecuritySignOutController.signOut(service, journey)))
          } { email =>
            if (email.isConfirmed.getOrElse(false))
              Future.successful(toResult(service, journey))
            else
              save4LaterService
                .saveEmail(GroupId(userWithEnrolments.groupId), email.copy(isConfirmed = Some(true)))
                .map { _ =>
                  Ok(emailConfirmedView(journey))
                }

          }
        }

    }

  def emailConfirmedContinue(service: Service, journey: Journey.Value): Action[AnyContent] =
    Action(_ => toResult(service, journey))

  def toResult(service: Service, journey: Journey.Value): Result =
    journey match {
      case Journey.Register =>
        Redirect(MatchingIdController.matchWithIdOnly(service))
      case Journey.Subscribe =>
        Redirect(MatchingIdController.matchWithIdOnlyForExistingReg(service))
    }

  private def submitNewDetails(groupId: GroupId, service: Service, journey: Journey.Value)(implicit
    hc: HeaderCarrier
  ): Future[Result] =
    save4LaterService.fetchEmail(groupId) flatMap {
      _.fold {
        logger.warn("[CheckYourEmailController][submitNewDetails] -  emailStatus cache none")
        throw new IllegalStateException("[CheckYourEmailController][submitNewDetails] - emailStatus cache none")
      } { emailStatus =>
        val email: String = emailStatus.email.getOrElse(
          throw new IllegalStateException("[CheckYourEmailController][submitNewDetails] - emailStatus.email none")
        )
        emailVerificationService.createEmailVerificationRequest(
          email,
          EmailController.form(service, journey).url
        ) flatMap {
          case Some(true) =>
            Future.successful(Redirect(CheckYourEmailController.verifyEmailView(service, journey)))
          case Some(false) =>
            logger.warn(
              "[CheckYourEmailController][sendVerification] - " +
                "Unable to send email verification request. Service responded with 'already verified'"
            )
            save4LaterService
              .saveEmail(groupId, emailStatus.copy(isVerified = true))
              .flatMap { _ =>
                cdsFrontendDataCache.saveEmail(email).map { _ =>
                  Redirect(EmailController.form(service, journey))
                }
              }
          case _ =>
            throw new IllegalStateException("CreateEmailVerificationRequest Failed")
        }
      }
    }

  private def locationByAnswer(groupId: GroupId, yesNoAnswer: YesNo, service: Service, journey: Journey.Value)(implicit
    request: Request[AnyContent]
  ): Future[Result] = yesNoAnswer match {
    case theAnswer if theAnswer.isYes =>
      submitNewDetails(groupId, service, journey)
    case _ => Future(Redirect(WhatIsYourEmailController.createForm(service, journey)))
  }

}
