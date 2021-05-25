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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import javax.inject.Inject
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.Status.{NO_CONTENT, OK}
import uk.gov.hmrc.eoricommoncomponent.frontend.audit.Auditable
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, EnrolmentStoreProxyResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest.{
  ES1Request,
  ES1Response,
  KnownFacts,
  KnownFactsQuery
}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.events.EnrolmentStoreProxyEvent
import uk.gov.hmrc.eoricommoncomponent.frontend.util.HttpStatusCheck
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient

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

  private def logResponse(response: HttpResponse): Unit =
    if (HttpStatusCheck.is2xx(response.status))
      logger.debug("GetEnrolmentByGroupId request is successful")
    else
      logger.warn(s"GetEnrolmentByGroupId request is failed with response $response")

  private def auditCall(url: String, groupId: String, response: EnrolmentStoreProxyResponse)(implicit
    hc: HeaderCarrier
  ): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Enrolment-Store-Proxy-Call",
      path = url,
      details = Json.toJson(EnrolmentStoreProxyEvent(groupId = groupId, enrolments = response.enrolments)),
      eventType = "EnrolmentStoreProxyCall"
    )

  def queryKnownFactsByIdentifiers(
    knownFactsQuery: KnownFactsQuery
  )(implicit hc: HeaderCarrier): Future[Option[KnownFacts]] = {

    import uk.gov.hmrc.http.HttpReads.Implicits._

    val url = s"$baseUrl/$serviceContext/enrolment-store/enrolments"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"QueryKnownFactsByIdentifiers: $url, body: $knownFactsQuery and hc: $hc")
    // $COVERAGE-ON

    http.POST[KnownFactsQuery, Option[KnownFacts]](url, knownFactsQuery) map {
      response =>
        // $COVERAGE-OFF$Loggers
        logger.debug(s"QueryKnownFactsByIdentifiers response $response")
        // $COVERAGE-ON
        response
    }
  }

  def queryGroupsWithAllocatedEnrolment(es1Request: ES1Request)(implicit hc: HeaderCarrier): Future[ES1Response] = {

    val url =
      s"$baseUrl/$serviceContext/enrolment-store/enrolments/${es1Request.enrolment}/groups?type=${es1Request.queryType.value}"

    // $COVERAGE-OFF$Loggers
    logger.debug(s"QueryGroupsWithAllocatedEnrolment: $url and hc: $hc")
    // $COVERAGE-ON

    http.GET[HttpResponse](url).map { response =>
      response.status match {
        case Status.OK =>
          ES1Response.format.reads(response.json).asOpt.getOrElse(
            throw new Exception("Incorrect format for ES1 response")
          )
        case Status.NO_CONTENT  => ES1Response(None, None)
        case Status.BAD_REQUEST =>
          // TODO - ADD alert config for this
          // $COVERAGE-OFF$Loggers
          logger.error(s"ES1 FAIL - Response status: 400 with body ${response.body}")
          // $COVERAGE-ON
          throw new Exception("ES1 call failed with 400 status")
        case _ =>
          //TODO - ADD alert config for this
          // $COVERAGE-OFF$Loggers
          logger.warn(s"ES1 FAIL - Response status: ${response.status} with body ${response.body}")
          // $COVERAGE-ON
          throw new Exception(s"ES1 call failed with ${response.status} status")
      }
    }
  }

  def auditEs1Call(url: String, response: ES1Response)(implicit hc: HeaderCarrier): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "ecc-es1-call",
      path = url,
      details = Json.toJson(response),
      eventType = "ecc-es1"
    )

}
