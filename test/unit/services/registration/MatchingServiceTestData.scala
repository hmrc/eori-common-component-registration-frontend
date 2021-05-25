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

package unit.services.registration

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.matching.MatchingResponse
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.messaging.{Address, Individual, RequestCommon}
import util.builders.matching.NinoFormBuilder

trait MatchingServiceTestData {
  val ninoId: String                        = NinoFormBuilder.Nino
  val establishmentDate: LocalDate          = LocalDate.parse("1961-04-12")
  val CorporateBody                         = "Corporate Body"
  val Partnership                           = "Partnership"
  val UnincorporatedBody                    = "Unincorporated Body"
  val individualFirstName                   = "John"
  val individualMiddleName                  = "Middle"
  val individualLastName                    = "Doe"
  val individualDateOfBirth                 = "1999-12-20"
  val individualLocalDateOfBirth: LocalDate = LocalDate.parse(individualDateOfBirth)
  val individual: Individual                = Individual.noMiddle(individualFirstName, individualLastName, individualDateOfBirth)

  val individualWithMiddleName =
    Individual(individualFirstName, Some(individualMiddleName), individualLastName, individualDateOfBirth)

  val utrId                 = "2108834503"
  val utr                   = Utr(utrId)
  val eoriId                = "eor-123"
  val eori                  = Eori(eoriId)
  val nino                  = Nino(ninoId)
  val someEstablishmentDate = Some(establishmentDate)

  val ExpectedRequestCommon =
    RequestCommon("CDS", new DateTime("2016-07-08T08:35:13Z"), "4482baa8-1c84-4d23-a8db-3fc180325e7a")

