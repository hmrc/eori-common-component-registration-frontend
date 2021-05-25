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
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationDetailsService @Inject() (sessionCache: SessionCache)(implicit ec: ExecutionContext) {

  def cacheOrgName(orgName: String)(implicit hq: HeaderCarrier): Future[Boolean] =
    sessionCache.registrationDetails.map {
      case rdo: RegistrationDetailsOrganisation => rdo.copy(name = orgName)
      case _                                    => throw new IllegalArgumentException("Expecting RegistrationDetailsOrganisation but found something else")
    }.flatMap(updatedHolder => sessionCache.saveRegistrationDetails(updatedHolder))

  def cacheAddress(address: Address)(implicit hq: HeaderCarrier): Future[Boolean] =
    sessionCache.registrationDetails.map {
      case rdo: RegistrationDetailsOrganisation => rdo.copy(address = address)
      case rdi: RegistrationDetailsIndividual   => rdi.copy(address = address)
      case _                                    => throw new IllegalStateException("Incomplete cache cannot complete journey")
    }.flatMap(updatedHolder => sessionCache.saveRegistrationDetails(updatedHolder))

  def cacheNameDateOfBirth(rd: RegistrationDetailsIndividual)(implicit hq: HeaderCarrier): Future[Boolean] =
    sessionCache.registrationDetails.map {
      case rdi: RegistrationDetailsIndividual => rdi.copy(name = rd.name, dateOfBirth = rd.dateOfBirth)
      case _                                  => throw new IllegalArgumentException("Expecting RegistrationDetailsIndividual but found something else")
    }.flatMap(updatedHolder => sessionCache.saveRegistrationDetails(updatedHolder))

  def initialiseCacheWithRegistrationDetails(
    organisationType: CdsOrganisationType
  )(implicit hq: HeaderCarrier): Future[Boolean] =
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
