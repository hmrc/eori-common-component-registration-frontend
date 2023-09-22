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
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, EnrolmentStoreProxyResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.EnrolmentStoreProxyEvent
import uk.gov.hmrc.eoricommoncomponent.frontend.util.HttpStatusCheck
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreProxyConnector @Inject() (http: HttpClient, appConfig: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  private val baseUrl        = appConfig.enrolmentStoreProxyBaseUrl
  private val serviceContext = appConfig.enrolmentStoreProxyServiceContext

  def getEnrolmentByGroupId(
    groupId: String
  )(implicit hc: HeaderCarrier, reads: Reads[EnrolmentStoreProxyResponse]): Future[EnrolmentStoreProxyResponse] = {
    val url =
      s"$baseUrl/$serviceContext/enrolment-store/groups/$groupId/enrolments?type=principal"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"GetEnrolmentByGroupId: $url and hc: $hc")
    // $COVERAGE-ON

    http.GET[HttpResponse](url) map { resp =>
      logResponse(resp)

      val parsedResponse = resp.status match {
        case OK => resp.json.as[EnrolmentStoreProxyResponse]
        case NO_CONTENT =>
          EnrolmentStoreProxyResponse(enrolments = List.empty[EnrolmentResponse])
        case _ =>
          throw new BadRequestException(s"Enrolment Store Proxy Status : ${resp.status}")
      }
      auditCall(url, groupId, parsedResponse)
      parsedResponse
    } recover {
      case e: Throwable =>
        logger.error(s"enrolment-store-proxy failed. url: $url, error: $e", e)
        throw e
    }
  }

  // $COVERAGE-OFF$Loggers
  private def logResponse(response: HttpResponse): Unit =
    if (HttpStatusCheck.is2xx(response.status))
      logger.debug("GetEnrolmentByGroupId request is successful")
    else
      logger.warn(s"GetEnrolmentByGroupId request is failed with response $response")

  // $COVERAGE-ON

  private def auditCall(url: String, groupId: String, response: EnrolmentStoreProxyResponse)(implicit
    hc: HeaderCarrier
  ): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Enrolment-Store-Proxy-Call",
      path = url,
      details = Json.toJson(EnrolmentStoreProxyEvent(groupId = groupId, enrolments = response.enrolments)),
      eventType = "EnrolmentStoreProxyCall"
    )

}
