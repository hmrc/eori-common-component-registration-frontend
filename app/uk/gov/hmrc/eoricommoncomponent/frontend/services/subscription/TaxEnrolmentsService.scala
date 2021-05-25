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
import org.joda.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.TaxEnrolmentsConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.TaxEnrolmentsRequest._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, KeyValue, TaxEnrolmentsRequest}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class TaxEnrolmentsService @Inject() (taxEnrolmentsConnector: TaxEnrolmentsConnector) {

  def issuerCall(formBundleId: String, eori: Eori, dateOfEstablishment: Option[LocalDate], service: Service)(implicit
    hc: HeaderCarrier
  ): Future[Int] = {
    val identifiers = List(KeyValue(key = "EORINUMBER", value = eori.id))
    val verifiers =
      dateOfEstablishment.map(doe => List(KeyValue(key = "DATEOFESTABLISHMENT", value = doe.toString(pattern))))
    val taxEnrolmentsRequest =
      TaxEnrolmentsRequest(serviceName = service.enrolmentKey, identifiers = identifiers, verifiers = verifiers)
    taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId)
  }

}
