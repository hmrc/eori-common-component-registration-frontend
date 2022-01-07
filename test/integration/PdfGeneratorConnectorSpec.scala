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

package integration

import uk.gov.hmrc.eoricommoncomponent.frontend.connector.PdfGeneratorConnector
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import util.externalservices.AuditService
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.stubs.PdfGeneratorServiceStub
import scala.concurrent.ExecutionContext.Implicits.global

class PdfGeneratorConnectorSpec extends IntegrationTestsSpec with PdfGeneratorServiceStub with ScalaFutures {

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll: Unit = startMockServer()

  override def afterAll: Unit = stopMockServer()

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map("microservice.services.pdf-generator.host" -> Host, "microservice.services.pdf-generator.port" -> Port)
    )
    .build()

  private lazy val pdfGeneratorConnector = app.injector.instanceOf[PdfGeneratorConnector]
  implicit val hc: HeaderCarrier         = HeaderCarrier()

  private val html = "html"

  val pdf: Array[Byte] = Array[Byte](0, 1, -128, -1, 127, 15, 0)

  "Generate PDF" should {

    "produce a PDF from a simple string" in {
      returnResponseForGenerateRequestWithBody(html, pdf)

      val response = await(pdfGeneratorConnector.generatePdf(html))

      response mustBe pdf
    }
  }
}
