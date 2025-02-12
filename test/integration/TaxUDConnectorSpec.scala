/*
 * Copyright 2025 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, postRequestedFor, urlEqualTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.eoricommoncomponent.frontend.config.{InternalAuthTokenInitialiser, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.eoricommoncomponent.frontend.connector.{
  ErrorResponse,
  ServiceUnavailableResponse,
  SuccessResponse,
  TaxUDConnector
}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.CdsOrganisationType.Embassy
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.Address
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.transformer.FormDataCreateEoriSubscriptionRequestTransformer
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.subscription.txe13.CreateEoriSubscriptionResponse.SubscriptionBody
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.registration.UserLocation.Uk
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.subscription.{FormData, SubscriptionDetails}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{RegistrationDetailsEmbassy, SafeId}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.ContactDetailsModel
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Service.gagmr
import uk.gov.hmrc.http.HeaderCarrier
import util.externalservices.ExternalServicesConfig._
import util.externalservices.{AuditService, EtmpTxe13StubService}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaxUDConnectorSpec extends IntegrationTestsSpec with ScalaFutures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "microservice.services.taxud.host"    -> Host,
        "microservice.services.taxud.port"    -> Port,
        "microservice.services.taxud.context" -> "taxud",
        "auditing.enabled"                    -> true,
        "auditing.consumer.baseUri.host"      -> Host,
        "auditing.consumer.baseUri.port"      -> Port
      )
    )
    .overrides(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
    .build()

  private lazy val taxUdConnector = app.injector.instanceOf[TaxUDConnector]
  val txe13Url: String            = "/txe13/eori/subscription/v1"

  private lazy val mockRequest =
    """
      |{
      |  "edgeCaseType": "01",
      |  "cdsFullName": "Masahiro Moro",
      |  "organisation": {
      |    "dateOfEstablishment": "",
      |    "organisationName": "Embassy Of Japan"
      |  },
      |  "cdsEstablishmentAddress": {
      |    "city": "London",
      |    "countryCode": "GB",
      |    "postcode": "SE28 1AA",
      |    "streetAndNumber": "101-104 Piccadilly, Greater London"
      |  },
      |  "legalStatus": "diplomatic mission",
      |  "separateCorrespondenceAddressIndicator": true,
      |  "consentToDisclosureOfPersonalData": true,
      |  "contactInformation": {
      |    "personOfContact": "Masahiro Moro",
      |    "streetAndNumber": "101-104 Piccadilly",
      |    "city": "Greater London",
      |    "countryCode": "GB",
      |    "isAgent": true,
      |    "isGroup": false,
      |    "email": "masahiro.moro@gmail.com",
      |    "postcode": "SE28 1AA",
      |    "telephoneNumber": "07806674501"
      |  },
      |  "serviceName": "HMRC-GVMS-ORG"
      |}
      |""".stripMargin

  val registrationDetails: RegistrationDetailsEmbassy = RegistrationDetailsEmbassy(
    embassyName = "Embassy Of Japan",
    embassyAddress =
      Address("101-104 Piccadilly", Some("Greater London"), Some("London"), None, Some("SE28 1AA"), "GB"),
    embassyCustomsId = None,
    embassySafeId = SafeId("")
  )

  val subscriptionDetails: SubscriptionDetails = {
    SubscriptionDetails(
      personalDataDisclosureConsent = Some(true),
      contactDetails = Some(
        ContactDetailsModel(
          "Masahiro Moro",
          "masahiro.moro@gmail.com",
          "07806674501",
          None,
          useAddressFromRegistrationDetails = false,
          Some("101-104 Piccadilly"),
          Some("Greater London"),
          Some("SE28 1AA"),
          Some("GB")
        )
      ),
      formData = FormData(organisationType = Some(Embassy)),
      embassyName = Some("Embassy Of Japan")
    )
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  before {
    resetMockServer()
    AuditService.stubAuditService()
  }

  override def beforeAll(): Unit =
    startMockServer()

  override def afterAll(): Unit =
    stopMockServer()

  "TaxUDConnector" should {

    "return SuccessResponse when a successful known response object comes back from ETMP" in {
      // Given
      val expectedRequest = new FormDataCreateEoriSubscriptionRequestTransformer().transform(
        registrationDetails,
        subscriptionDetails,
        Uk,
        gagmr
      )

      val expectedResponse = CreateEoriSubscriptionResponse(
        SubscriptionBody(
          "93000022142",
          "WORKLIST",
          LocalDateTime.parse("2023-11-28T10:15:10Z", DateTimeFormatter.ISO_DATE_TIME),
          "XR0000100051093"
        )
      )

      EtmpTxe13StubService.returnCreated(txe13Url, mockRequest)

      // When
      val eoriHttpResponse =
        await(taxUdConnector.createEoriSubscription(registrationDetails, subscriptionDetails, Uk, gagmr))

      // Then
      wiremockVerifyTxe13PostRequest()

      eoriHttpResponse mustBe SuccessResponse(
        "930000221423",
        SafeId("XR0000100051093"),
        LocalDateTime.parse("2023-11-28T10:15:10Z", DateTimeFormatter.ISO_DATE_TIME)
      )
    }

    "return error response" when {
      "Unprocessable entity comes back from ETMP" in {
        EtmpTxe13StubService.returnUnprocessableEntity(txe13Url, mockRequest)

        val eoriHttpResponse =
          await(taxUdConnector.createEoriSubscription(registrationDetails, subscriptionDetails, Uk, gagmr))

        wiremockVerifyTxe13PostRequest()

        eoriHttpResponse mustBe ErrorResponse
      }

      "Bad Request comes back from ETMP" in {
        EtmpTxe13StubService.returnBadRequestEntity(txe13Url, mockRequest)

        val eoriHttpResponse =
          await(taxUdConnector.createEoriSubscription(registrationDetails, subscriptionDetails, Uk, gagmr))

        wiremockVerifyTxe13PostRequest()

        eoriHttpResponse mustBe ErrorResponse
      }

      "return InvalidResponse when a 500 Internal Server Error comes back from ETMP" in {
        EtmpTxe13StubService.returnInternalServerError(txe13Url, mockRequest)

        val eoriHttpResponse =
          await(taxUdConnector.createEoriSubscription(registrationDetails, subscriptionDetails, Uk, gagmr))

        wiremockVerifyTxe13PostRequest()

        eoriHttpResponse mustBe ErrorResponse
      }
    }

    "return ServiceUnavailableResponse when" in {
      EtmpTxe13StubService.faultWithConnectionReset(txe13Url, mockRequest)

      val eoriHttpResponse =
        await(taxUdConnector.createEoriSubscription(registrationDetails, subscriptionDetails, Uk, gagmr))

      wiremockVerifyTxe13PostRequest()

      eoriHttpResponse mustBe ServiceUnavailableResponse
    }
  }

  private def wiremockVerifyTxe13PostRequest(): Unit = {
    WireMock.verify(
      postRequestedFor(urlEqualTo(txe13Url))
        .withRequestBody(equalToJson(mockRequest))
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
        .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
    )
  }

}
