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

import play.api.Logging
import play.api.mvc.Request
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{
  CdsOrganisationType,
  RegistrationDetailsIndividual,
  RegistrationDetailsOrganisation
}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.cache.SessionCache

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationDetailsService @Inject() (sessionCache: SessionCache)(implicit ec: ExecutionContext) extends Logging {

  def cacheAddress(address: Address)(implicit request: Request[_]): Future[Boolean] =
    sessionCache.registrationDetails.map {
      case rdo: RegistrationDetailsOrganisation => rdo.copy(address = address)
      case rdi: RegistrationDetailsIndividual   => rdi.copy(address = address)
      case _ =>
        val error = "Incomplete cache cannot complete journey"
        // $COVERAGE-OFF$Loggers
        logger.warn(error)
        // $COVERAGE-ON
        throw new IllegalStateException(error)
    }.flatMap(updatedHolder => sessionCache.saveRegistrationDetails(updatedHolder))

  def initialiseCacheWithRegistrationDetails(
    organisationType: CdsOrganisationType
  )(implicit request: Request[_]): Future[Boolean] =
    sessionCache.saveSubscriptionDetails(
      SubscriptionDetails(formData = FormData(organisationType = Some(organisationType)))
    ).flatMap(_ => saveRegistrationDetails(organisationType))

  private def saveRegistrationDetails(orgType: CdsOrganisationType)(implicit request: Request[_]) =
    if (IndividualOrganisations.contains(orgType)) sessionCache.saveRegistrationDetails(RegistrationDetailsIndividual())
    else sessionCache.saveRegistrationDetails(RegistrationDetailsOrganisation())

}
