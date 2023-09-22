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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import uk.gov.hmrc.eoricommoncomponent.frontend.connector.HandleSubscriptionConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.HandleSubscriptionRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.RecipientDetails
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, SafeId, TaxPayerId}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDateTime, ZoneId, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class HandleSubscriptionService @Inject() (handleSubscriptionConnector: HandleSubscriptionConnector) {

  def handleSubscription(
    formBundleId: String,
    recipientDetails: RecipientDetails,
    sapNumber: TaxPayerId,
    eori: Option[Eori],
    emailVerificationTimestamp: Option[LocalDateTime],
    safeId: SafeId
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val timestampValue =
      emailVerificationTimestamp.getOrElse(
        LocalDateTime.ofInstant(Clock.systemUTC().instant, ZoneId.of("Europe/London"))
      )
    val zonedDateTime = timestampValue.atZone(ZoneOffset.UTC)

    handleSubscriptionConnector.call(
      HandleSubscriptionRequest(
        recipientDetails,
        formBundleId,
        sapNumber.id,
        eori.map(_.id),
        s"$zonedDateTime",
        safeId.id
      )
    )
  }

}
