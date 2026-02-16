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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import cats.data.EitherT
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector.{NoMatchFound, matchFailureResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.{RegisterWithId, RegisterWithIdConfirmation, RegisterWithIdSubmitted}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingServiceConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, audit: Auditor)(implicit
  ec: ExecutionContext
) extends HandleResponses {

  private val url = url"${appConfig.getServiceUrl("register-with-id")}"

  private def logIfNotContains(matchingResponse: MatchingResponse, message: String): Unit = {
    // $COVERAGE-OFF$Loggers
    if (!matchingResponse.registerWithIDResponse.responseCommon.statusText.contains(message)) {
      logger.warn(
        s"REG01 failed Lookup: responseCommon: ${matchingResponse.registerWithIDResponse.responseCommon}"
      )
    }
    // $COVERAGE-ON
  }

  def lookup(req: MatchingRequestHolder)(implicit hc: HeaderCarrier): EitherT[Future, ResponseError, MatchingResponse] =
    EitherT {

      httpClient
        .post(url)
        .withBody(Json.toJson(req))
        .setHeader(AUTHORIZATION -> appConfig.internalAuthToken)
        .execute
        .map { response =>
          response.status match {
            case OK =>
              handleResponse[MatchingResponse](response).flatMap { matchingResponse =>
                val details = Json.toJson(RegisterWithId(RegisterWithIdSubmitted(req), RegisterWithIdConfirmation(matchingResponse)))
                audit.sendRegistrationDataEvent(url.toString, details)
                val idResponse = matchingResponse.registerWithIDResponse

                if (idResponse.responseDetail.isEmpty) {
                  logIfNotContains(matchingResponse, NoMatchFound)
                  idResponse.responseCommon.statusText match {
                    case Some(text) if text.equalsIgnoreCase(NoMatchFound) => Left(matchFailureResponse)
                    case Some(text) => Left(ResponseError(OK, text))
                    case None => Left(ResponseError(OK, "Detail object not returned"))
                  }
                } else {
                  // $COVERAGE-OFF$Loggers
                  logger.debug(
                    s"REG01 Lookup: responseCommon: ${matchingResponse.registerWithIDResponse.responseCommon}"
                  )
                  // $COVERAGE-ON
                  Right(matchingResponse)
                }
              }

            case _ =>
              val error = s"REG01 Lookup failed with reason: ${response.body}"
              logger.error(error)
              Left(ResponseError(response.status, error))
          }

        }
    }
}

object MatchingServiceConnector {
  val NoMatchFound = "002 - No match found"
  val DownstreamFailure = "001 - Request could not be processed"
  val matchFailureResponse: ResponseError = ResponseError(OK, NoMatchFound)
  val downstreamFailureResponse: ResponseError = ResponseError(OK, DownstreamFailure)
  val otherErrorHappen: ResponseError = ResponseError(INTERNAL_SERVER_ERROR, "Unknown error occurred.")
}
