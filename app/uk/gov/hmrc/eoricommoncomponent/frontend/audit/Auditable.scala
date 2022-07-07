/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.audit

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent, ExtendedDataEvent}

import scala.concurrent.ExecutionContext

@Singleton
class Auditable @Inject() (auditConnector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val auditSource: String = appConfig.appName

  private val audit: Audit = Audit(auditSource, auditConnector)

  def sendDataEvent(
    transactionName: String,
    path: String = "N/A",
    tags: Map[String, String] = Map.empty,
    detail: Map[String, String],
    eventType: String
  )(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      DataEvent(
        auditSource,
        eventType,
        tags = hc.toAuditTags(transactionName, path) ++ tags,
        detail = hc.toAuditDetails(detail.toSeq: _*)
      )
    )

  def sendExtendedDataEvent(
    transactionName: String,
    path: String,
    tags: Map[String, String] = Map.empty,
    details: JsValue,
    eventType: String
  )(implicit hc: HeaderCarrier): Unit =
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(auditSource, eventType, tags = hc.toAuditTags(transactionName, path) ++ tags, detail = details)
    )

}
