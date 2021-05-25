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

package uk.gov.hmrc.eoricommoncomponent.frontend.services.organisation

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{EtmpOrganisationType, RegistrationDetailsOrganisation}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.{RequestSessionData, SessionCache}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrgTypeLookup @Inject() (requestSessionData: RequestSessionData, sessionCache: SessionCache)(implicit
  ec: ExecutionContext
) {

  def etmpOrgTypeOpt(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Option[EtmpOrganisationType]] =
    requestSessionData.userSelectedOrganisationType match {
      case Some(cdsOrgType) => Future.successful(Some(EtmpOrganisationType(cdsOrgType)))
      case None =>
        sessionCache.registrationDetails map {
          case RegistrationDetailsOrganisation(_, _, _, _, _, _, orgType) => orgType
          case _                                                          => throw new IllegalStateException("No Registration details in cache.")
        }
    }

  def etmpOrgType(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[EtmpOrganisationType] =
    requestSessionData.userSelectedOrganisationType match {
      case Some(cdsOrgType) => Future.successful(EtmpOrganisationType(cdsOrgType))
      case None =>
        sessionCache.registrationDetails map {
          case RegistrationDetailsOrganisation(_, _, _, _, _, _, Some(orgType)) => orgType
          case RegistrationDetailsOrganisation(_, _, _, _, _, _, _) =>
            throw new IllegalStateException("Unable to retrieve Org Type from the cache")
          case _ => throw new IllegalStateException("No Registration details in cache.")
        }
    }

}
