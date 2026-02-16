/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend

import play.api.i18n.MessagesApi
import play.api.mvc.*
import play.api.mvc.Results.*
import play.api.{Configuration, Logging}
import play.mvc.Http.Status.*
import play.twirl.api.Html
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes.*
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{DataUnavailableException, SessionTimeOutException}
import uk.gov.hmrc.eoricommoncomponent.frontend.util.{Constants, InvalidUrlValueException}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName.*
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{error_template, notFound}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CdsErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  val configuration: Configuration,
  errorTemplateView: error_template,
  notFoundView: notFound
)(implicit val ec: ExecutionContext)
    extends FrontendErrorHandler
    with Logging {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: RequestHeader
  ): Future[Html] = throw new IllegalStateException("This method must not be used any more.")

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    // $COVERAGE-OFF$Loggers
    logger.error(s"Error with status code: $statusCode and message: $message")
    // $COVERAGE-ON
    implicit val req: Request[_] = Request(request, "")

    statusCode match {
      case NOT_FOUND => Future.successful(Results.NotFound(notFoundView(service)))
      case BAD_REQUEST if message == Constants.INVALID_PATH_PARAM =>
        Future.successful(Results.NotFound(notFoundView(service)))
      case FORBIDDEN if message.contains(Constants.NO_CSRF_FOUND) =>
        Future.successful(Redirect(SecuritySignOutController.displayPage(service)).withNewSession)
      case _ => Future.successful(Results.InternalServerError(errorTemplateView(service)))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {

    implicit val req: Request[_] = Request(request, "")

    exception match {
      case sessionTimeOut: SessionTimeOutException =>
        // $COVERAGE-OFF$Loggers
        logger.info("Session time out: " + sessionTimeOut.errorMessage, exception)
        // $COVERAGE-ON
        Future.successful(Redirect(SecuritySignOutController.displayPage(service)).withNewSession)

      case invalidRequirement: InvalidUrlValueException =>
        // $COVERAGE-OFF$Loggers
        logger.warn(invalidRequirement.message)
        // $COVERAGE-ON
        Future.successful(Results.NotFound(notFoundView(service)))
      case dataUnavailableException: DataUnavailableException =>
        // $COVERAGE-OFF$Loggers
        logger.warn(
          s"DataUnavailableException - ${dataUnavailableException.message} - user is redirected to start page"
        )
        // $COVERAGE-ON
        Future.successful(Redirect(ApplicationController.startRegister(service)))
      case _ =>
        // $COVERAGE-OFF$Loggers
        logger.error("Internal server error: " + exception.getMessage, exception)
        // $COVERAGE-ON
        Future.successful(Results.InternalServerError(errorTemplateView(service)))
    }
  }

}