  val matchedAddress = Address("Line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("SE28 1AA"), "GB")

  def utrOnlyRequestJson(utrIdToMatch: String, isAnAgent: Boolean): JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "UTR",
         |      "IDNumber": "$utrIdToMatch",
         |      "requiresNameMatch": false,
         |      "isAnAgent": $isAnAgent
         |    }
         |  }
         |}
    """.stripMargin)

  val utrAndNameRequestJson: JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "UTR",
         |      "IDNumber": "$utrId",
         |      "requiresNameMatch": true,
         |      "isAnAgent": false,
         |      "organisation": {
         |        "organisationName": "someOrg",
         |        "organisationType": "0001"
         |      }
         |    }
         |  }
         |}
        """.stripMargin)

  val eoriAndNameRequestJson: JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "EORI",
         |      "IDNumber": "$eoriId",
         |      "requiresNameMatch": true,
         |      "isAnAgent": false,
         |      "organisation": {
         |        "organisationName": "someOrg",
         |        "organisationType": "0004"
         |      }
         |    }
         |  }
         |}
        """.stripMargin)

  val utrIndividualRequestJson: JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "UTR",
         |      "IDNumber": "$utrId",
         |      "requiresNameMatch": true,
         |      "isAnAgent": false,
         |      "individual": {
         |        "firstName": "$individualFirstName",
         |        "lastName": "$individualLastName",
         |        "dateOfBirth": "$individualDateOfBirth"
         |      }
         |    }
         |  }
         |}
        """.stripMargin)

  val eoriIndividualRequestJson: JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "EORI",
         |      "IDNumber": "$eoriId",
         |      "requiresNameMatch": true,
         |      "isAnAgent": false,
         |      "individual": {
         |        "firstName": "$individualFirstName",
         |        "middleName": "$individualMiddleName",
         |        "lastName": "$individualLastName",
         |        "dateOfBirth": "$individualDateOfBirth"
         |      }
         |    }
         |  }
         |}
        """.stripMargin)

  val ninoIndividualRequestJson: JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "NINO",
         |      "IDNumber": "$ninoId",
         |      "requiresNameMatch": true,
         |      "isAnAgent": false,
         |      "individual": {
         |        "firstName": "first",
         |        "lastName": "last",
         |        "dateOfBirth": "1980-03-31"
         |      }
         |    }
         |  }
         |}
        """.stripMargin)

  val ninoRequestJson: JsValue =
    Json.parse(s"""{
         |  "registerWithIDRequest": {
         |    "requestCommon": {
         |      "regime": "CDS",
         |      "receiptDate": "2016-07-08T08:35:13Z",
         |      "acknowledgementReference": "4482baa8-1c84-4d23-a8db-3fc180325e7a"
         |    },
         |    "requestDetail": {
         |      "IDType": "NINO",
         |      "IDNumber": "$ninoId",
         |      "requiresNameMatch": false,
         |      "isAnAgent": false
         |    }
         |  }
         |}
        """.stripMargin)

  val successResponse: JsValue =
    Json.parse("""
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
        |      "isAnIndividual": false,
        |      "organisation":       {
        |        "organisationName": "orgName",
        |        "isAGroup": false,
        |        "organisationType": "Partnership",
        |        "code": "0001"
        |      },
        |      "address":       {
        |        "addressLine1": "Line 1",
        |        "addressLine2": "line 2",
        |        "addressLine3": "line 3",
        |        "addressLine4": "line 4",
        |        "postalCode": "SE28 1AA",
        |        "countryCode": "GB"
        |      },
        |      "contactDetails":       {
        |        "phoneNumber": "01632961234",
        |        "emailAddress": "john.doe@example.com"
        |      }
        |    }
        |  }
        |}
      """.stripMargin)

  val matchSuccessResponse: MatchingResponse = successResponse.as[MatchingResponse]

  val successResponseMandatoryFields: JsValue =
    Json.parse("""
        |{
        |  "registerWithIDResponse": {
        |    "responseCommon": {
        |      "status": "OK",
        |      "processingDate": "2016-07-08T08:35:13Z"
        |    },
        |    "responseDetail": {
        |      "SAFEID": "XE0000123456789",
        |      "isEditable": true,
        |      "isAnAgent": false,
        |      "isAnIndividual": false,
        |      "organisation": {
        |        "organisationName": "orgName",
        |        "isAGroup": false
        |      },
        |      "address": {
        |        "addressLine1": "Line 1",
        |        "countryCode": "ZZ"
        |      },
        |      "contactDetails": {
        |      }
        |    }
        |  }
        |}
      """.stripMargin)

  val matchSuccessResponseMandatoryFields: MatchingResponse = successResponseMandatoryFields.as[MatchingResponse]

  val successResponseWithoutOrganisationType: JsValue =
    Json.parse("""
        |{
        |  "registerWithIDResponse": {
        |    "responseCommon": {
        |      "status": "OK",
        |      "processingDate": "2016-07-08T08:35:13Z",
        |      "returnParameters": [{
        |        "paramName": "SAP_NUMBER",
        |        "paramValue": "0123456789"
        |      }]
        |    },
        |    "responseDetail": {
        |      "SAFEID": "XE0000123456789",
        |      "isEditable": true,
        |      "isAnAgent": false,
        |      "isAnIndividual": false,
        |      "organisation": {
        |        "organisationName": "orgName",
        |        "isAGroup": false
        |      },
        |      "address": {
        |        "addressLine1": "Line 1",
        |        "countryCode": "ZZ"
        |      },
        |      "contactDetails": {
        |      }
        |    }
        |  }
        |}
      """.stripMargin)

  val matchSuccessResponseWithoutOrganisationType: MatchingResponse =
    successResponseWithoutOrganisationType.as[MatchingResponse]

  val successResponseIndividual: JsValue =
    Json.parse(s"""
         |{
         |  "registerWithIDResponse": {
         |    "responseCommon":    {
         |      "status": "OK",
         |      "processingDate": "2016-07-08T08:35:13Z",
         |      "returnParameters": [{
         |        "paramName": "SAP_NUMBER",
         |        "paramValue": "0123456789"
         |      }]
         |    },
         |    "responseDetail":    {
         |      "SAFEID": "XE0000123456789",
         |      "ARN": "",
         |      "isEditable": true,
         |      "isAnAgent": false,
         |      "isAnIndividual": true,
         |      "individual":       {
         |        "firstName": "$individualFirstName",
         |        "lastName": "$individualLastName",
         |        "dateOfBirth": "$individualDateOfBirth"
         |      },
         |      "address":       {
         |        "addressLine1": "Line 1",
         |        "addressLine2": "line 2",
         |        "addressLine3": "line 3",
         |        "addressLine4": "line 4",
         |        "postalCode": "SE28 1AA",
         |        "countryCode": "GB"
         |      },
         |      "contactDetails": {
         |        "phoneNumber": "01632961234",
         |        "emailAddress": "john.doe@example.com"
         |      }
         |    }
         |  }
         |}
    """.stripMargin)

  val matchIndividualSuccessResponse: MatchingResponse = successResponseIndividual.as[MatchingResponse]

  val successResponseIndividualWithMiddleName: JsValue =
    Json.parse(s"""
         |{
         |  "registerWithIDResponse": {
         |    "responseCommon":    {
         |      "status": "OK",
         |      "processingDate": "2016-08-16T15:55:33Z",
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
         |      "isAnIndividual": true,
         |      "individual":       {
         |        "firstName": "$individualFirstName",
         |        "middleName": "$individualMiddleName",
         |        "lastName": "$individualLastName",
         |        "dateOfBirth": "$individualDateOfBirth"
         |      },
         |      "address":       {
         |        "addressLine1": "Line 1",
         |        "addressLine2": "line 2",
         |        "addressLine3": "line 3",
         |        "addressLine4": "line 4",
         |        "postalCode": "SE28 1AA",
         |        "countryCode": "GB"
         |      },
         |      "contactDetails": {
         |        "phoneNumber": "01632961234",
         |        "emailAddress": "john.doe@example.com"
         |      }
         |    }
         |  }
         |}
    """.stripMargin)

  val matchIndividualWithMiddleNameSuccessResponse: MatchingResponse =
    successResponseIndividualWithMiddleName.as[MatchingResponse]

}
