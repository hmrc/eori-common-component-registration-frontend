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

import uk.gov.hmrc.eoricommoncomponent.frontend.connector.MatchingServiceConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.{MatchingRequestHolder, MatchingResponse}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.{AuditService, MatchService}

class MatchingServiceConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.host"          -> Host,
        "microservice.services.eori-common-component-hods-proxy.port"          -> Port,
        "microservice.services.eori-common-component-hods-proxy.match.context" -> "register-with-id",
        "auditing.enabled"                                                     -> true,
        "auditing.consumer.baseUri.host"                                       -> Host,
        "auditing.consumer.baseUri.port"                                       -> Port
      )
    )
    .build()

  private lazy val matchingServiceConnector = app.injector.instanceOf[MatchingServiceConnector]
  val expectedPostUrl                       = "/register-with-id"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  private val serviceRequestJson =
    Json.parse("""{
        |  "registerWithIDRequest": {
        |    "requestCommon": {
        |      "regime": "CDS",
        |      "receiptDate": "2016-07-08T08:35:13Z",
        |      "acknowledgementReference": "fce07075-2e2e-4b12-840e-a63bff6ab1bd"
        |    },
        |    "requestDetail": {
        |      "IDType": "UTR",
        |      "IDNumber": "2108834503",
        |      "requiresNameMatch": false,
        |      "isAnAgent": false
        |    }
        |  }
        |}
      """.stripMargin)

  private def requestJsonFragment(isAnIndividual: Boolean): String =
    if (isAnIndividual)
      """
        |     "individual": {
        |        "firstName": "John",
        |        "middleName": "Middle",
        |        "lastName": "Doe",
        |        "dateOfBirth": "1980-01-01"
        |      },
      """.stripMargin
    else
      """
        |      "organisation": {
        |        "organisationName": "orgName",
        |        "isAGroup": false,
        |        "organisationType": "Partnership",
        |        "code": "0001"
        |      },
      """.stripMargin

  private def serviceResponseJsonWithOptionalParams(isAnIndividual: Boolean) =
    Json.parse(s"""
         |{
         |  "registerWithIDResponse": {
         |    "responseCommon":    {
         |      "status": "OK",
         |      "processingDate": "2016-07-08T08:35:13Z",
         |      "returnParameters": [      {
         |        "paramName": "SAP_NUMBER",
         |        "paramValue": "0123456789"
         |      }]
         |    },
         |    "responseDetail":    {
         |      "SAFEID": "XE0000123456789",
         |      "ARN": "",
         |      "isEditable": true,
         |      "isAnAgent": false,
         |      "isAnIndividual": $isAnIndividual,
         |${requestJsonFragment(isAnIndividual)}
         |      "address":       {
         |        "addressLine1": "Line 1",
         |        "addressLine2": "line 2",
         |        "addressLine3": "line 3",
         |        "addressLine4": "line 4",
         |        "postalCode": "SE28 1AA",
         |        "countryCode": "ZZ"
         |      },
         |      "contactDetails":       {
         |        "phoneNumber": "01632961234",
         |        "emailAddress": "john.doe@example.com"
         |      }
         |    }
         |  }
         |}
      """.stripMargin)

  private val serviceResponseJsonOrganisationWithOptionalParams =
    serviceResponseJsonWithOptionalParams(isAnIndividual = false)

  private val serviceResponseJsonIndividualWithOptionalParams =
    serviceResponseJsonWithOptionalParams(isAnIndividual = true)

  private val matchFailureResponse = Json.parse("""
      |{
      |  "registerWithIDResponse": {
      |    "responseCommon":    {
      |      "status": "OK",
      |      "statusText":"002 - No Match Found",
      |      "processingDate": "2016-07-08T08:35:13Z",
      |      "returnParameters": [{
      |       "paramName": "POSITION",
      |       "paramValue": "FAIL"
      |      }]
      |    }
      |  }
      |}
    """.stripMargin)

  private val match400ErrorResponse = Json.parse("""
      |{
      |  "errorDetail": {
      |    "timestamp": "2016­08­16T18:15:41Z",
      |    "correlationId": "f058ebd6­02f7­4d3f­942e­904344e8cde5",
      |    "errorCode": "400",
      |    "errorMessage": "Duplicate Submission",
      |    "source": "Back End",
      |    "sourceFaultDetail": {
      |      "detail": [
      |        "003 ­ Duplicate submission",
      |        "other error description"
      |        ]
      |    }
      |  }
      |}
    """.stripMargin)

  private val match500ErrorResponse = Json.parse("""
      |{
      |  "errorDetail": {
      |    "timestamp": "2016­08­16T18:17:45Z",
      |    "correlationID": "f058ebd6­02f7­4d3f­942e­904344e8cde5",
      |    "errorCode": "500",
      |    "errorMessage": "Internal error",
      |    "source": "Back End",
      |    "sourceFaultDetail": {
      |      "detail": [
      |        "Connection Timeout"
      |      ]
      |    }
      |  }
      |}
    """.stripMargin)

  "matchingServiceConnector" should {

    "return successful response with organisation when matching service returns 200" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        serviceResponseJsonOrganisationWithOptionalParams.toString
      )
      await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder])).get must be(
        serviceResponseJsonOrganisationWithOptionalParams.as[MatchingResponse]
      )
    }

    "return successful response with individual when matching service returns 200" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        serviceResponseJsonIndividualWithOptionalParams.toString
      )
      await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder])).get must be(
        serviceResponseJsonIndividualWithOptionalParams.as[MatchingResponse]
      )
    }

    "return None when matching service can't find a match" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        matchFailureResponse.toString(),
        OK
      )
      await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder])) must be(None)
    }

    "return Exception when matching service returns a downstream 500 response (Internal Service Error)" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        match500ErrorResponse.toString(),
        INTERNAL_SERVER_ERROR
      )
      val caught = intercept[UpstreamErrorResponse] {
        await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder]))
      }

      caught.statusCode mustBe 500
      caught.message must include(s"Response body: '$match500ErrorResponse'")
    }

    "return Exception when matching service fails with 4xx (any 4xx response apart from 400)" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        "Forbidden",
        FORBIDDEN
      )
      val caught = intercept[UpstreamErrorResponse] {
        await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder]))
      }

      caught.statusCode mustBe 403
      caught.message must include("Response body: 'Forbidden'")
    }

    "audit a successful request" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString,
        serviceResponseJsonOrganisationWithOptionalParams.toString
      )
      await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder]))

      eventually(AuditService.verifyXAuditWrite(1))
    }

    "not audit a failed request" in {
      MatchService.returnTheMatchResponseWhenReceiveRequest(
        expectedPostUrl,
        serviceRequestJson.toString(),
        match400ErrorResponse.toString(),
        BAD_REQUEST
      )
      val caught = intercept[UpstreamErrorResponse] {
        await(matchingServiceConnector.lookup(serviceRequestJson.as[MatchingRequestHolder]))
      }

      caught.statusCode mustBe 400
      AuditService.verifyXAuditWrite(0)
    }

  }
}
