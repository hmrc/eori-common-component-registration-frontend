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

package uk.gov.hmrc.eoricommoncomponent.frontend

import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc._
import play.mvc.Http.Status._
import play.twirl.api.Html
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.routes._
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionTimeOutException
import uk.gov.hmrc.eoricommoncomponent.frontend.util.{Constants, InvalidUrlValueException}
import uk.gov.hmrc.eoricommoncomponent.frontend.views.ServiceName._
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.{client_error_template, error_template, notFound}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import scala.concurrent.Future

class CdsErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  val configuration: Configuration,
  errorTemplateView: error_template,
  clientErrorTemplateView: client_error_template,
  notFoundView: notFound
) extends FrontendErrorHandler {

  private val logger = Logger(this.getClass)

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_]
  ): Html = throw new IllegalStateException("This method should not be used any more.")

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {

    logger.error(s"Error with status code: $statusCode and message: $message")
    implicit val req: Request[_] = Request(request, "")

    statusCode match {
      case NOT_FOUND                                              => Future.successful(Results.NotFound(notFoundView()))
      case BAD_REQUEST if message == Constants.INVALID_PATH_PARAM => Future.successful(Results.NotFound(notFoundView()))
      case _                                                      => Future.successful(Results.InternalServerError(clientErrorTemplateView(message)))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {

    implicit val req: Request[_] = Request(request, "")

    exception match {
      case sessionTimeOut: SessionTimeOutException =>
        logger.info("Session time out: " + sessionTimeOut.errorMessage, exception)
        Future.successful(Redirect(SecuritySignOutController.displayPage(service)).withNewSession)
      case invalidRequirement: InvalidUrlValueException =>
        logger.warn(invalidRequirement.message)
        Future.successful(Results.NotFound(notFoundView()))
      case _ =>
        logger.error("Internal server error: " + exception.getMessage, exception)
        Future.successful(Results.InternalServerError(errorTemplateView()))
    }
  }

}
