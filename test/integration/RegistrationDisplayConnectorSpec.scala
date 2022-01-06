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

import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status._
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{RegistrationDisplayConnector, ServiceUnavailableResponse}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.registration.{
  RegistrationDisplayRequestHolder,
  RegistrationDisplayResponseHolder
}
import uk.gov.hmrc.http.HeaderCarrier
import util.externalservices.ExternalServicesConfig.{Host, Port}
import util.externalservices.{AuditService, RegistrationDisplay}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationDisplayConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.host"                         -> Host,
        "microservice.services.eori-common-component-hods-proxy.port"                         -> Port,
        "microservice.services.eori-common-component-hods-proxy.registration-display.context" -> "registration-display",
        "auditing.consumer.baseUri.host"                                                      -> Host,
        "auditing.consumer.baseUri.port"                                                      -> Port
      )
    )
    .build()

  implicit val hc = HeaderCarrier()

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  val servicePostUrl = "/registration-display"

  private lazy val Connector = app.injector.instanceOf[RegistrationDisplayConnector]

  private val serviceRequestJson =
    Json.parse(s"""
         |{
         |   "registrationDisplayRequest":{
         |      "requestCommon":{
         |         "receiptDate":"2019-04-09T14:00:00Z",
         |         "requestParameters":[
         |            {
         |               "paramName":"REGIME",
         |               "paramValue":"CDS"
         |            },
         |            {
         |               "paramName":"ID_Type",
         |               "paramValue":"SAFE"
         |            },
         |            {
         |               "paramName":"ID_Value",
         |               "paramValue":"XE111123456789"
         |            }
         |         ]
         |      }
         |   }
         |}
    """.stripMargin)

  private val serviceRegistrationDisplayResponseJson =
    Json.parse(s"""
         |{
         |   "registrationDisplayResponse":{
         |      "responseCommon":{
         |         "status":"OK",
         |         "processingDate":"2016-09-02T09:30:47Z",
         |         "taxPayerID":"0100086619"
         |      },
         |      "responseDetail":{
         |         "SAFEID":"XY0000100086619",
         |         "isEditable":true,
         |         "isAnAgent":false,
         |         "isAnIndividual":true,
         |         "individual":{
         |            "firstName":"John",
         |            "lastName":"Doe",
         |            "dateOfBirth":"1989-09-21"
         |         },
         |         "address":{
         |            "addressLine1":"Street",
         |            "addressLine2":"City",
         |            "postalCode":"SE28 1AA",
         |            "countryCode":"GB"
         |         },
         |         "contactDetails":{
         |            "phoneNumber":"07584673896",
         |            "emailAddress":"John.Doe@example.com"
         |         }
         |      }
         |   }
         |}
      """.stripMargin)

  private val serviceRegistrationError500WithDetailsResponseJson =
    Json.parse("""{
        |     "errorDetail": {
        |         "timestamp": "2016-06-12T19:35:45.260000Z",
        |         "correlationID": "f058ebd6-02f7-4d3f-942e-904344e8cde5",
        |         "errorCode": "500",
        |         "errorMessage": "Internal error",
        |         "source": "Back End",
        |         "sourceFaultDetail": {
        |             "detail": [
        |             "Connection Timeout"
        |           ]
        |         }
        |     }
        |}""".stripMargin)

  private val serviceRegistrationError500WithoutDetailsResponseJson =
    Json.parse("""
        |{
        | "errorDetail": {
        | "timestamp": "2016-08-16T18:22:00.000Z",
        | "correlationID": "f058ebd6-02f7-4d3f-942e-904344e8cde5",
        | "errorCode": "500",
        | "errorMessage": "Internal error",
        | "source": "Internal error"
        | }
        |}
        |""".stripMargin)

  private val serviceRegistrationError200ResponseWithErrorJson =
    Json.parse("""
        | {
        | "registrationDisplayResponse": {
        | "responseCommon": {
        | "status": "OK",
        | "statusText":"004 - Duplicate submission acknowledgment reference",
        | "processingDate": "2016-08-17T19:33:47Z",
        | "returnParameters": [{
        | "paramName": "POSITION",
        | "paramValue": "FAIL"
        | }]
        | }
        | }
        |}
        |""".stripMargin)

  private val serviceRegistrationError400ResponseJson =
    Json.parse("""
        |{
        |  "errorDetail": {
        |    "timestamp": "2014-01-19T11:31:47Z",
        |    "correlationId": "f05uigd6-02f7-4d3f-942e-904365e8cde5",
        |    "errorCode": "400",
        |    "errorMessage": "REGIME missing or invalid",
        |    "source": "Back End",
        |    "sourceFaultDetail": {
        |      "detail": [
        |        "001 - REGIME missing or invalid"
        |      ]
        |    }
        |  }
        |}
        |""".stripMargin)

  private val serviceRegistrationError405ResponseJson =
    Json.parse("""
        |{
        |  "errorDetail": {
        |    "timestamp": "2014-01-19T11:31:47Z",
        |    "correlationId": "f05uigd6-02f7-4d3f-942e-904365e8cde5",
        |    "errorCode": "405",
        |    "errorMessage": "Method not allowed",
        |    "source": "Back End",
        |    "sourceFaultDetail": {
        |      "detail": [
        |        "405 - Method not allowed"
        |      ]
        |    }
        |  }
        |}
        |
        |""".stripMargin)

  "RegistrationDisplayConnector" should {
    "return RegistrationDisplayResponse with status successful response when registration-display returns 200" in {
      RegistrationDisplay.returnResponseWhenReceiveRequest(
        servicePostUrl,
        serviceRequestJson.toString(),
        serviceRegistrationDisplayResponseJson.toString()
      )
      await(Connector.registrationDisplay(serviceRequestJson.as[RegistrationDisplayRequestHolder])) must be(
        Right(serviceRegistrationDisplayResponseJson.as[RegistrationDisplayResponseHolder].registrationDisplayResponse)
      )
    }

    "return error RegistrationResponse when registration-display returns 500 with details" in {
      RegistrationDisplay.returnResponseWhenReceiveRequest(
        servicePostUrl,
        serviceRequestJson.toString(),
        serviceRegistrationError500WithDetailsResponseJson.toString(),
        INTERNAL_SERVER_ERROR
      )
      await(Connector.registrationDisplay(serviceRequestJson.as[RegistrationDisplayRequestHolder])) must be(
        Left(ServiceUnavailableResponse)
      )
    }

    "return error RegistrationResponse with registration-display returns 500 without details" in {
      RegistrationDisplay.returnResponseWhenReceiveRequest(
        servicePostUrl,
        serviceRequestJson.toString(),
        serviceRegistrationError500WithoutDetailsResponseJson.toString(),
        INTERNAL_SERVER_ERROR
      )
      await(Connector.registrationDisplay(serviceRequestJson.as[RegistrationDisplayRequestHolder])) must be(
        Left(ServiceUnavailableResponse)
      )
    }

    "return error RegistrationResponse when registration-display returns 200 with error message" in {
      RegistrationDisplay.returnResponseWhenReceiveRequest(
        servicePostUrl,
        serviceRequestJson.toString(),
        serviceRegistrationError200ResponseWithErrorJson.toString()
      )
      await(Connector.registrationDisplay(serviceRequestJson.as[RegistrationDisplayRequestHolder])) must be(
        Right(
          serviceRegistrationError200ResponseWithErrorJson
            .as[RegistrationDisplayResponseHolder]
            .registrationDisplayResponse
        )
      )
    }

    "return error RegistrationResponse when registration-display returns 400" in {
      RegistrationDisplay.returnResponseWhenReceiveRequest(
        servicePostUrl,
        serviceRequestJson.toString(),
        serviceRegistrationError400ResponseJson.toString(),
        BAD_REQUEST
      )
      await(Connector.registrationDisplay(serviceRequestJson.as[RegistrationDisplayRequestHolder])) must be(
        Left(ServiceUnavailableResponse)
      )
    }

    "return error RegistrationResponse when registration-display returns 405" in {
      RegistrationDisplay.returnResponseWhenReceiveRequest(
        servicePostUrl,
        serviceRequestJson.toString(),
        serviceRegistrationError405ResponseJson.toString(),
        METHOD_NOT_ALLOWED
      )
      await(Connector.registrationDisplay(serviceRequestJson.as[RegistrationDisplayRequestHolder])) must be(
        Left(ServiceUnavailableResponse)
      )
    }
  }
}
