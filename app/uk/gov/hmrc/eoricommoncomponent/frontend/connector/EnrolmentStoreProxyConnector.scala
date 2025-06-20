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

import cats.data.EitherT
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditor
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, EnrolmentStoreProxyResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.EnrolmentStoreProxyEvent
import uk.gov.hmrc.eoricommoncomponent.frontend.util.HttpStatusCheck
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyConnector @Inject() (http: HttpClientV2, appConfig: AppConfig, audit: Auditor)(implicit
  ec: ExecutionContext
) extends HandleResponses {

  private val baseUrl = appConfig.enrolmentStoreProxyBaseUrl
  private val serviceContext = appConfig.enrolmentStoreProxyServiceContext

  def getEnrolmentByGroupId(groupId: String)(implicit
    hc: HeaderCarrier,
    reads: Reads[EnrolmentStoreProxyResponse]
  ): EitherT[Future, ResponseError, EnrolmentStoreProxyResponse] = EitherT {
    val url =
      s"$baseUrl/$serviceContext/enrolment-store/groups/$groupId/enrolments?type=principal"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"GetEnrolmentByGroupId: $url and hc: $hc")
    // $COVERAGE-ON

    http
      .get(new URI(url).toURL)
      .execute[HttpResponse]
      .map { response =>
        logResponse(response)

        val parsedResponse = response.status match {
          case OK => handleResponse[EnrolmentStoreProxyResponse](response)
          case NO_CONTENT =>
            Right(EnrolmentStoreProxyResponse(enrolments = List.empty[EnrolmentResponse]))
          case status =>
            Left(ResponseError(status, s"Enrolment Store Proxy Response : ${response.body}}"))
        }

        val auditDetails =
          parsedResponse
            .fold(
              Json.toJson(_),
              proxyResponse => Json.toJson(EnrolmentStoreProxyEvent(groupId = groupId, enrolments = proxyResponse.enrolments))
            )
        audit.sendEnrolmentStoreCallEvent(url, auditDetails)

        parsedResponse
      } recover { case e: Throwable =>
      val error = s"enrolment-store-proxy failed. url: $url, error: $e"
      logger.error(error, e)
      Left(ResponseError(INTERNAL_SERVER_ERROR, error))
    }
  }

  // $COVERAGE-OFF$Loggers
  private def logResponse(response: HttpResponse): Unit =
    if (HttpStatusCheck.is2xx(response.status))
      logger.debug("GetEnrolmentByGroupId request is successful")
    else
      logger.warn(s"GetEnrolmentByGroupId request is failed with response $response")

  // $COVERAGE-ON
}
