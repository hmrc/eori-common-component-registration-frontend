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

import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class PdfGeneratorConnector @Inject() (http: WSClient, appConfig: AppConfig) {

  private val logger = Logger(this.getClass)

  private val baseUrl: String = appConfig.pdfGeneratorBaseUrl

  private lazy val url = s"$baseUrl/pdf-generator-service/generate"

  def generatePdf(html: String)(implicit ec: ExecutionContext): Future[ByteString] = {
    logger.debug(s"postUrl: $url")

    http
      .url(url)
      .withHttpHeaders(CONTENT_TYPE -> MimeTypes.JSON)
      .post(Json.toJson(Map("html" -> html)))
      .map(_.bodyAsBytes)
      .recoverWith {
        case NonFatal(e) =>
          logger.error(s"postUrl: $url FAILED.", e)
          Future.failed(e)
      }
  }

}
