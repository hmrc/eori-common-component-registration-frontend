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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.SubscriptionStatusConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Sub01Outcome, SubscriptionStatusQueryParams}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionStatusService @Inject() (
  connector: SubscriptionStatusConnector,
  requestCommonGenerator: RequestCommonGenerator,
  cache: SessionCache
)(implicit ec: ExecutionContext) {

  private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

  def getStatus(idType: String, id: String)(implicit
    hc: HeaderCarrier,
    originatingService: Service,
    request: Request[_]
  ): Future[PreSubscriptionStatus] = {

    def createRequest =
      SubscriptionStatusQueryParams(requestCommonGenerator.receiptDate, Service.regimeCDS, idType, id)

    def saveToCache(processingDate: LocalDateTime) =
      cache.saveSub01Outcome(Sub01Outcome(dateFormat.format(processingDate)))

    def checkSubscriptionStatus(): Future[PreSubscriptionStatus] =
      connector.status(createRequest).flatMap { response =>
        saveToCache(response.responseCommon.processingDate).map { _ =>
          response.responseDetail.subscriptionStatus match {
            case "00" => NewSubscription
            case "01" => SubscriptionProcessing
            case "04" => SubscriptionExists
            case "05" => SubscriptionRejected
            case "11" => SubscriptionProcessing
            case "14" => SubscriptionProcessing
            case "99" => SubscriptionRejected
          }
        }
      }

    checkSubscriptionStatus()
  }

}

sealed trait PreSubscriptionStatus

case object NewSubscription extends PreSubscriptionStatus

case object SubscriptionRejected extends PreSubscriptionStatus

case object SubscriptionProcessing extends PreSubscriptionStatus

case object SubscriptionExists extends PreSubscriptionStatus
