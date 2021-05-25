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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import common.support.testdata.TestData
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import play.mvc.Http.MimeTypes.JSON
import play.mvc.Http.Status.OK
import uk.gov.hmrc.eoricommoncomponent.frontend.models.Journey

object SubscriptionDisplayMessagingService {

  private def subscriptionPath(id: String, requestAcknowledgementReference: String, journey: Journey.Value): String =
    journey match {
      case Journey.Register =>
        s"/subscription-display?regime=CDS&taxPayerID=$id&acknowledgementReference=$requestAcknowledgementReference"
      case Journey.Subscribe =>
        s"/subscription-display?regime=CDS&EORI=$id&acknowledgementReference=$requestAcknowledgementReference"
    }

  def validResponse(typeOfLegalEntity: String, taxPayerID: String = TestData.TaxPayerID): String =
    s"""{
       |  "subscriptionDisplayResponse": {
       |    "responseCommon": {
       |      "status": "OK",
       |      "processingDate": "2016-08-17T19:33:47Z",
       |      "taxPayerID": "$taxPayerID",
       |      "returnParameters": [
       |        {
       |          "paramName": "ETMPFORMBUNDLENUMBER",
       |          "paramValue": "9876543210"
       |        },
       |        {
       |          "paramName": "POSITION",
       |          "paramValue": "LINK"
       |        }
       |      ]
       |    },
       |    "responseDetail": {
       |      "EORINo": "EN123456789012345",
       |      "SAFEID": "XY000$taxPayerID",
       |      "CDSFullName": "John Doe",
       |      "CDSEstablishmentAddress": {
       |        "streetAndNumber": "house no Line 1",
       |        "city": "city name",
       |        "postalCode": "SE28 1AA",
       |        "countryCode": "ZZ"
       |      },
       |      "establishmentInTheCustomsTerritoryOfTheUnion": "0",
       |      "typeOfLegalEntity": "$typeOfLegalEntity",
       |      "contactInformation": {
       |        "personOfContact": "John Doe",
       |        "streetAndNumber": "Line 1",
       |        "city": "city name",
       |        "postalCode": "SE28 1AA",
       |        "countryCode": "ZZ",
       |        "telephoneNumber": "01632961234",
       |        "faxNumber": "01632961235",
       |        "emailAddress": "john.doe@example.com"
       |      },
       |      "VATIDs": [
       |        {
       |          "countryCode": "GB",
       |          "VATID": "999999"
       |        },
       |        {
       |          "countryCode": "ES",
       |          "VATID": "888888"
       |        }
       |      ],
       |      "thirdCountryUniqueIdentificationNumber": [
       |        "321",
       |        "222"
       |      ],
       |      "consentToDisclosureOfPersonalData": "1",
       |      "shortName": "Doe",
       |      "typeOfPerson": "1",
       |      "principalEconomicActivity": "2000"
       |    }
       |  }
       |}
       | """.stripMargin

  def returnSubscriptionDisplayWhenReceiveRequest(
    id: String,
    requestAcknowledgementReference: String,
    journey: Journey.Value,
    returnedStatus: Int = OK
  ): Unit =
    stubFor(
      get(urlEqualTo(subscriptionPath(id, requestAcknowledgementReference, journey)))
        .willReturn(
          aResponse()
            .withStatus(returnedStatus)
            .withBody(validResponse(typeOfLegalEntity = "0001"))
            .withHeader(CONTENT_TYPE, JSON)
        )
    )

}
