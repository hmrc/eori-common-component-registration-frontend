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
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.EnrolmentStoreProxyConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EnrolmentResponse, ExistingEori, GroupId}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.models.enrolmentRequest.ES1Request
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyService @Inject() (enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit
  ec: ExecutionContext
) {

  private val activatedState = "Activated"

  def enrolmentForGroup(groupId: GroupId, service: Service)(implicit
    hc: HeaderCarrier
  ): Future[Option[EnrolmentResponse]] =
    enrolmentStoreProxyConnector
      .getEnrolmentByGroupId(groupId.id)
      .map(_.enrolments)
      .map(enrolment => enrolment.find(x => x.state == activatedState && x.service == service.enrolmentKey))

  def enrolmentsForGroup(groupId: GroupId)(implicit hc: HeaderCarrier): Future[List[EnrolmentResponse]] =
    enrolmentStoreProxyConnector
      .getEnrolmentByGroupId(groupId.id)
      .map(_.enrolments)
      .map(enrolment => enrolment.filter(x => x.state == activatedState))

  def isEnrolmentInUse(service: Service, existingEori: ExistingEori)(implicit
    hc: HeaderCarrier
  ): Future[Option[ExistingEori]] = {
    val es1Request = ES1Request(service, existingEori.id)
    enrolmentStoreProxyConnector
      .queryGroupsWithAllocatedEnrolment(es1Request)
      .map { es1Response =>
        if (es1Response.isEnrolmentInUse) Some(existingEori)
        else None
      }
  }

}
