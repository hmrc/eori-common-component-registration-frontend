/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationDetailsService @Inject() (sessionCache: SessionCache)(implicit ec: ExecutionContext) {

  def cacheAddress(address: Address)(implicit request: Request[_]): Future[Boolean] =
    sessionCache.registrationDetails.map {
      case rdo: RegistrationDetailsOrganisation => rdo.copy(address = address)
      case rdi: RegistrationDetailsIndividual   => rdi.copy(address = address)
      case _                                    => throw new IllegalStateException("Incomplete cache cannot complete journey")
    }.flatMap(updatedHolder => sessionCache.saveRegistrationDetails(updatedHolder))

  def initialiseCacheWithRegistrationDetails(
    organisationType: CdsOrganisationType
  )(implicit request: Request[_]): Future[Boolean] =
    sessionCache.subscriptionDetails flatMap { subDetails =>
      sessionCache.saveSubscriptionDetails(
        subDetails.copy(formData = subDetails.formData.copy(organisationType = Some(organisationType)))
      )

      organisationType match {
        case SoleTrader | Individual | ThirdCountryIndividual | ThirdCountrySoleTrader =>
          sessionCache.saveRegistrationDetails(RegistrationDetailsIndividual())
        case _ => sessionCache.saveRegistrationDetails(RegistrationDetailsOrganisation())
      }
    }

}
