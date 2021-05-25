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

package integration

import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor3
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.RegisterWithoutIdConnector
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging._
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.registration.ContactDetailsModel
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.externalservices.ExternalServicesConfig._
import util.externalservices.{AuditService, RegisterWithoutIdMessagingService}

class RegisterWithoutIdConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.eori-common-component-hods-proxy.host"                        -> Host,
        "microservice.services.eori-common-component-hods-proxy.port"                        -> Port,
        "microservice.services.eori-common-component-hods-proxy.register-without-id.context" -> "register-without-id",
        "auditing.enabled"                                                                   -> true,
        "auditing.consumer.baseUri.host"                                                     -> Host,
        "auditing.consumer.baseUri.port"                                                     -> Port
      )
    )
    .build()

  private lazy val registerWithoutIdConnector = app.injector.instanceOf[RegisterWithoutIdConnector]
  val expectedPostUrl: String                 = "/register-without-id"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll: Unit =
    startMockServer()

  override def afterAll: Unit =
    stopMockServer()

  private val requestDate = (new DateTime).withDate(2016, 3, 17).withTime(9, 30, 47, 114)

  private val contactDetails =
    ContactDetailsModel("John Doe", "john@example.com", "441234987654", None, true, None, None, None, None)

  val organisationReq: RegisterWithoutIDRequest = RegisterWithoutIDRequest(
    RequestCommon("CDS", requestDate, "abcdefg1234567890hijklmnop0987654"),
    RegisterWithoutIdReqDetails.organisation(
      OrganisationName("orgName"),
      Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("SE28 1AA"), "ZZ"),
      contactDetails
    )
  )

  private val organisationRequestJsonString =
    Json.parse("""
        |{
        |  "registerWithoutIDRequest": {
        |    "requestCommon": {
        |      "regime": "CDS",
        |      "receiptDate": "2016-03-17T09:30:47Z",
        |      "acknowledgementReference": "abcdefg1234567890hijklmnop0987654"
        |    },
        |    "requestDetail": {
        |      "contactDetails": {
        |        "phoneNumber": "441234987654",
        |        "emailAddress": "john@example.com"
        |      },
        |      "address": {
        |        "addressLine1": "Line 1",
        |        "addressLine2": "line 2",
        |        "addressLine3": "line 3",
        |        "addressLine4": "line 4",
        |        "postalCode": "SE28 1AA",
        |        "countryCode": "ZZ"
        |      },
        |      "organisation": {
        |        "organisationName": "orgName"
        |      }
        |    }
        |  }
        |}
      """.stripMargin).toString

  val individualReq: RegisterWithoutIDRequest = RegisterWithoutIDRequest(
    RequestCommon("CDS", requestDate, "abcdefg1234567890hijklmnop0987654"),
    RegisterWithoutIdReqDetails.individual(
      address = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("SE28 1AA"), "ZZ"),
      individual = Individual("John", Some("Middle"), "Doe", "1999-12-20"),
      contactDetail = contactDetails
    )
  )

  private val individualRequestJsonString =
    Json.parse("""
        |{
        | "registerWithoutIDRequest":{
        |   "requestCommon":{
        |      "regime":"CDS",
        |      "receiptDate":"2016-03-17T09:30:47Z",
        |      "acknowledgementReference":"abcdefg1234567890hijklmnop0987654"
        |   },
        |   "requestDetail":{
        |      "contactDetails": {
        |        "phoneNumber": "441234987654",
        |        "emailAddress": "john@example.com"
        |      },
        |      "address":{
        |         "addressLine1":"Line 1",
        |         "addressLine2":"line 2",
        |         "addressLine3":"line 3",
        |         "addressLine4":"line 4",
        |         "postalCode":"SE28 1AA",
        |         "countryCode":"ZZ"
        |      },
        |      "individual": {
        |        "firstName": "John",
        |        "middleName": "Middle",
        |        "lastName": "Doe",
        |        "dateOfBirth": "1999-12-20"
        |      }
        |   }
        | }
        |}
      """.stripMargin).toString

  private val processingDate = (new DateTime).withDate(2016, 3, 17).withTime(9, 31, 5, 0)

  val registrationResponse: RegisterWithoutIdResponseHolder = RegisterWithoutIdResponseHolder(
    RegisterWithoutIDResponse(
      ResponseCommon(
        "OK",
        Some("Status text"),
        processingDate,
        Some(List(MessagingServiceParam("SAP_NUMBER", "sapNumber-123")))
      ),
      Some(RegisterWithoutIdResponseDetail("XE0000123456789", ARN = Some("ZARN1234567")))
    )
  )

  private val serviceResponseJsonString =
    """
      |{
      |   "registerWithoutIDResponse":{
      |      "responseCommon":{
      |         "status":"OK",
      |         "statusText": "Status text",
      |         "processingDate":"2016-03-17T09:31:05Z",
      |         "returnParameters":[
      |            {
      |               "paramName":"SAP_NUMBER",
      |               "paramValue":"sapNumber-123"
      |            }
      |         ]
      |      },
      |      "responseDetail":{
      |         "SAFEID":"XE0000123456789",
      |         "ARN":"ZARN1234567"
      |      }
      |   }
      |}
    """.stripMargin

  val testData: TableFor3[String, String, RegisterWithoutIDRequest] =
    Table(
      ("Registering entity type", "Request json string", "Registration function"),
      ("organisation", organisationRequestJsonString, organisationReq),
      ("individual", individualRequestJsonString, individualReq)
    )

  "registerWithoutIdConnector" should {

    forAll(testData) { (registeringEntityType, requestJsonString, registerWithoutIdRequest) =>
      s"return successful response when matching service returns 200 for $registeringEntityType" in {

        RegisterWithoutIdMessagingService.returnTheResponseWhenReceiveRequest(
          expectedPostUrl,
          requestJsonString,
          serviceResponseJsonString
        )

        await(registerWithoutIdConnector.register(registerWithoutIdRequest)) must be(
          registrationResponse.registerWithoutIDResponse
        )
      }

      s"fail when Bad Request for $registeringEntityType" in {
        RegisterWithoutIdMessagingService.returnTheResponseWhenReceiveRequest(
          expectedPostUrl,
          requestJsonString,
          serviceResponseJsonString,
          BAD_REQUEST
        )

        val caught: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(registerWithoutIdConnector.register(registerWithoutIdRequest))
        }

        caught.statusCode mustBe 400
        caught.getMessage must startWith("POST of ")
      }

      s"fail when Internal Server Error for $registeringEntityType" in {
        RegisterWithoutIdMessagingService.returnTheResponseWhenReceiveRequest(
          expectedPostUrl,
          requestJsonString,
          serviceResponseJsonString,
          INTERNAL_SERVER_ERROR
        )

        val caught: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(registerWithoutIdConnector.register(registerWithoutIdRequest))
        }

        caught.statusCode mustBe 500
        caught.getMessage must startWith("POST of ")
      }

      s"fail when 4xx status code is received for $registeringEntityType" in {
        RegisterWithoutIdMessagingService.returnTheResponseWhenReceiveRequest(
          expectedPostUrl,
          requestJsonString,
          serviceResponseJsonString,
          FORBIDDEN
        )

        val caught: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(registerWithoutIdConnector.register(registerWithoutIdRequest))
        }

        caught.statusCode mustBe 403
        caught.getMessage must startWith("POST of ")
      }

      s"audit a successful request for $registeringEntityType" in {
        RegisterWithoutIdMessagingService.returnTheResponseWhenReceiveRequest(
          expectedPostUrl,
          requestJsonString,
          serviceResponseJsonString
        )

        await(registerWithoutIdConnector.register(registerWithoutIdRequest))

        eventually(AuditService.verifyXAuditWrite(1))
      }

      s"not audit a failed request for $registeringEntityType" in {
        RegisterWithoutIdMessagingService.returnTheResponseWhenReceiveRequest(
          expectedPostUrl,
          requestJsonString,
          serviceResponseJsonString,
          BAD_REQUEST
        )

        val caught = intercept[UpstreamErrorResponse] {
          await(registerWithoutIdConnector.register(registerWithoutIdRequest))
        }

        caught.statusCode mustBe 400
        AuditService.verifyXAuditWrite(0)
      }
    }
  }
}
