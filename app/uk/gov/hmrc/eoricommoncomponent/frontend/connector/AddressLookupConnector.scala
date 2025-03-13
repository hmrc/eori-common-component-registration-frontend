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
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.models.address.{AddressLookupFailure, AddressLookupResponse, AddressLookupSuccess, AddressRequestBody}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupConnector @Inject() (http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def lookup(postcode: String, firstLineOpt: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[AddressLookupResponse] = {

    val body = AddressRequestBody(postcode, firstLineOpt)

    val url = appConfig.addressLookup // + queryParams

    // $COVERAGE-OFF$Loggers
    logger.debug(s"Address lookup url: $url, body: $body")
    // $COVERAGE-ON

    http.POST[AddressRequestBody, HttpResponse](url, body) map { response =>
      response.status match {
        case OK => AddressLookupSuccess(response.json.as[Seq[Address]](Address.lookupReads)).sorted()
        case _ =>
          logger.warn(s"Address lookup respond with status ${response.status} and body: ${response.body}")
          AddressLookupFailure
      }
    }
  }

}
