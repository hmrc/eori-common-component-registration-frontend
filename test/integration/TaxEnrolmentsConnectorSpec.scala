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

package integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, putRequestedFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.TaxEnrolmentsConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{TaxEnrolmentsRequest, TaxEnrolmentsResponse}
import uk.gov.hmrc.http.HeaderCarrier
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.TaxEnrolmentsService._

class TaxEnrolmentsConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.tax-enrolments.host"    -> Host,
        "microservice.services.tax-enrolments.port"    -> Port,
        "microservice.services.tax-enrolments.context" -> "tax-enrolments",
        "auditing.enabled"                             -> false,
        "auditing.consumer.baseUri.host"               -> Host,
        "auditing.consumer.baseUri.port"               -> Port
      )
    )
    .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val formBundleId                = "bundle-id"
  private val expectedPutUrl              = s"/tax-enrolments/subscriptions/$formBundleId/issuer"
  private lazy val taxEnrolmentsConnector = app.injector.instanceOf[TaxEnrolmentsConnector]

  private val taxEnrolmentsRequest = validTaxEnrolmentsIssuerRequestJson.as[TaxEnrolmentsRequest]

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  "TaxEnrolmentConnector" should {

    "return success with list of enrolments" in {
      val safeId = "testId"
      returnTheTaxEnrolmentsResponseOK(safeId)

      taxEnrolmentsConnector.getEnrolments(safeId).futureValue mustBe List(
        TaxEnrolmentsResponse("516b9976-00fd-4da6-b59c-4d09054912bb")
      )
    }

    "call tax enrolment service with correct url and payload" in {
      scala.concurrent.Await.ready(taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId), defaultTimeout)
      WireMock.verify(
        putRequestedFor(urlEqualTo(expectedPutUrl)).withRequestBody(
          equalToJson(validTaxEnrolmentsIssuerRequestJson.toString)
        )
      )
    }

    "return successful future with correct status when enrolment status service returns good status(204)" in {
      returnEnrolmentResponseWhenReceiveRequest(
        expectedPutUrl,
        validTaxEnrolmentsIssuerRequestJson.toString,
        NO_CONTENT
      )
      taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId).futureValue mustBe NO_CONTENT
    }

    "return successful future with correct status when enrolment status service returns any fail status" in {
      returnEnrolmentResponseWhenReceiveRequest(
        expectedPutUrl,
        validTaxEnrolmentsIssuerRequestJson.toString,
        BAD_REQUEST
      )

      taxEnrolmentsConnector.enrol(taxEnrolmentsRequest, formBundleId).futureValue mustBe BAD_REQUEST
    }
  }
}
