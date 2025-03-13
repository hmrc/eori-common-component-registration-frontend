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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import play.api.Logger
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers._
import uk.gov.hmrc.http.HttpReads.Implicits.readFromJson
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdateVerifiedEmailConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) {

  private val url = url"${appConfig.getServiceUrl("update-verified-email")}"
  private val logger = Logger(this.getClass)

  def updateVerifiedEmail(
    request: VerifiedEmailRequest
  )(implicit hc: HeaderCarrier): Future[Either[HttpErrorResponse, VerifiedEmailResponse]] =
    httpClient
      .put(url)
      .withBody(Json.toJson(request))
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute[VerifiedEmailResponse] map { resp =>
      Right(resp)
    } recover {
      case _: BadRequestException | UpstreamErrorResponse(_, BAD_REQUEST, _, _) => Left(BadRequest)
      case _: ForbiddenException | UpstreamErrorResponse(_, FORBIDDEN, _, _) => Left(Forbidden)
      case _: InternalServerException | UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _) =>
        Left(ServiceUnavailable)
      case NonFatal(e) =>
        logger.error(
          s"[UpdateVerifiedEmailConnector][updateVerifiedEmail] update-verified-email. url: $url, error: ${e.getMessage}"
        )
        Left(UnhandledException)
    }

}
