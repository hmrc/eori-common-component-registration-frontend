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

package unit.services.subscription

import base.UnitSpec
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.TaxEnrolmentsConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{Eori, SafeId, TaxEnrolmentsRequest, TaxEnrolmentsResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service
import uk.gov.hmrc.eoricommoncomponent.frontend.services.subscription.TaxEnrolmentsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class TaxEnrolmentsServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfter {

  private val mockTaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]

  private val service                               = new TaxEnrolmentsService(mockTaxEnrolmentsConnector)
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  before {
    reset(mockTaxEnrolmentsConnector)
  }

  val testService          = Service.cds
  val safeId               = SafeId("safeid")
  val eori                 = Eori("GB99999999")
  val formBundleId         = "formBundleId"
  val date                 = LocalDate.parse("2010-04-28")
  val taxEnrolmentResponse = TaxEnrolmentsResponse(serviceName = testService.enrolmentKey)

  "TaxEnrolmentsService" should {

    "make issuer call" in {
      when(
        mockTaxEnrolmentsConnector
          .enrol(any[TaxEnrolmentsRequest], any[String])(any[HeaderCarrier])
      ).thenReturn(Future.successful(NO_CONTENT))

      await(service.issuerCall(formBundleId, eori, Some(date), testService)) shouldBe NO_CONTENT

      verify(mockTaxEnrolmentsConnector)
        .enrol(any[TaxEnrolmentsRequest], any[String])(any[HeaderCarrier])
    }
  }
}
