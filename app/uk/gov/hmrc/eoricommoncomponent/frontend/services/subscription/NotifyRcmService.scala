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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.NotifyRcmConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.NotifyRcmRequest
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotifyRcmService @Inject() (sessionCache: SessionCache, notifyRcmConnector: NotifyRcmConnector) {

  def notifyRcm(service: Service)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val ff = for {
      sd    <- sessionCache.subscriptionDetails
      email <- sessionCache.email
    } yield {
      val name       = sd.name
      val eori       = sd.eoriNumber.getOrElse(throw new IllegalArgumentException("Eori not found"))
      val rcmRequest = NotifyRcmRequest(eori, name, email, service)
      notifyRcmConnector.notifyRCM(rcmRequest)
    }
    ff.flatMap(identity)
  }

}
