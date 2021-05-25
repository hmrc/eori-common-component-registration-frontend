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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, GroupId}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.EnrolmentStoreProxyService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupEnrolmentExtractor @Inject() (enrolmentStoreProxyService: EnrolmentStoreProxyService)(implicit
  ec: ExecutionContext
) {

  def hasGroupIdEnrolmentTo(groupId: String, service: Service)(implicit hc: HeaderCarrier): Future[Boolean] =
    groupIdEnrolmentTo(groupId, service).map(_.isDefined)

  def groupIdEnrolmentTo(groupId: String, service: Service)(implicit
    hc: HeaderCarrier
  ): Future[Option[EnrolmentResponse]] =
    enrolmentStoreProxyService.enrolmentForGroup(GroupId(groupId), service)

  def groupIdEnrolments(groupId: String)(implicit hc: HeaderCarrier): Future[List[EnrolmentResponse]] =
    enrolmentStoreProxyService.enrolmentsForGroup(GroupId(groupId))

}
