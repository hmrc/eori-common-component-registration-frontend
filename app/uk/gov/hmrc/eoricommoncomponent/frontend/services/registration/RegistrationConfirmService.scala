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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.registration

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.LoggedInUser
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{
  ClearCacheAndRegistrationIdentificationService,
  RequestSessionData,
  SessionCache
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.{PreSubscriptionStatus, SubscriptionStatusService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationConfirmService @Inject() (
  val cdsFrontendCache: SessionCache,
  val subscriptionStatusService: SubscriptionStatusService,
  val requestSessionData: RequestSessionData,
  val clearDataService: ClearCacheAndRegistrationIdentificationService
)(implicit ec: ExecutionContext) {

  def currentSubscriptionStatus(implicit hc: HeaderCarrier): Future[PreSubscriptionStatus] =
    cdsFrontendCache.registrationDetails flatMap { registrationDetails =>
      subscriptionStatusService.getStatus("taxPayerID", registrationDetails.sapNumber.mdgTaxPayerId)
    }

  def clearRegistrationData(loggedInUser: LoggedInUser)(implicit hc: HeaderCarrier): Future[Unit] =
    clearDataService.clear(loggedInUser)

}
