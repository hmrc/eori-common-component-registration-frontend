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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import play.api.Logger
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json._
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadRequestException, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class Save4LaterConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // $COVERAGE-OFF$Loggers
  private def logSuccess(method: String, url: String) =
    logger.debug(s"$method complete for call to $url")

  private def logFailure(method: String, url: String, e: Throwable) =
    logger.warn(s"$method request failed for call to $url: ${e.getMessage}", e)

  // $COVERAGE-ON

  def get[T](id: String, key: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[Option[T]] = {
    val url = url"${appConfig.handleSubscriptionBaseUrl}/save4later/$id/$key"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"GET: $url")
    // $COVERAGE-ON

    httpClient
      .get(url)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute map { response =>
      logSuccess("Get", url.toString)

      response.status match {
        case OK =>
          Some(response.json.as[T])
        case NOT_FOUND =>
          None
        case _ => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case NonFatal(e) =>
        logFailure("Get", url.toString, e)
        Future.failed(e)
    }
  }

  def put[T](id: String, key: String, payload: JsValue)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = url"${appConfig.handleSubscriptionBaseUrl}/save4later/$id/$key"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"PUT: $url")
    // $COVERAGE-ON

    httpClient
      .put(url)
      .withBody(payload)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute map { response =>
      logSuccess("Put", url.toString)
      response.status match {
        case NO_CONTENT | CREATED | OK => ()
        case _                         => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case NonFatal(e) =>
        logFailure("Put", url.toString, e)
        Future.failed(e)
    }
  }

  def delete[T](id: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = url"${appConfig.handleSubscriptionBaseUrl}/save4later/$id"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"DELETE: $url")
    // $COVERAGE-ON

    httpClient
      .delete(url)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute map { response =>
      logSuccess("Delete", url.toString)
      response.status match {
        case NO_CONTENT => ()
        case _          => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case NonFatal(e) =>
        logFailure("Delete", url.toString, e)
        Future.failed(e)
    }
  }

  def deleteKey[T](id: String, key: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = url"${appConfig.handleSubscriptionBaseUrl}/save4later/$id/$key"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"DELETE Key: $url")
    // $COVERAGE-ON

    httpClient
      .delete(url)
      .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
      .execute map { response =>
      logSuccess("Delete key", url.toString)
      response.status match {
        case NO_CONTENT => ()
        case _          => throw new BadRequestException(s"Status:${response.status}")
      }
    } recoverWith {
      case NonFatal(e) =>
        logFailure("Delete key", url.toString, e)
        Future.failed(e)
    }
  }

}
