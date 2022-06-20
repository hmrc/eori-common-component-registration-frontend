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
import java.time.LocalDate
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, KeyValue, SafeId, TaxEnrolmentsRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.TaxEnrolmentsRequest._
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsService @Inject()(taxEnrolmentsConnector: TaxEnrolmentsConnector) {

  private val serviceName = "HMRC-CUS-ORG"

  def doesEnrolmentExist(safeId: SafeId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    taxEnrolmentsConnector.getEnrolments(safeId.id).map { enrolments =>
      enrolments.exists(_.serviceName == serviceName)
    }

  def issuerCall(formBundleId: String, eori: Eori, dateOfEstablishment: Option[LocalDate], service: Service)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Int] = {
    val identifiers = List(KeyValue(key = "EORINUMBER", value = eori.id))
    val verifiers =
      dateOfEstablishment.map(doe => List(KeyValue(key = "DATEOFESTABLISHMENT", value = pattern.format(doe))))
    val taxEnrolmentsRequest =
      TaxEnrolmentsRequest(serviceName = service.enrolmentKey, identifiers = identifiers, verifiers = verifiers)
    taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId)
  }
}
