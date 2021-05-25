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
import play.api.Logger
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.Save4LaterConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailStatus
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.CachedData
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Save4LaterService @Inject() (save4LaterConnector: Save4LaterConnector) {

  private val logger = Logger(this.getClass)

  private val safeIdKey  = "safeId"
  private val orgTypeKey = "orgType"
  private val emailKey   = "email"

  def saveSafeId(groupId: GroupId, safeId: SafeId)(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.debug(s"saving SafeId $safeId for groupId $groupId")
    save4LaterConnector.put[SafeId](groupId.id, safeIdKey, safeId)
  }

  def saveOrgType(groupId: GroupId, mayBeOrgType: Option[CdsOrganisationType])(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    logger.debug(s"saving OrganisationType $mayBeOrgType for groupId $groupId")
    save4LaterConnector
      .put[CdsOrganisationType](groupId.id, orgTypeKey, mayBeOrgType)
  }

  def saveEmail(groupId: GroupId, emailStatus: EmailStatus)(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.debug(s"saving email address $emailStatus for groupId $groupId")
    save4LaterConnector.put[EmailStatus](groupId.id, emailKey, emailStatus)
  }

  def fetchOrgType(groupId: GroupId)(implicit hc: HeaderCarrier): Future[Option[CdsOrganisationType]] = {
    logger.debug(s"fetching OrganisationType for groupId $groupId")
    save4LaterConnector
      .get[CdsOrganisationType](groupId.id, orgTypeKey)
  }

  def fetchSafeId(groupId: GroupId)(implicit hc: HeaderCarrier): Future[Option[SafeId]] = {
    logger.debug(s"fetching SafeId for groupId $groupId")
    save4LaterConnector
      .get[SafeId](groupId.id, safeIdKey)
  }

  def fetchEmail(groupId: GroupId)(implicit hc: HeaderCarrier): Future[Option[EmailStatus]] = {
    logger.debug(s"fetching EmailStatus groupId $groupId")
    save4LaterConnector
      .get[EmailStatus](groupId.id, emailKey)
  }

  def fetchCacheIds(groupId: GroupId)(implicit hc: HeaderCarrier): Future[Option[CacheIds]] = {
    logger.debug(s"fetching CacheIds groupId $groupId")
    save4LaterConnector.get[CacheIds](groupId.id, CachedData.groupIdKey)
  }

  def deleteCachedGroupId(groupId: GroupId)(implicit hc: HeaderCarrier) =
    save4LaterConnector.deleteKey[CacheIds](groupId.id, CachedData.groupIdKey)

  def deleteCacheIds(groupId: GroupId)(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.debug(s"deleting CachIds for groupId $groupId")
    save4LaterConnector.delete[CacheIds](groupId.id)
  }

  def fetchProcessingService(
    groupId: GroupId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Service]] = {
    logger.debug(s"fetching Processing service for groupId $groupId")
    save4LaterConnector
      .get[CacheIds](groupId.id, CachedData.groupIdKey)
      .map {
        case Some(cacheIds) => cacheIds.serviceCode.flatMap(Service.withName)
        case _              => None
      }

  }

}
