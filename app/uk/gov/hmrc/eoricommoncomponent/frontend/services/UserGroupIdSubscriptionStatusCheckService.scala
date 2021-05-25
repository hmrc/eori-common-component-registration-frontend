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

package uk.gov.hmrc.eoricommoncomponent.frontend.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Result
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{GroupId, InternalId}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserGroupIdSubscriptionStatusCheckService @Inject() (
  subscriptionStatusService: SubscriptionStatusService,
  save4Later: Save4LaterService
)(implicit ec: ExecutionContext) {
  private val idType = "SAFE"

  def checksToProceed(groupId: GroupId, internalId: InternalId, service: Service)(continue: => Future[Result])(
    userIsInProcess: => Future[Result]
  )(otherUserWithinGroupIsInProcess: => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    save4Later.fetchCacheIds(groupId)
      .flatMap {
        case Some(cacheIds) =>
          val sameUser    = cacheIds.internalId == internalId
          val sameService = cacheIds.serviceCode.contains(service.code)

          subscriptionStatusService
            .getStatus(idType, cacheIds.safeId.id)
            .flatMap { status =>
              if (status != SubscriptionProcessing)
                if (sameService)
                  save4Later.deleteCachedGroupId(groupId).flatMap(_ => continue)
                else
                  save4Later.deleteCacheIds(groupId).flatMap(_ => continue)
              else
                (sameUser, sameService) match {
                  case (true, true)  => continue
                  case (true, false) => userIsInProcess
                  case (false, _)    => otherUserWithinGroupIsInProcess
                }
            }
        case _ =>
          continue
      }

}
