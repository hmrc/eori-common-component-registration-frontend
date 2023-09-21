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

import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.UpdateVerifiedEmailConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.httpparsers._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.email.{RequestDetail, UpdateVerifiedEmailRequest}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.RequestCommon
import uk.gov.hmrc.http.HeaderCarrier
import util.externalservices.ExternalServicesConfig._
import util.externalservices.UpdateVerifiedEmailMessagingService

import java.time.LocalDateTime

class UpdateVerifiedEmailConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.port"                          -> Port,
        "microservice.services.eori-common-component-hods-proxy.update-verified-email.context" -> "update-verified-email",
        "auditing.enabled"                                                                     -> false,
        "auditing.consumer.baseUri.port"                                                       -> Port
      )
    )
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  private val connector          = app.injector.instanceOf[UpdateVerifiedEmailConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val responseWithOk: JsValue =
    Json.parse("""
        |{
        |  "subscriptionStatusResponse": {
        |    "responseCommon": {
        |      "status": "OK",
        |      "processingDate": "2016-03-17T09:30:47Z"
        |    }
        |  }
        |}
      """.stripMargin)

  before {
    resetMockServer()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  val expectedUrl = "/update-verified-email"

  val dateTime                     = DateTime.now()
  private val requestDetail        = RequestDetail("idType", "idNumber", "test@email.com", dateTime)
  private val requestDateInd       = LocalDateTime.of(2001, 12, 17, 9, 30, 47, 0)
  private val requestCommon        = RequestCommon("CDS", requestDateInd, "012345678901234")
  private val verifiedEmailRequest = VerifiedEmailRequest(UpdateVerifiedEmailRequest(requestCommon, requestDetail))

  "updateVerifiedEmail" should {

    "return Right with VerifiedEmailResponse when call was successful with OK" in {
      UpdateVerifiedEmailMessagingService.returnTheResponseWhenReceiveRequest(
        expectedUrl,
        UpdateVerifiedEmailMessagingService.validResponse
      )
      await(connector.updateVerifiedEmail(verifiedEmailRequest)) must be(
        Right(Json.parse(UpdateVerifiedEmailMessagingService.validResponse).as[VerifiedEmailResponse])
      )
    }

    "return Left with Forbidden when call returned Upstream4xxResponse with 403" in {
      UpdateVerifiedEmailMessagingService.stubTheResponse(expectedUrl, "", FORBIDDEN)

      await(connector.updateVerifiedEmail(verifiedEmailRequest)) must be(Left(Forbidden))
    }

    "return Left with BadRequest when call returned Upstream4xxResponse with 400" in {
      UpdateVerifiedEmailMessagingService.stubTheResponse(expectedUrl, "", BAD_REQUEST)

      await(connector.updateVerifiedEmail(verifiedEmailRequest)) must be(Left(BadRequest))
    }

    "return Left with ServiceUnavailable when call returned Upstream5xxResponse with 500" in {
      UpdateVerifiedEmailMessagingService.stubTheResponse(expectedUrl, "", INTERNAL_SERVER_ERROR)

      await(connector.updateVerifiedEmail(verifiedEmailRequest)) must be(Left(ServiceUnavailable))
    }

  }
}
