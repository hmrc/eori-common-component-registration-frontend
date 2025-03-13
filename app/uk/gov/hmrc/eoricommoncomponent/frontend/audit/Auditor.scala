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

package uk.gov.hmrc.eoricommoncomponent.frontend.audit

import play.api.libs.json.JsValue
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Auditor @Inject() (auditConnector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val auditSource: String = appConfig.appName

  private def sendExtendedDataEvent(
    transactionName: String,
    path: String,
    tags: Map[String, String] = Map.empty,
    details: JsValue,
    eventType: String
  )(implicit hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(auditSource, eventType, tags = hc.toAuditTags(transactionName, path) ++ tags, detail = details)
    )

  private def sendExtendedDataEvent(
    transactionName: String,
    path: String,
    details: JsValue,
    eventType: String
  )(implicit hc: HeaderCarrier): Future[AuditResult] =
    sendExtendedDataEvent(transactionName = transactionName, path = path, details = details, tags = Map.empty, eventType = eventType)

  def sendSubscriptionDataEvent(path: String, details: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-subscription", path, details, "Subscription")
  }

  def sendRegistrationDataEvent(path: String, details: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-registration", path, details, "Registration")
  }

  def sendEnrolmentStoreCallEvent(path: String, details: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("Enrolment-Store-Proxy-Call", path, details, "EnrolmentStoreProxyCall")
  }

  def sendRegistrationDisplayEvent(path: String, detail: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-registration-display", path, detail, "RegistrationDisplay")
  }

  def sendSubscriptionDisplayEvent(path: String, detail: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-subscription-display", path, detail, "SubscriptionDisplay")
  }

  def sendSubscriptionStatusEvent(path: String, detail: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-subscription-status", path, detail, "SubscriptionStatus")
  }

  def sendTaxEnrolmentIssuerCallEvent(path: String, detail: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-issuer-call", path, detail, "IssuerCall")
  }

  def sendCustomsDataStoreEvent(path: String, detail: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("customs-data-store", path, detail, "CustomsDataStoreUpdate")
  }

  def sendSubscriptionFlowSessionFailureEvent(detail: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    sendExtendedDataEvent("ecc-registration-subscription-flow-session-failure", "", detail, "SubscriptionFlowSessionFailure")
  }
}
