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

package uk.gov.hmrc.eoricommoncomponent.frontend.config

import play.api.{Configuration, Logging}
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Done
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Done]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() () extends InternalAuthTokenInitialiser with Logging {

  override val initialised: Future[Done] = {
    logger.info("Internal auth not active")
    Future.successful(Done)
  }

}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
  configuration: Configuration,
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends InternalAuthTokenInitialiser with Logging {

  private val internalAuthService =
    servicesConfig.baseUrl("internal-auth")

  private val authToken: String =
    configuration.get[String]("internal-auth.token")

  private val appName: String =
    configuration.get[String]("appName")

  override val initialised: Future[Done] =
    ensureAuthToken()

  Await.result(initialised, 30.seconds)

  private def ensureAuthToken(): Future[Done] =
    authTokenIsValid.flatMap { isValid =>
      if (isValid) {
        logger.info("Auth token is already valid")
        Future.successful(Done)
      } else
        createClientAuthToken()
    }

  private def createClientAuthToken(): Future[Done] = {
    logger.info("Initialising auth token")
    httpClient.post(url"$internalAuthService/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token"     -> authToken,
          "principal" -> appName,
          "permissions" -> Seq(
            Json.obj("resourceType" -> "eori-common-component", "resourceLocation" -> "*", "actions" -> List("WRITE")),
            Json.obj(
              "resourceType"     -> "eori-common-component-hods-proxy",
              "resourceLocation" -> "*",
              "actions"          -> List("WRITE")
            )
          )
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == 201) {
          logger.info("Auth token initialised")
          Future.successful(Done)
        } else
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
      }

  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("Checking auth token")
    httpClient.get(url"$internalAuthService/test-only/token")(HeaderCarrier())
      .setHeader(AUTHORIZATION -> authToken)
      .execute
      .map(_.status == 200)
  }

}
